(ns org.kowboy.bukkit.scratch
  (:require [org.kowboy.bukkit.listener :refer [register-listener]]
            [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.block-finder :as bf]
            [org.kowboy.bukkit.chunks :as chunks]
            [org.kowboy.bukkit.blocks :as blocks]
            [quil.core :as q]
            [quil.middleware :as qm]
            [clojure.string :as str])
  (:import [org.bukkit.enchantments Enchantment]
           [org.bukkit.entity Player]
           [org.bukkit.util BlockIterator Vector]
           [org.bukkit Material ChatColor Location]
           [org.bukkit.inventory ItemStack]
           [org.bukkit.block BlockFace Biome]
           [org.bukkit Chunk World]
           [org.bukkit.event.server TabCompleteEvent]
           [org.bukkit.material SimpleAttachableMaterialData Torch]))

(def ^:private scratch-state (atom {}))

(defn inject-plugin [p]
  (reset! scratch-state
          (if p
            {:plugin p
             :server (.getServer p)}
            {})))

(defn get-plugin []
  (:plugin @scratch-state))

(defn set-player! [player-name]
  (let [player (.getPlayer (:server @scratch-state) player-name)]
    (swap! scratch-state assoc
           :player player)))

(defn filter-chunks [r pred]
  (for [x (range (- r) r)
        z (range (- r) r)
        :when (pred x z)]
    [x z]))

(defn get-generated-chunks
  [^World world r]
  (filter-chunks r
                 (fn [x z]
                   (.isChunkGenerated world x z))))

(def get-chunk-coords (juxt #(.getX ^Chunk %) #(.getZ ^Chunk %)))

(defn get-player-chunk-coords
  ([^Player player]
   (when-let [player-loc (and player (.isOnline player) (util/location player))]
     (get-chunk-coords (.getChunk player-loc))))
  ([] (get-chunk-coords (:player @scratch-state))))

(defn update-map [{:keys [^Player player
                          player-chunk
                          generated-chunks-ref] :as state}]
  ;; Behavior changes based on player's online status
  (if (and player (.isOnline player))
    (let [new-player-chunk (get-player-chunk-coords player)]
      (if (= new-player-chunk player-chunk)
        ;; no change if the player hasn't moved
        ;; however, the agent may have been updated in the background
        (assoc state :generated-chunks @generated-chunks-ref)
        (do
          ;; get the generated chunks asynchronously
          (send-off generated-chunks-ref (fn [_] (get-generated-chunks (.getWorld player) 255)))
          (assoc state :player-chunk new-player-chunk
                       :generated-chunks @generated-chunks-ref))))
    (dissoc state :player-chunk)))

(defn map-setup []
  (q/frame-rate 1)
  (update-map (assoc @scratch-state
                :generated-chunks-ref (agent []))))


(defn draw-generated-chunks [{:keys [generated-chunks] :as state}]
  (when generated-chunks
    (q/fill 0 77 38)                             ;; maybe base color on biome?
    (q/no-stroke)
    (doseq [[x z] generated-chunks]
      (q/rect (* x 4) (* z 4) 4 4)))
  state)

(defn draw-player [{:keys [player-chunk] :as state}]
  (when player-chunk
    (let [[x z] player-chunk]
      (q/fill 255 0 0)
      (q/no-stroke)
      (q/rect (* x 4) (* z 4) 4 4)))
  state)

(defn draw-grid [state]
  (q/stroke 224)
  (q/stroke-weight 1)
  (doseq [x (range -600 600 40)]
    (q/line [x -600] [x 600]))
  (doseq [z (range -600 600 40)]
    (q/line [-600 z] [600 z]))
  state)

(defn draw-axis [state]
  ;; Make axis darker and thicker
  (q/stroke 160)
  (q/stroke-weight 2)
  (q/line [0 -600] [0 600])
  (q/line [-600 0] [600 0])
  state)

(defn draw-map [state]
  ;; start with white background
  (q/background 255)

  ;; center the origin
  (q/with-translation
    [(/ (q/width) 2)
     (/ (q/height) 2)]
    (when state
      (-> state
          (draw-generated-chunks)
          (draw-grid)
          (draw-axis)
          (draw-player)))))

(defn do-generated-chunk-map! []
  (q/defsketch generated-chunk-map
               :title "Minecraft generated chunks map"
               :size [1200 1200]
               :setup map-setup
               :update update-map
               :draw draw-map
               :middleware [qm/fun-mode]))

#_(do-generated-chunk-map!)

(def ^:private biomes-data-ref (agent {:biomes #{}
                                       :biome-chunks []}))

(defn get-biome-chunks [^World world biome-set]
  (filter-chunks
    150
    (fn [x z]
      (contains? biome-set
                 (.getBiome world (* 16 x) (* 16 z))))))

(defn set-biomes! [world biome-names]
  (send-off biomes-data-ref
        (fn [{:keys [biomes] :as biome-data}]
          (util/info (:plugin @scratch-state)
                     "Selecting chunks with biomes: %s" (apply str (interpose ", " biome-names)))
          (let [biome-set (into #{} (map #(Biome/valueOf (.toUpperCase ^String %)) biome-names))]
            (if (not= biomes biome-set)
              {:biomes       biome-set
               :biome-chunks (let [biome-chunks (get-biome-chunks world biome-set)]
                                (util/info (:plugin @scratch-state) "Done getting biome chunks.")
                                biome-chunks)}
              biome-data)))))

(defn set-biomes!! [& biome-names]
  (let [^Player player (:player @scratch-state)]
    (when (and player (.isOnline player))
      (set-biomes! (.getWorld player) biome-names))))


(defn update-player-position [{:keys [^Player player player-chunk] :as state}]
  (if (and player (.isOnline player))
    (let [new-player-chunk (get-player-chunk-coords player)]
      (if (= new-player-chunk player-chunk)
        (do
          (util/info (:plugin @scratch-state) "Player position hasn't changed")
          state)
        (do
          (util/info (:plugin @scratch-state) "Player position has changed!")
          (assoc state :draw true
                       :player-chunk new-player-chunk))))
    (do
      (util/info (:plugin @scratch-state) "Player is offline")
      (dissoc state :player-chunk))))

(defn update-biome [{:keys [biomes-data-ref
                            biomes-data] :as state}]
  (if (not= @biomes-data-ref biomes-data)
    (do
      (util/info (:plugin @scratch-state) "Biome state changed!")
      (assoc state :biomes-data @biomes-data-ref
                   :draw true))
    (do
      (util/info (:plugin @scratch-state) "Biome state has not changed")
      state)))

(defn biome-setup [biome-names]
  (fn []
    (q/frame-rate 1)
    (apply set-biomes!! biome-names)
    (update-biome (assoc @scratch-state
                    :biomes-data-ref biomes-data-ref))))

(defn draw-biomes [{:keys [biomes-data draw] :as state}]
  (util/info (:plugin @scratch-state) "State: %s" (select-keys state [:draw :player-chunk]))
  (when draw
    (util/info (:plugin @scratch-state) "Drawing stuff...")
    (q/background 255)
    (q/with-translation
      [(/ (q/width) 2)
       (/ (q/height) 2)]
      (q/fill 255 255 0)
      (q/stroke 0)
      (q/stroke-weight 2)
      (doseq [[x z] (:biome-chunks biomes-data)]
        (q/rect (* x 4) (* z 4) 4 4))
      (-> state
          (draw-grid)
          (draw-axis)
          (draw-player)))))

(defn find-biomes! [& biome-names]
  (q/defsketch find-biome-map
               :title "Minecraft biome search"
               :size [1200 1200]
               :setup (biome-setup biome-names)
               :update (fn [state]
                         (-> (assoc state :draw false)
                             (update-player-position)
                             (update-biome)))
               :draw draw-biomes
               :middleware [qm/fun-mode]))

(defn unique-biomes
  []
  (let [player (.getPlayer (:server @scratch-state) "KowboyMac")
        world (.getWorld player)]
    (->> (filter-chunks 150 (constantly true))
         (map (fn [[x z]]
                (str (.getBiome world (+ 7 (* 16 x)) (+ 7 (* 16 z))))))
         (into #{})
         (sort))))

(defn test-sketch! []
  (q/defsketch test-sketch!
               :title "Minecraft test sketch"
               :size [1200 1200]
               :setup (fn [] {})
               :update identity
               :draw (fn [state]
                       (q/background 255)
                       (q/with-translation
                         [(/ (q/width) 2)
                          (/ (q/height) 2)]
                         (q/fill 255 255 0)
                         (q/stroke 0)
                         (q/stroke-weight 2)
                         (-> state
                             (draw-grid)
                             (draw-axis))))
               :middleware [qm/fun-mode]))

(comment
  (in-ns 'org.kowboy.bukkit.scratch)

  (set-player! "KowboyMac")

  (do-generated-chunk-map!)

  (find-biomes! "shattered_savanna_plateau" "savanna")

  (set-biomes!! "ocean" "deep_ocean" "warm_ocean" "lukewarm_ocean" "deep_lukewarm_ocean"
                "cold_ocean" "deep_cold_ocean" "frozen_ocean" "deep_frozen_ocean")

  (test-sketch!)


  (require '[org.kowboy.bukkit.light :as light])
  (set-player! "KowboyMac")

  (.. (:player @scratch-state)
      (getTargetBlock nil 5)
      (getData))

  )


