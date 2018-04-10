(ns org.kowboy.bukkit.drill
  (:require [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.listener :as listener]
            [org.kowboy.bukkit.blocks :as blocks])
  (:import [org.bukkit.entity Player]
           [org.bukkit.block Block BlockFace]
           [org.bukkit.plugin Plugin]
           [org.bukkit.event.player PlayerInteractEvent]
           [org.bukkit.event.block Action]
           [org.bukkit.util BlockIterator]
           [org.bukkit.inventory ItemStack]
           [org.bukkit Material]))

(defn drill!
  "Swaps solid blocks with TNT along the player's line of sight, then lights the
  first block of TNT. Run!!!!"
  [^Plugin plugin
   ^PlayerInteractEvent event]
  (let [blocks
        (->> (BlockIterator. (.getPlayer event)
                             100)
             (iterator-seq)
             (drop-while blocks/air?)
             (take-while (complement blocks/bedrock?)))]
    (doseq
      [block blocks]
      (.setType block Material/TNT false))
    (.setType
      (.getRelative (first blocks) BlockFace/UP)
      Material/FIRE
      false)))


;; These are the actions we are listening for...
(def left-click? #{Action/LEFT_CLICK_AIR Action/LEFT_CLICK_BLOCK})

(defn make-drill-listener
  [^Plugin plugin]
  (fn [^PlayerInteractEvent event]
    (let [item   (.getItem event)
          action (.getAction event)]
      (if (and
            (left-click? action)
            item
            (= Material/TNT (util/material item)))
        (drill! plugin event)))))

(defn register-listeners
  [plugin]
  (listener/register-listener plugin PlayerInteractEvent
                              (make-drill-listener plugin)))



































(comment
  ;; Pre-baked drill! impl.
  (let [blocks (->> (BlockIterator. (.getPlayer event) 50)
                    iterator-seq
                    (drop-while blocks/air?)
                    (take-while (complement blocks/bedrock?)))]
    (doseq [^Block block blocks]
      (.setType block Material/TNT false))
    (.createExplosion (.getWorld (.getPlayer event))
                      (util/location (first blocks))
                      0.1))

  (let [blocks (->> (BlockIterator. (.getPlayer event) 50)
                    iterator-seq
                    (drop-while blocks/air?))]
    (doseq [block blocks]
      (.setType block Material/TNT false)))

  )