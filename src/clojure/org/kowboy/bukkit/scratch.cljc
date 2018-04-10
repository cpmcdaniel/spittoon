(ns org.kowboy.bukkit.scratch
  (:require [org.kowboy.bukkit.listener :refer [register-listener]]
            [org.kowboy.bukkit.util :as util]
            [org.kowboy.bukkit.block-finder :as bf]
            [org.kowboy.bukkit.chunks :as chunks]
            [org.kowboy.bukkit.blocks :as blocks]
            [clojure.string :as str])
  (:import [org.bukkit.enchantments Enchantment]
           [org.bukkit.entity Player]
           [org.bukkit.util BlockIterator Vector]
           [org.bukkit Material ChatColor Location]
           [org.bukkit.inventory ItemStack]
           [org.bukkit.block BlockFace]
           [org.bukkit.event.server TabCompleteEvent]
           [org.bukkit.material SimpleAttachableMaterialData Torch]))

(def ^:private plugin (atom nil))

(defn inject-plugin [p]
  (reset! plugin p))

(comment
  (in-ns 'org.kowboy.bukkit.scratch)
  (+ 2 3)
  (bean (.getServer @plugin))
  (def player (.. @plugin (getServer) (getPlayer "KowboyMac")))

  (def world (.getWorld player))

  (.. player getWorld getName)


  (def inventory (.getInventory player))

  (defn main-item [inventory]
    (.getItemInMainHand inventory))

  (def pick-axe (main-item inventory))
  (.getEnchantments pick-axe)

  (.addEnchantments pick-axe
                    {Enchantment/DURABILITY        (int 3)
                     Enchantment/DIG_SPEED         (int 5)
                     Enchantment/MENDING           (int 1)
                     Enchantment/LOOT_BONUS_BLOCKS (int 3)})

  (.getDataFolder @plugin)
  (.isSlimeChunk (.. player getLocation getChunk))

  (drop 10 (iterator-seq (BlockIterator. player 100)))
  ) 

