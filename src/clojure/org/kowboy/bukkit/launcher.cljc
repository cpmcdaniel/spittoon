(ns org.kowboy.bukkit.launcher
  "A plugin that launches mobs based on the item type the player
  is holding. For example, using a piece of leather will launch 
  a cow. The cow will be on fire and will explode when it hits
  the ground."
  (:require [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.listener :as listener])
  (:import [org.bukkit.command CommandExecutor]
           [org.bukkit.plugin Plugin]
           [org.bukkit.event.player PlayerInteractEvent] 
           [org.bukkit.event.block Action]
           [org.bukkit.entity Player LivingEntity]
           [org.bukkit.inventory ItemStack]
           [org.bukkit Material Location World]
           [org.bukkit.util Vector]
           [org.bukkit.scheduler BukkitRunnable]))


(def token-tree {"on" nil
                 "off" nil
                 "power" nil})

;; State for the launcher plugin is held in config.yml
;; This can be managed through the /launcher command.
(defn launcher-command
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      (condp = (vec args)
        ["on"] (do (.. plugin (getConfig) (set "launcher.on" true)) true)
        ["off"] (do (.. plugin (getConfig) (set "launcher.on" false)) true)
        (if (and (= "power" (first args))
                 (util/is-numeric? (second args)))
          (do (.. plugin (getConfig) (set "launcher.power" (Float/parseFloat (second args))))
              true)
          false)))))

(def material->entity
  {Material/LEATHER     org.bukkit.entity.Cow
   Material/RABBIT_HIDE org.bukkit.entity.Rabbit
   Material/PORK        org.bukkit.entity.Pig
   Material/GOLD_NUGGET org.bukkit.entity.PigZombie
   Material/FEATHER     org.bukkit.entity.Chicken
   Material/STRING      org.bukkit.entity.Spider
   Material/CARPET      org.bukkit.entity.Llama
   ;; can add more here later...
   })

(def vector-mult 3)
(def default-power 4.0)

(defn fire [^Plugin plugin
            ^PlayerInteractEvent event]
  "Spawn the specified entity and launch it!"
  (let [^Player player (.getPlayer event)
        ^World world (.getWorld player)
        ^Material material (util/material (.getItem event))
        entity-class (material->entity material)
        ^Location loc (.add (util/location player) 0 1 0)
        ^Vector v (.multiply (.getDirection loc) vector-mult) 
        ^LivingEntity entity (.spawn world loc entity-class)
        power (float (.. plugin getConfig (getDouble "launcher.power" default-power)))

        ;; This runnable checks repeatedly to see if the entity has hit
        ;; the ground yet. When it does, it will explode and die in a fire.
        ^BukkitRunnable task
        (proxy [BukkitRunnable] []
          (run []
            (if (.isOnGround entity)
              (do
                (.createExplosion world (.getLocation entity) power true)
                (.setHealth entity 0)
                (.cancel this))
              (do
                (.setFireTicks entity 20)
                (.setHealth entity (.getMaxHealth entity))))))]
    ;; Launch the entity and set it on fire!
    (.setVelocity entity v)
    (.setFireTicks entity 20)

    ;; Start the runnable task
    (.runTaskTimer task plugin 0 0)))

;; These are the actions we are acting on...
(def act? #{Action/LEFT_CLICK_AIR Action/LEFT_CLICK_BLOCK})

(defn make-launcher-listener
  [^Plugin plugin]
  (fn [^PlayerInteractEvent event]
    (let [^ItemStack item (.getItem event)
          ^Action action (.getAction event) ]
      (if (and
            (act? action)
            (.. plugin (getConfig) (getBoolean "launcher.on"))
            item
            (material->entity (util/material item)))
        (fire plugin event)))))


(defn register-listeners
  [plugin]
  (listener/register-listener plugin PlayerInteractEvent
                              (make-launcher-listener plugin)))
