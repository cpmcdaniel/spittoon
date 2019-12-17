(ns org.kowboy.bukkit.blocks
  (:require [org.kowboy.bukkit.util :as util])
  (:import [org.bukkit Material ChatColor]
           [org.bukkit.block Block]))

(defn material-predicate
  "Makes a block predicate for the given material."
  [& materials]
  (fn [^Block b] ((set materials) (util/material b))))

(def air? (material-predicate Material/AIR))
(def bedrock? (material-predicate Material/BEDROCK))
(def water? (material-predicate Material/WATER))
(defn liquid? [^Block b] (.isLiquid b))

;; Chat colors for materials!
(def color->materials
  {ChatColor/DARK_GRAY  [Material/COAL_ORE]
   ChatColor/DARK_GREEN [Material/GRASS 
                         Material/DIRT]
   ChatColor/GREEN      [Material/EMERALD_ORE]
   ChatColor/DARK_RED   [Material/REDSTONE_ORE]
   ChatColor/DARK_AQUA  [Material/AIR]
   ChatColor/DARK_PURPLE [Material/CLAY]
   ChatColor/BLUE       [Material/WATER
                         Material/LAPIS_ORE]
   ChatColor/WHITE      [Material/NETHER_QUARTZ_ORE
                         Material/IRON_ORE]
   ChatColor/GOLD       [Material/GOLD_ORE]
   ChatColor/AQUA       [Material/DIAMOND_ORE]
   ChatColor/YELLOW     [Material/GLOWSTONE
                         Material/SAND
                         Material/SANDSTONE]
   ChatColor/RED        [Material/LAVA]
   })

(def color (into {} (for [[color materials] color->materials
                          material materials]
                      [material color])))

(defn color-str
  "Returns a colorized, lower-case version of the block type."
  [^Block block]
  (let [m (util/material block)]
    (str (color m ChatColor/GRAY) 
         (.toLowerCase (str m))
         ChatColor/RESET)))

(defn data-value
  "Returns the MaterialData data value (byte) for a block. This
  may be helpful in identifying the type of stone or wood a block
  is made of, for example."
  [^Block block]
  (.. block (getData)))
