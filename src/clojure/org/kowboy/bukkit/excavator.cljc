(ns org.kowboy.bukkit.excavator
  (:require [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.chunks :as chunks]
            [org.kowboy.bukkit.blocks :as blocks])
  (:import [org.bukkit.command CommandExecutor] 
           [org.bukkit.block Block]
           [org.bukkit.enchantments Enchantment]
           [org.bukkit.entity Player]
           [org.bukkit.inventory ItemStack]
           [org.bukkit Material Chunk World]))

(def excavation-filter 
  (complement
    (blocks/material-predicate Material/AIR 
                               Material/BEDROCK
                               Material/MOB_SPAWNER)))

(defn break-block
  "Break the block with the given pickaxe. If the pickaxe is nil, 
  it means we don't want drops, so we will simply replace it with
  air."
  [^Block block
   ^ItemStack pickaxe]
  (if pickaxe
    (.breakNaturally block pickaxe)
    ;; No tool to dig with. This means we don't want drops.
    (.setType block Material/AIR false)))

;; Matches "stone" and "stone(3)", for example:
(def material-dv-pattern #"([^(]+)(\((\d)\))?")

(defn make-block-pred
  "Takes a lowercase block name with an optional data value in parens
  and returns a predicate fn that takes a block object."
  [plugin block-dv]
  ;; If we can't match the material type, we should log a WARNING 
  ;; in the plugin log and return a predicate that always returns
  ;; false!
  (let [matcher (re-matcher material-dv-pattern block-dv)
        [_ material-str _ dv-str] (when (re-find matcher) (re-groups matcher))
        material (when material-str (util/material material-str))
        dv (when dv-str (Byte/parseByte dv-str))]
    (if material
      (fn [block] 
        (and (= material (util/material block))
             (or (nil? dv)
                 (= dv (util/dv block)))))
      
      ;; couldn't match up with a Material instance, always false predicate.
      (do (util/warn plugin "Bad excavator config - %s is not a valid material"
                     material-str)
          (constantly false)))))

(defn mining-strategy
  "Creates a function that takes a block and returns a pickaxe to
  be used when breaking the block. The pickaxe will be enchanted 
  with Silk Touch or Fortune III, depending on the excavate.fortune
  list in config.yaml. Some blocks we do not want to drop (like stone
  and dirt), so those will return nil. These block types are specified
  in the excavate.nodrop list in config.yaml."
  [plugin]
  (let [nodrop (.. plugin getConfig (getStringList "excavate.nodrop"))
        fortune (.. plugin getConfig (getStringList "excavate.fortune"))
        
        ;; predicate fns
        block-pred (partial make-block-pred plugin)
        nodrop-pred (apply some-fn (map block-pred nodrop))
        fortune-pred (apply some-fn (map block-pred fortune))
        
        ;; Tools for breaking blocks naturally.
        fortune-pickaxe (doto 
                          (ItemStack. Material/DIAMOND_PICKAXE 1)
                          (.addEnchantments {Enchantment/DURABILITY (int 3)
                                             Enchantment/DIG_SPEED (int 5)
                                             Enchantment/LOOT_BONUS_BLOCKS (int 3)}))
        silk-pickaxe    (doto 
                          (ItemStack. Material/DIAMOND_PICKAXE 1)
                          (.addEnchantments {Enchantment/DURABILITY (int 3)
                                             Enchantment/DIG_SPEED (int 5)
                                             Enchantment/SILK_TOUCH (int 1)}))]
    (fn [^Block block]
      (cond
        (nodrop-pred block)  nil ;; indicates we don't want drops
        (fortune-pred block) fortune-pickaxe
        :else                silk-pickaxe))))

(def filler-material
  {"NETHER" Material/NETHER_BRICK
   "NORMAL" Material/DIRT
   "THE_END" Material/PURPUR_BLOCK})

(def floor-material
  {"NETHER" Material/NETHER_BRICK
   "NORMAL" Material/GRASS
   "THE_END" Material/PURPUR_BLOCK})

(defn fill-floor!
  "Bedrock is very uneven at the bottom. Let's smooth out the floor by filling
  empty space with dirt."
  [{:keys [player ^Chunk player-chunk] :as ctx}]

  (let [env (util/environment player-chunk)
        filler (filler-material env)
        floor (floor-material env)] 
    (doall
      (for [y (range 0 6)
            x (range 16)
            z (range 16)
            :let [block (.getBlock player-chunk x y z)
                  block-type (if (= 5 y) floor filler)]
            :when (blocks/air? block)]
        (doto block (.setType block-type false)))))
  ctx)

(defn light-floor!
  "Place glowstone on the floor in locations that will prevent mob 
  spawning."
  [{:keys [player ^Chunk player-chunk] :as ctx}]
  (doseq [[x z] [[3 3]              [12 3] 
                        [7 7] [8 7]
                        [7 8] 
                 [3 12]             [12 12]]]
    (.. player-chunk (getBlock x 5 z) (setType Material/GLOWSTONE false)))
  ctx)

(defn light-spawners!
  "Place glowstone around monster spawners to keep them from spawning
  until we can build a grinder. The blocks will be placed on the inner
  corners of the spawning area, as well as above and below the spawner."
  [{:keys [spawners] :as ctx}]
  ;; Create x, y, z offsets where we will place the glowstone. One in each
  ;; of the 8 corners, as well as one above and one below the spawner.
  (let [offsets (concat 
                  (for [x [-4 4] z [-4 4] y [-2 2]]
                    [x y z]) 
                  [[0 -2 0] [0 2 0]])]
    (doall
      (for [^Block spawner spawners
            [offset-x offset-y offset-z] offsets]
        (.. spawner 
            (getRelative offset-x offset-y offset-z)
            (setType Material/GLOWSTONE false)))))
  ctx)

(defn- perimeter-filter-fn
  [^Chunk ch]
  (if (= "NORMAL" (util/environment ch))
    (some-fn blocks/air? blocks/liquid?)  
    blocks/liquid?))

(defn excavate!
  "Clears an entire chunk by mining all desirable items. Which blocks 
  yield drops is controlled by the strategy (see mining-strategy above).
  The chunk will first be protected from lava and water blocks around the
  perimeter by filling in those blocks with dirt."
  [{:keys [player plugin strategy] :as ctx}]
  (let [^Chunk ch (.getChunk (util/location player))
        ^World world (util/world player)
        perimeter-filter (perimeter-filter-fn ch)
        filler (filler-material (util/environment ch))
        northwest-perimiter-x (dec (* 16 (.getX ch)))
        northwest-perimiter-z (dec (* 16 (.getZ ch)))
        
        ;; Find spawners before we start breaking things.
        ;; We will need this later so we can put lights around
        ;; them to prevent spawning.
        spawners (chunks/chunk-blocks 
                   ch (blocks/material-predicate Material/MOB_SPAWNER))]

    ;; Liquid blocks around the perimiter need to be sealed off!
    ;; We don't want liquid hot magma pouring into our hole and destroying drops.
    (doseq [^Block block (flatten (chunks/chunk-perimeter-blocks 
                                    ch 
                                    perimeter-filter))]
      (.setType block filler false)) 

    ;; break all the blocks in the chunk (besides air and bedrock).
    (doseq [block (chunks/chunk-blocks ch excavation-filter)]
      (break-block block (strategy block)))
    
    (-> ctx
        (assoc :player-chunk ch :spawners spawners)
        ;; make a smooth floor. 
        (fill-floor!)
        ;; prevent spawns.
        (light-floor!)
        (light-spawners!)
        )
    
    (.teleport player (doto (util/location player) (.setY 6)))
    
    ))


(defn excavate-command
  [plugin]
  (let [strategy (mining-strategy plugin)]
    (reify CommandExecutor
      (onCommand [this
                  sender
                  cmd
                  label
                  args]
        (if (not (instance? Player sender))
          (do 
            (util/send-message sender "This is a player-only command.")
            true)

          (do
            (excavate! {:player sender :args args :plugin plugin
                        :strategy strategy})
            true))))))

