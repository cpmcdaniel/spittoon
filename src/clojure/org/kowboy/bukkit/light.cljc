(ns org.kowboy.bukkit.light
  "A plugin that shows light levels around the player when holding a
  torch in the main hand. Only works when sneaking so as to not cause lag."
  (:require [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.listener :as listener])
  (:import [org.bukkit.command CommandExecutor]
           [org.bukkit.plugin Plugin]
           [org.bukkit.event.player PlayerMoveEvent]
           [org.bukkit.entity Player]
           [org.bukkit Material Location Particle Particle$DustOptions Color]
           [org.bukkit.block BlockFace Block]
           [org.bukkit.scheduler BukkitRunnable]))

(def token-tree {"on" nil
                 "off" nil
                 "radius" nil})

;; State for this plugin is held in config.yml
;; This can be managed through the /light-level command.
(defn light-level-command
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      (condp = (vec args)
        ["on"] (do (.. plugin (getConfig) (set "light-level.on" true)) true)
        ["off"] (do (.. plugin (getConfig) (set "light-level.on" false)) true)
        (if (and (= "radius" (first args))
                 (util/is-numeric? (second args)))
          (do (.. plugin (getConfig) (set "light-level.radius" (Integer/parseInt (second args))))
              true)
          false)))))

(def default-radius 8)
(def default-on false)

(defn get-color [light-level]
  (condp <= light-level
    12 Color/LIME
    10 Color/GREEN
    8  Color/YELLOW
    Color/MAROON))

(defn new-task [^Plugin plugin
                ^Player player
                tasks]
  (proxy [BukkitRunnable] []
    (run []
      (cond
        (not (.isOnline player)) (.cancel this)
        (not (.isSneaking player)) (.cancel this)
        :else
        (let [radius (int (.. plugin (getConfig) (getInt "light-level.radius" default-radius)))]
          (doseq [x (range (- radius) radius)
                  z (range (- radius) radius)
                  ;; can't really see 3 blocks above player location
                  y (range (- radius) 3)]
            (let [^Location loc (.add (.getLocation player) x y z)
                  ^Block block (.getBlock loc)]
              (when
                (and (not= Material/AIR (.getType block))
                     (.. block (getType) (isSolid))
                     (#{Material/AIR Material/WATER}  (.. block (getRelative BlockFace/UP) (getType))))
                (let [^Location loc (.. block (getLocation) (add 0.5 1.0 0.5))
                      light-level (int (.getLightFromBlocks (.getBlock loc)))
                      ^Color color (get-color light-level)
                      data (Particle$DustOptions. color 1.0)]
                  (.spawnParticle player Particle/REDSTONE loc 3 data))))))))

    (cancel []
      (dosync (alter tasks disj (.getUniqueId player)))
      (proxy-super cancel))))

(defn make-light-listener
  [^Plugin plugin]
  (let [tasks (ref #{})]
    (fn on-player-move [^PlayerMoveEvent e]
      (when-let [^Player player (.getPlayer e)]
        (when (and
                (not (contains? @tasks (.getUniqueId player)))
                (.. plugin (getConfig) (getBoolean "light-level.on" default-on))
                (.isSneaking player)
                (or (= Material/TORCH (.. player (getInventory) (getItemInMainHand) (getType)))
                    (= Material/TORCH (.. player (getInventory) (getItemInOffHand) (getType)))))
          (.runTaskTimer ^BukkitRunnable (new-task plugin player tasks) plugin 10 10)
          (dosync
            (alter tasks conj (.getUniqueId player))))))))

(defn register-listeners
  [plugin]
  (listener/register-listener plugin PlayerMoveEvent
                              (make-light-listener plugin)))