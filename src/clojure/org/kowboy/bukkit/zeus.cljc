(ns org.kowboy.bukkit.zeus
  (:require [org.kowboy.bukkit.util :refer [info]]
            [org.kowboy.bukkit.listener :as listener])
  (:import [org.bukkit.event.player PlayerInteractEvent]
           [org.bukkit.event.block Action]
           [org.bukkit.plugin Plugin]
           [org.bukkit.entity Player]
           [org.bukkit.inventory ItemStack]
           [org.bukkit Material]
           [org.bukkit.command CommandExecutor]))

(def token-tree {"on" nil "off" nil})

(defn zeus-command
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      (condp = (vec args)
        ["on"]  (do (.. plugin (getConfig) (set "zeus.on" true)) true)
        ["off"] (do (.. plugin (getConfig) (set "zeus.off" false)) true)
        false))))

(defn strike
  [^Player player
   ^Plugin plugin]
  (.. player
      (getWorld)
      (strikeLightning (.. player
                           (getTargetBlock nil 200)
                           (getLocation)))))

(defn make-lightning-rod-listener
  [^Plugin plugin]
  (fn [^PlayerInteractEvent event]
    (let [^ItemStack item (.getItem event)
          ^Action action (.getAction event)]
      (if (and
           (#{Action/LEFT_CLICK_AIR Action/LEFT_CLICK_BLOCK} action)
           (.. plugin getConfig (getBoolean "zeus.on"))
           item
           (= Material/STICK (.getType item)))
        ;; Creates a bolt of lightning at the target the player is looking at
        (strike (.getPlayer event) plugin)))))

(defn register-listeners
  [plugin]
  (listener/register-listener plugin PlayerInteractEvent
                              (make-lightning-rod-listener plugin)))
