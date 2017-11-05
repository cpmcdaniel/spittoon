(ns org.kowboy.bukkit.listener
  (:require [clojure.string :as str])
  (:import [org.bukkit.event Event Listener EventPriority]
           [org.bukkit.event.server TabCompleteEvent]
           [org.bukkit.plugin EventExecutor]))

(defn register-listener
  "Registers a handler fn for a specific Event type."
  ([plugin event-class handler
    {:keys [priority ignore-cancelled]
     :or {priority EventPriority/NORMAL
          ignore-cancelled false}}]
   (let [executor-listener
         (reify Listener EventExecutor
           (^void execute [this ^Listener listener ^Event event]
            (handler event)))]
     (.. plugin (getServer)
         (getPluginManager)
         (registerEvent event-class
                        executor-listener
                        priority
                        executor-listener
                        plugin
                        ignore-cancelled))))
  ([plugin event-class handler]
   (register-listener plugin event-class handler {})))

(defn- command-regex
  [command-name]
  (re-pattern (str "^/?" command-name "\\s")))

(defn- trim-slash
  [^String buff]
  (if (.startsWith buff "/")
    (.substring buff 1)
    buff))

(defn- arg-filter
  "Creates a filter given the arg."
  [arg]
  (fn [completion] 
    (and 
      ;; don't complete if it's already complete
      (not= arg completion)
      (.startsWith completion arg))))

(defn get-completions
  "Takes the given command tokens and token-tree map. Returns
  the seq of completions."
  [tokens token-tree]
  (loop [tokens tokens
         token-tree token-tree]
    (cond 
      (= 1 (count tokens))
      (sort (filterv (arg-filter (first tokens)) (keys token-tree)))

      (and (seq tokens)
           (not (nil? token-tree))
           (seq (token-tree (first tokens))))
      (recur (rest tokens) (token-tree (first tokens)))

      :else
      [])))

(defn- get-tokens
  "If the buffer ends with whitespace, append an empty token.
  This allows completion of the next argument using all keys in
  the token-tree."
  [buff]
  (let [tokens (str/split buff #"\s")]
    (if (re-find #"\s$" buff)
      (concat tokens [""])
      tokens)))

(defn- tab-completer
  [token-tree]
  (fn [^TabCompleteEvent e]
    (.sendMessage (.getSender e) (.getBuffer e))
    (let [tokens (get-tokens (trim-slash (.getBuffer e)))]
      (when-let [completions (get-completions (rest tokens) 
                                              (token-tree (first tokens)))]
        (when (seq completions)
          (.setCompletions e completions))))))

(defn register-tab-completer
  [plugin token-tree]
  (register-listener plugin TabCompleteEvent (tab-completer token-tree)))
