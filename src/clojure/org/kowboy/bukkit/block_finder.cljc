(ns org.kowboy.bukkit.block-finder
  (:require [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.blocks :as blocks]
            [org.kowboy.bukkit.chunks :as chunks]
            [clojure.core.match :refer [match]]
            [clojure.string :as str])
  (:import [org.bukkit World Location Chunk ChunkSnapshot Material]
           [org.bukkit.block Block]
           [org.bukkit.entity Entity EntityType Player]
           [org.bukkit.command CommandExecutor]))

(def block-materials 
  (filter #(.isBlock %) (Material/values)))

(def living-entities
  (filter #(.isAlive %) (EntityType/values)))

(def token-tree 
  {"entities" nil
   "blocks" nil
   "entity" 
   (into {} (for [le living-entities] [(.toLowerCase (str le)) nil]))
   
   "block"
   (into {} (for [bm block-materials] [(.toLowerCase (str bm)) nil]))})


(defn get-cmd [{:keys [args]}]
  (keyword (first args)))


;; Mulitmethods for the various finder subcommands
;; These have been split into separate phases: argument parsing and
;; then the actual finder implementation

(defmulti finder 
  "Executes the appropriate finder implementation based on the 
  first argument." 
  get-cmd)

(defmulti parse-args
  "Parses the arguments based on the first arg, which is the finder
  subcommand. May add parsed data to the context map."
  get-cmd)

(defn entity-group-summary
  "Takes a group of like entities and returns summary data:
  [EntityType Count Nearest]. The input to this
  funtions is an origin location for distance caluclation and
  a map-entry from a group-by."
  [^Location loc [etype entities]]
  (let [nearest (first (sort-by (partial util/distance loc) entities))]
    [etype ;; maybe colorize? 
     (count entities) 
     nearest]))

(defn nearest-distance
  "Sort function that takes an player location and an entity group summary 
  [EntityType Count Nearest] and returns the distance from the player
  to the nearest entity of that type."
  [player-loc [_ _ loc]]
  (util/distance player-loc loc))

(defn entity-group-string
  "Converts an entity group summary to a string."
  [[entity-type entity-count nearest-loc]]
  (str (.toLowerCase (str entity-type))
       " - "
       entity-count
       " - "
       (util/location-str nearest-loc)))

(defn exclude-player
  "Filter function for omitting the player."
  [^Player p ^Entity e]
  (not= p e))

;; ENTITIES

(defmethod finder :entities 
  [{:keys [^Player player]}]
  (let [^Location loc (util/location player)
        ^World world (util/world loc)]
    (util/send-message
      player
      (->> (seq (.getLivingEntities world))
           (filter (partial exclude-player player))
           (group-by util/entity-type)
           (map (partial entity-group-summary loc)) 
           (sort-by (partial nearest-distance loc))
           (map entity-group-string)
           (cons "---- ENTITIES FOUND ----")))
    true))

(defmethod parse-args :entity
  [{:keys [args] :as ctx}]

  (letfn [(parse-entity-type [my-ctx]
            (if-let [etype (util/entity-type (second args))]
              (assoc my-ctx :entity-type etype) 
              ;; entity type didn't parse
              (assoc my-ctx :bad-args true :errors ["not a valid entity type"])))]
   (if (= 2 (count args))
    ;; Handle entity type
    (parse-entity-type ctx)
    ;; Wrong # of args
    (assoc ctx :bad-args true))))

;; only show the top n closest entities
(def entity-top-n 8)

(defmethod finder :entity
  [{:keys [^Player player
           bad-args
           errors
           entity-type] :as ctx}]
  (when errors (util/send-message player errors))
  (if bad-args
    false

    (let [^Location loc (util/location player)
          ^World world (util/world loc)]
      (util/send-message 
        player
        (->> (seq (.getLivingEntities world))
             (filter #(= entity-type (util/entity-type %)))
             (map #(vector % (util/distance loc %)))
             (sort-by second)
             (take entity-top-n)
             (map (comp #(str (.toLowerCase (str %)) ;; maybe colorize?
                              " - "
                              (util/location-str %))     
                        first))
             (cons "---- ENTITIES FOUND ----")
             ))
      true)))

;; BLOCKS

(defn make-block-filter
  [types]
  (let [type-filter (if (sequential? types) (set types) #{types})]
    (fn [^Block b] (type-filter (util/material b)))))

(defn get-block-filter
  [plugin]
  (make-block-filter (->> (.. plugin 
                              (getConfig)
                              (getStringList "finder.block_filter"))
                          (map #(Material/valueOf (.toUpperCase %))))))

(defn block-group-summary
  [^Location loc [material-type blocks]]
  (let [^Block nearest (first (sort-by (partial util/distance loc) blocks))]
    [(blocks/color-str nearest)
     (count blocks)
     (util/location-str nearest)]))

(defn parse-inradius 
  [{:keys [args] :as ctx}]
  (condp = (count args)
    ;; no inradius arg, just use default
    1 (assoc ctx :inradius chunks/default-inradius)
    ;; need to convert and check the range of the inradius arg
    2 (try 
        (let [int-arg (Integer/parseInt (second args))]
          (if (<= 0 int-arg 10)
            (assoc ctx :inradius int-arg)
            (assoc ctx 
                   :errors [(format "inradius must be between 0 and %s" chunks/max-inradius)]
                   :bad-args true))) 
           (catch NumberFormatException e
             (assoc ctx
                    :errors ["inradius must be an integer"]
                    :bad-args true))) 
    (assoc ctx :bad-args true)))

(defmethod parse-args :blocks
  [ctx]
  (parse-inradius ctx))

(defmethod finder :blocks
  [{:keys [player
           plugin
           inradius
           bad-args
           errors] :as ctx}]
  (when errors (util/send-message player errors))
  (if (not bad-args) 
    (let [block-filter (get-block-filter plugin)
          ^Location player-loc (util/location player)
          ^Location player-block-loc (util/location (util/block player-loc))]
      (util/send-message
        player
        (->> (for [ch (chunks/chunk-seq player-block-loc inradius)]
               (chunks/chunk-blocks ch block-filter))
             (apply concat)
             (group-by util/material)
             (map (partial block-group-summary player-block-loc))
             (map #(str/join " - " %))
             (cons "---- BLOCKS FOUND ----")))
      true)
    false))

(defmethod parse-args :block
  [{:keys [args] :as ctx}]
  (letfn [(parse-block-type [my-ctx]
            (let [material (util/material (second args))]
              (if (some-> material (.isBlock))
                (assoc my-ctx :block-type material)
                ;; not a block material!
                (assoc my-ctx :bad-args true :errors ["not a valid block type"] ))))
          (parse-radius [my-ctx]
            (try
              (let [r (Integer/parseInt (nth args 2))]
                (if (<= 0 r chunks/max-inradius)
                  (assoc my-ctx :inradius r)
                  (-> my-ctx
                      (assoc :bad-args true)
                      (update :errors concat [(format "inradius must be between 0 and %s"
                                                      chunks/max-inradius)]))))))]
    (if (<= 2 (count args) 3)
      ;; handle block type first
      (cond-> (parse-block-type ctx)
        ;; handle inradius if it exists
        (= 3 (count args)) (parse-radius))
      (assoc ctx :bad-args true))))

(defprotocol IVein
  (add-block [vein block]))

(defrecord Vein
  [blocks        ;; Should be a set of blocks.
   ^int xmin     ;; These min and max coordinates define the bounding box
   ^int xmax     ;; for the vein and should be kept internally consistent.
   ^int ymin     ;; Keep these at +1 in each direction because we will be 
   ^int ymax     ;; doing lots of adjacency tests to add blocks to the vein,
   ^int zmin     ;; and this avoids lots of unnecessary inc/dec'ing. 
   ^int zmax
   ^double distance ;; distance from the player to the vein
                    ;; only set once for performance reasons 
   ]

  IVein
  (add-block [vein block]
    ;; This fn only adds the block to the vein if it is adjacent.
    ;; We test to see if this block is within the bounding box for
    ;; the blocks that are already in the vein.
    (let [^Block b block
          x (.getX b) y (.getY b) z (.getZ b)]
      (if (and (<= xmin x xmax)
               (<= ymin y ymax)
               (<= zmin z zmax))
        (-> vein
            (assoc :xmin (min xmin (dec x)) ;; using dec here to keep the 1-block 'aura'.
                   :xmax (max xmax (inc x)) ;; likewise
                   :ymin (min ymin (dec y))
                   :ymax (max ymax (inc y))
                   :zmin (min zmin (dec z))
                   :zmax (max zmax (inc z)))
            (update :blocks conj block))

        ;; not within the box, return unchanged.
        vein)))
  

  util/HasLocation
  (location [vein] (util/location (first blocks)))
  (location-str [vein] (util/location-str (util/location vein)))
  (distance [vein thingy2] (util/distance (util/location vein) thingy2))
  (world [vein] (util/world (util/block vein)))
  (block [vein] (util/block (first blocks)))

  util/HasMaterial
  (material [vein] (util/material (first blocks)))
  
  Comparable
  (compareTo [this that]
    (let [c (compare distance (:distance that))]
      (if (zero? c) 
        ;; distance is same, compare everything else
        (compare (count (:blocks that))
                 (count (:blocks this)))
        c))))

(defn new-vein 
  [^Location player-loc 
   ^Block b]
  (let [x (.getX b) y (.getY b) z (.getZ b)]
    (Vein. #{b} (dec x) (inc x) (dec y) (inc y) (dec z) (inc z)
           (util/distance player-loc b))))

(defn as-veins
  [^Location player-loc
   block-seq]
  ;; So...
  ;; Two recursive fns
  ;; fn1: takes the next item from ungrouped - that becomes the first block in the vein.
  ;;      does this recursively until ungrouped is empty.
  ;; fn2: recursively adds adjacent blocks to the vein, removing them from ungrouped.
  ;;
  (loop [veins (sorted-set)
         ungrouped (set block-seq)]
    (if (seq ungrouped)
      (let [vein (reduce
                   ;; find all adjacent blocks and add them to the vein
                   add-block 
                   ;; start with the first block
                   (new-vein player-loc (first ungrouped))
                   ;; reduce over all ungrouped blocks
                   (rest ungrouped))]
        (recur (conj veins vein)
               ;; remove the new vein's blocks from ungrouped.
               (reduce disj ungrouped (:blocks vein)))) 
      ;; done - return the veins 
      veins)))

(defn vein-summary
  "Converts a Vein record into a string for display."
  [vein]
  (format "%d %s at %s"
            (count (:blocks vein))
            (blocks/color-str (util/block vein))
            (util/location-str vein)))

(defmethod finder :block
  [{:keys [^Player player
           bad-args
           errors
           block-type
           inradius]
    :or {inradius chunks/default-inradius}}]
   (when errors (util/send-message player errors))
   (if bad-args
     false

     (let [^World world (util/world player)
           block-filter (make-block-filter block-type)
           ^Location player-loc (util/location player)
           ^Location player-block-loc (util/location (util/block player-loc))]
       (util/send-message
         player
         (->> (for [ch (chunks/chunk-seq player-block-loc inradius)]
                (chunks/chunk-blocks ch block-filter))
              (apply concat)
              ;; We are only going to show at most the top 10 closest
              ;; veins. This will whittle things down so
              ;; we don't process more than we have to (for example if 
              ;; the user asked for coal with an inradius of 10!).
              ;; Note, for large inradius, this might not include the
              ;; closest veins!!! Use a smaller inradius to start.
              (take 5000)
              ;; Group into veins - this step is eager.
              (as-veins player-loc)
              ;; Veins are sorted by distance from player, so take top 5.
              ;; Note: distance is calculated from the first block added to
              ;; each vein. While not super accurate, it is performant and 
              ;; close enough for government work.
              (take 5)
              (map vein-summary)
              (cons "---- VEINS FOUND ----")))
       true)))

;; SLIME CHUNKS!

(defmethod parse-args :slime
  [ctx]
  (parse-inradius ctx))

(defmethod finder :slime
  [{:keys [player
           inradius
           bad-args
           errors]
    :or {inradius chunks/max-inradius}}]
  (when (seq errors) (util/send-message player errors))
  (if bad-args
    false

    (do
      (util/send-message 
        player
        (->> (chunks/chunk-seq player inradius)
             (filter #(.isSlimeChunk %))
             (map #(format "(%d, %d)"
                           (.getX %) (.getZ %)))
             (str/join ", ")
             (conj ["---- SLIME CHUNKS ----"])))
      true)))


(defmethod finder :default
  [_] false)

(defmethod parse-args :default
  [ctx] ctx)

(defn find-command
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      
      (if (not (instance? Player sender))
        (do (util/send-message sender "This is a player-only command.")
            true)

        (finder (parse-args {:player sender :args args :plugin plugin}))))))

