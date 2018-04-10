(ns org.kowboy.bukkit.xray
  (:require [org.kowboy.bukkit.blocks :as blocks]
            [org.kowboy.bukkit.util :as util])
  (:import [org.bukkit.util BlockIterator]
           [org.bukkit.entity Player LivingEntity]
           [org.bukkit.block Block]
           [org.bukkit.command CommandExecutor]))

(def max-distance 100)

(defn block-seq
  "Creates a BlockIterator and wraps it in a Clojure seq.
  The distance is capped at 100."
  ([^LivingEntity entity distance]
    (iterator-seq 
      (BlockIterator. entity 
                      (min max-distance (or distance max-distance)))))
  ([^LivingEntity entity]
   (block-seq entity nil)))

;; map partitions into [TYPE COUNT FIRST_LOC] line data
(defn compress-block-data
  "Takes a sequence of consecutive blocks of the same type and compresses
  them down into a vector of [TYPE COUNT FIRST_LOC], where FIRST_LOC is the 
  location of the first block in the sequence."
  [blocks]
  (let [^Block first-block (first blocks)]
     [(blocks/color-str first-block) 
      (str (count blocks))
      (util/location-str first-block)]))

;; map line data to lines
(defn block-datum->string
  [block-datum]
  (apply format "%s  %s  %s" block-datum))

(defn xray 
  "Generates strings of block info using a BlockIterator.
  This info contains the block type, count of consecutive blocks of that type,
  and the coordinates of the first block in that sequence."
  [^Player player distance]
  (eduction (drop-while blocks/air?) ;; the first couple blocks are air - ignore.
            (take-while (complement blocks/bedrock?))
            (partition-by util/material) 
            (map compress-block-data) 
            (map block-datum->string)
            (block-seq player distance)))

(defn xray!
  "Generates block-info and sends the results to the player console."
  [^Player player distance]
  (util/send-message player (format "Blocks along line of sight (range %d):" distance))
  (util/send-message player (xray player distance))

  ;; Return true for command executor
  true)

(defn xray-command [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      (let [vargs (vec args)]
        (cond 
          ;; Don't allow this command from the console
          (not (instance? Player sender)) 
          (do 
            (util/send-message sender "This is a player-only command.")
            true)
          
          ;; command only takes 1 optional arg: distance
          (zero? (count vargs)) (xray! sender max-distance)

          ;; 1 arg, must be a number between 1 and max-distance, inclusive.
          (= 1 (count vargs))
          (try 
            (let [distance (Integer/parseInt (first vargs))]
              (if (<= 1 distance max-distance)
                (xray! sender distance)
                (do (util/send-message
                      sender 
                      (format "distance must be between 1 and %d." max-distance))
                    false)))
            (catch NumberFormatException e
              (do (util/send-message sender "distance must be an integer.")
                  false)))

          :else
          false)))))
