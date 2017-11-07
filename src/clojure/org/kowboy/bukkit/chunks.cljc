(ns org.kowboy.bukkit.chunks
  (:require [org.kowboy.bukkit.util :as util])
  (:import [org.bukkit Chunk ChunkSnapshot World]))


;; inradius is the distance from the center to the middle of a side.
(def max-inradius 10)
(def default-inradius 0)

(defn chunk-seq
  "Takes an object with a Location and an inradius, r. Returns a sequence of 
  chunks centered around the Location chunk. The order of chunks is not 
  guaranteed to be consistent. Performance and simplicity are the priorities."
  [has-loc r]
  (let [inradius (min max-inradius (or r default-inradius))
        initial-loc (.clone (util/location has-loc))
        ^Chunk initial-chunk (.getChunk initial-loc)
        northwest-x (- (.getX initial-chunk) inradius)
        northwest-z (- (.getZ initial-chunk) inradius)
        side-length (inc (* 2 inradius))]
    (for [x (take side-length (iterate inc northwest-x))
          z (take side-length (iterate inc northwest-z))]
      (.getChunkAt (util/world initial-loc) x z))))

(defn- highest-block-fn
  [^Chunk ch]
  (let [^ChunkSnapshot cs (.getChunkSnapshot ch)]
    (if (= "NORMAL" (util/environment ch))
      (fn [x z] (.getHighestBlockYAt cs x z))
      (constantly 127))))

(defn chunk-blocks
  "Gets all blocks that match the filter in the given chunk."
  [^Chunk ch
   block-filter]
  (let [^ChunkSnapshot cs (.getChunkSnapshot ch)
        highest-block (highest-block-fn ch)]
    (for [x (range 0 16) 
          z (range 0 16)
          y (range 1 (highest-block x z))
          :let [block (.getBlock ch x y z)]
          :when (block-filter block)]
      block)))

(defn chunk-perimeter-blocks
  "Gets all blocks around the perimeter of this chunk that match
  the given filter. Returns them as a seq of columns. Each column
  is a seq of blocks that share the same [x z] coordinates."
  [^Chunk ch
   block-filter]
  ;; Start by making a seq of xz pairs (columns) that we are going to 
  ;; then process along the y-axis. We will do this in 4 parts - one
  ;; for each edge.
  (let [^World world (.getWorld ch)
        highest-block (highest-block-fn ch)
        west-x  (dec (* 16 (.getX ch)))
        east-x  (+ 17 west-x)
        north-z (dec (* 16 (.getZ ch)))
        south-z (+ 17 north-z)
        xz-pairs (concat 
                   (for [x (take 17 (iterate inc west-x))] [x north-z])
                   (for [z (take 17 (iterate inc north-z))] [east-x z])
                   (for [x (take 17 (iterate dec east-x))] [x south-z])
                   (for [z (take 17 (iterate dec south-z))] [west-x z]))]
    (for [[x z] xz-pairs]
      (for [y (range 1 (highest-block x z))
            :let [^Block block (.getBlockAt world x y z)]
            :when (block-filter block)]
        block))))
