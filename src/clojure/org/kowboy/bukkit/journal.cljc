(ns org.kowboy.bukkit.journal
  "Allows a player to add, show, and remove journal entries. These entries
  include a number (for deletion) and start with the coordinates where the
  entry was made. No timestamp is displayed, but entries will be kept in 
  chronological order. 
  
  Command summary:
  /journal add This is wonderful journal entry.
  /journal show [page number]
  /journal rem <entry number>
  
  Journals will be stored in the plugin data directory. A directory
  hierarchy will exist to separate users' journals. Each world will
  have it's own journal file. For the user 'KowboyMac', the file paths will
  be like so:
  - <data-dir>/KowboyMac/world_name.yml
  - <data-dir>/KowboyMac/world_name_nether.yml
  - <data-dir>/KowboyMac/world_name_the_end.yml
  "
  (:require [clojure.string :as str]
            [org.kowboy.bukkit.util :as util])
  (:import [java.io File]
           [org.bukkit.command CommandExecutor]
           [org.bukkit.entity Player]
           [org.bukkit.plugin Plugin]
           [org.bukkit.configuration.file YamlConfiguration]))

(def token-tree {"add" nil
                 "show" nil
                 "rem" nil})

(defn journal-file
  [^Plugin plugin ^Player player]
  ;; Here's to hoping names don't collide.
  (File. (.getDataFolder plugin) 
         (str (.getName player) "_" (.. player getWorld getName) ".yml")))

(defn parse-show-args
  "Parses the optional page number arg."
  [{:keys [args] :as ctx}]
  (cond 
    ;; More than one argument
    (> (count args) 1) 
    (assoc ctx :bad-args true)

    ;; Page number argument - validate it.
    (= 1 (count args))
    (if-let [page (try (Integer/parseInt (first args))
                       (catch NumberFormatException e nil))]
      (assoc ctx
             :command :show
             :page page)
      (assoc ctx 
             :bad-args true
             :errors [(format "%s is not a valid page number" (first args))]))

    ;; No page number, show first page.
    :else
    (assoc ctx :command :show :page 1)))

(defn parse-rem-args
  "Parses the required journal entry number for the 'rem' command."
  [{:keys [args] :as ctx}]
  (if (not= 1 (count args))
    (assoc ctx :bad-args true)
    (if-let [entry (try (Integer/parseInt (first args))
                        (catch NumberFormatException e nil))]
      (assoc ctx
             :command :rem
             :entry entry)
      (assoc ctx
             :bad-args true
             :errors [(format "%s is not a valid page number" (first args))]))))

(defn parse-args
  [{:keys [player args plugin] :as ctx}]
  (condp = (first args)
    "add"  (assoc ctx 
                  :command :add 
                  :message (str/join " " (rest args)))
    "show" (parse-show-args (update ctx :args rest))
    "rem"  (parse-rem-args  (update ctx :args rest))
    ;; first argument not recognized
    (assoc ctx 
           :bad-args true
           :errors [(format "%s not recognized as first argument."
                            (first args))])))

(defn load-journal
  "Loads the journal data from storage."
  [{:keys [plugin player] :as ctx}]
  (assoc ctx :journal
         (YamlConfiguration/loadConfiguration
           (journal-file plugin player))))

(defn save-journal
  "Persists the journal to storage."
  [{:keys [plugin player journal] :as ctx}]
  (.save journal (journal-file plugin player)))

(defmulti journal :command)

(defmethod journal :add
  [{:keys [journal plugin player message] :as ctx}]
  (let [entry-list (.getStringList journal "entries") ]
    ;; mutation!
    (.add entry-list 
          (str (util/location-str 
                 (.getBlock (util/location player)))
               ": " message)) 
    (.set journal "entries" entry-list)
    (.save journal (journal-file plugin player)))
  ctx)

(def page-size 8)

(defmethod journal :show
  [{:keys [journal page player] :as ctx}]
  (let [entry-list (.getStringList journal "entries")
        start-index (* (dec page) page-size)
        entries (->> entry-list
                     (drop start-index)
                     (take page-size)
                     (map-indexed #(str (+ 1 (* page-size (dec page)) %1) ". " %2)))
        total-pages (inc (quot (count entry-list) page-size))]
    (util/send-message player entries)
    (when (and (< 1 total-pages)
               (<= page total-pages))
      (util/send-message player (format "Page %d of %d" page total-pages))))
  ctx)

(defmethod journal :rem
  [{:keys [plugin player journal entry] :as ctx}]
  (let [entry-vec (vec (.getStringList journal "entries"))]
    (.set journal "entries" 
          (into-array String (concat 
                               (subvec entry-vec 0 (dec entry))
                               (subvec entry-vec entry))))
    (.save journal (journal-file plugin player)))
  ctx)

(defn journal-command
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      
      (if (not (instance? Player sender))
        (do (util/send-message sender "This is a player-only command.")
            true)
        (let [{:keys [errors bad-args] :as ctx} (parse-args {:plugin plugin
                                                             :player sender
                                                             :args (vec args)})]
          (when (seq errors) (util/send-message sender errors))
          (if bad-args
            false

            (do
              (journal (load-journal ctx))
              true)))))))
