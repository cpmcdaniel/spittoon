(ns org.kowboy.bukkit.util
  (:import [org.bukkit.plugin Plugin]
           [org.bukkit.block Block]
           [org.bukkit.entity Entity EntityType]
           [org.bukkit.command CommandSender]
           [org.bukkit.inventory ItemStack]
           [org.bukkit.material MaterialData]
           [org.bukkit Location Material Chunk World]))

(defn- get-message
  [^String message & args]
  (if (pos? (count args))
    (apply format message args)
    message))

(defn warn [^Plugin plugin ^String message & args]
  (.. plugin (getLogger) (warning (get-message message args))))

(defn info [^Plugin plugin ^String message & args]
  (.. plugin (getLogger) (info (get-message message args))))

(defn debug [^Plugin plugin ^String message & args]
  (.. plugin (getLogger) (fine (get-message message args))))


;; Utility functions
(defn is-numeric?
  "Is the string a number?"
  [^String s]
  (re-matches #"\d+(\.\d+)?" s))

(defn send-message
  [^CommandSender sender messages]
  (.sendMessage sender (if (sequential? messages)
                         (into-array String messages)
                         (str messages))))

;; Protocols and convenience for better interop.

(defprotocol HasLocation
  (location [thingy])
  (location-str [thingy])
  (distance [thing1 thing2])
  (world [thingy])
  (block [thingy]))

(extend-protocol HasLocation
  Location
  (location [loc] loc)
  (location-str [^Location loc]
    (format "(%d, %d, %d)"
            (int (.getX loc))
            (int (.getY loc))
            (int (.getZ loc))))
  (distance [^Location loc1 thingy2]
    (.distance loc1 ^Location (location thingy2)))
  (world [^Location loc] (.getWorld loc))
  (block [^Location loc] (.getBlock loc))

  Block
  (location [^Block block] (.getLocation block))
  (location-str [block] (location-str (location block)))
  (distance [^Block block thingy2]
    (distance (location block) thingy2))
  (world [^Block block] (.getWorld block))
  (block [b] b)

  Entity
  (location [^Entity entity] (.getLocation entity))
  (location-str [entity] (location-str (location entity)))
  (distance [^Entity entity thingy2] 
    (distance (location entity) thingy2))
  (world [^Entity entity] (.getWorld entity))
  (block [^Entity entity] (block (location entity))))

(defprotocol HasMaterial
  (material [thingy]))

(extend-protocol HasMaterial
  Block
  (material [^Block block] (.getType block))

  ItemStack
  (material [^ItemStack stack] (.getType stack))
  
  MaterialData
  (material [^MaterialData md] (.getItemType md))
  
  String
  (material [^String s]
    (try (Material/valueOf (.toUpperCase s))
         (catch IllegalArgumentException e nil))))

(defprotocol HasEntityType
  (entity-type [thingy]))

(extend-protocol HasEntityType
  Entity
  (entity-type [^Entity entity] (.getType entity))

  String
  (entity-type [^String s]
    (try (EntityType/valueOf (.toUpperCase s))
       (catch IllegalArgumentException e nil))))

(defprotocol HasDataValue
  (dv [thingy]))

(extend-protocol HasDataValue
  Block
  (dv [^Block block] (.getData block)))


(defprotocol HasEnvironment
  (environment [thingy]))

(extend-protocol HasEnvironment
  World
  (environment [^World world] (.. world getEnvironment toString))
  
  Chunk
  (environment [^Chunk ch] (environment (.getWorld ch)))

  Location
  (environment [^Location loc] (environment (.getWorld loc))))
