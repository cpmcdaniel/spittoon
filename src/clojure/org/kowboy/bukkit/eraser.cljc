(ns org.kowboy.bukkit.eraser
  "A command and listener for destroying items when a player drops
  them from their inventory."
  (:require [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.listener :as listener])
  (:import [org.bukkit.command CommandExecutor]
           [org.bukkit.plugin Plugin]
           [org.bukkit.scheduler BukkitRunnable]
           [org.bukkit.metadata FixedMetadataValue]
           [org.bukkit.event.player PlayerDropItemEvent]))

(def token-tree {"on" nil
                 "off" nil})

(defn turn-on 
  [plugin player] 
  (.. player (setMetadata "eraser-on" (FixedMetadataValue. plugin true))) 
  (util/send-message player "Eraser turned on. Dropped items will be destroyed!")
  true)

(defn turn-off
  [plugin player]
  (.. player (removeMetadata "eraser-on" plugin))
  (util/send-message player "Eraser turned off.")
  false)

(defn eraser-command 
  "Toggles eraser on and off."
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      (if (= 1 (count args))
        (condp = (first args)
          "on" (turn-on plugin sender)
          "off" (turn-off plugin sender)
          false) 
        false))))

(defn on-drop!
  [^Plugin plugin
   ^PlayerDropItemEvent event]
  (let [player (.getPlayer event) 
        item (.getItemDrop event)
        stack (.getItemStack item)
        amount (.getAmount stack)
        material (util/material stack)]  
    ;; destroy it before they can pick it up
    (.setPickupDelay item 200)
    (.runTaskLater
      (proxy [BukkitRunnable] []
        (run [] 
          (.remove item)
          (util/send-message
            player
            (format "Destroyed %d %s" amount material))))
      plugin, 15)))

(defn make-eraser-listener
  "Listens for player drop item events and destroys the dropped item if the
  eraser has been turned on."
  [^Plugin plugin]
  (fn [^PlayerDropItemEvent event]
    (when (.hasMetadata (.getPlayer event) "eraser-on")
      (on-drop! plugin event))))

(defn register-listeners
  [plugin]
  (listener/register-listener plugin PlayerDropItemEvent
                              (make-eraser-listener plugin)))
