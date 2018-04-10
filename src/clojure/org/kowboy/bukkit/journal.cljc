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
           [org.bukkit Location]
           [org.bukkit.command CommandExecutor]
           [org.bukkit.entity Player]
           [org.bukkit.plugin Plugin]
           [org.bukkit.configuration.file YamlConfiguration]))

(def token-tree {"add" nil
                 "show" nil
                 "rem" nil
                 "tp" nil})

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

(defn parse-entry-arg
  "Parses the required journal entry number for the 'rem' and 'tp' commands."
  [{:keys [args] :as ctx} command-key]
  (if (not= 1 (count args))
    (assoc ctx :bad-args true)
    (if-let [entry (try (Integer/parseInt (first args))
                        (catch NumberFormatException e nil))]
      (assoc ctx
             :command command-key 
             :entry entry)
      (assoc ctx
             :bad-args true
             :errors [(format "%s is not a valid entry number" (first args))]))))

(defn parse-args
  [{:keys [player args plugin] :as ctx}]
  (let [first-arg (first args)]
    (as-> (update ctx :args rest) ctx
      (condp = first-arg
        "add"  (assoc ctx 
                      :command :add 
                      :message (str/join " " (:args ctx)))
        "show" (parse-show-args ctx)
        "rem"  (parse-entry-arg ctx :rem)
        "tp"   (parse-entry-arg ctx :tp)
        ;; first argument not recognized
        (assoc ctx 
               :bad-args true
               :errors [(format "%s not recognized as first argument."
                                first-arg)])))))

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

(defn get-entries [journal]
  (vec (.getStringList journal "entries")))
(def page-size 8)

(defmethod journal :show
  [{:keys [journal page player] :as ctx}]
  (let [entries (get-entries journal)
        start-index (* (dec page) page-size)
        page-entries (->> entries
                          (drop start-index)
                          (take page-size)
                          (map-indexed #(str (+ 1 (* page-size (dec page)) %1) ". " %2)))
        total-pages (inc (quot (count entries) page-size))]
    (util/send-message player page-entries)
    (when (and (< 1 total-pages)
               (<= page total-pages))
      (util/send-message player (format "Page %d of %d" page total-pages))))
  ctx)

(defmethod journal :rem
  [{:keys [plugin player journal entry] :as ctx}]
  (let [entry-vec (get-entries journal)]
    (.set journal "entries" 
          (into-array String (concat 
                               (subvec entry-vec 0 (dec entry))
                               (subvec entry-vec entry))))
    (.save journal (journal-file plugin player)))
  ctx)

(def location-re #"\((-?\d+),\s(-?\d+),\s(-?\d+)\)")
(defn- get-entry-loc [{:keys [player journal entry]}]
  (let [entry-str (get (get-entries journal) (dec entry))
        [x y z]
        (when entry-str
          (->> (re-matcher location-re entry-str)
               (re-find)
               (rest) ;; lose the full match, we only care about the groups
               (map #(Integer/parseInt %))))]
    (when (and x y z) (Location. (util/world player) x y z))))

(defmethod journal :tp
  [{:keys [plugin player journal entry] :as ctx}]
  (if-let [loc (get-entry-loc ctx)]
    (do (.teleport player loc)
        ctx)
    (assoc ctx :errors
               (format "Can't find location for entry %d" entry))))

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
