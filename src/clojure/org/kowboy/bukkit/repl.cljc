(ns org.kowboy.bukkit.repl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [org.kowboy.bukkit.listener :as listener]
            [org.kowboy.bukkit.scratch :as scratch])
  (:import [org.bukkit.command CommandExecutor]))

(def token-tree {"on" nil "off" nil})

(def ^:private repl-server (atom nil))

(defn stop!
  "If the REPL is running, stop it."
  ([plugin sender]
   (swap! repl-server
          (fn [server]
            (if server
              (do (nrepl/stop-server server)
                  (scratch/inject-plugin nil)
                  (when sender
                    (.sendMessage sender "REPL stopped.")))
              (when sender
                (.sendMessage sender "REPL is not running."))))))

  ([plugin] (stop! plugin nil)))

(defn start!
  "If the REPL is not running, start it."
  ([plugin sender]
   (swap! repl-server
          (fn [server]
            (if server
              ;; already running...
              (do (when sender
                    (.sendMessage sender
                                  (format "REPL is already running on port %d."
                                          (:port server))))
                  ;; return the current value
                  server)
              ;; not running, start a new server and return it
              (let [new-server (nrepl/start-server :handler cider-nrepl-handler)]
                (scratch/inject-plugin plugin)
                (when sender
                  (.sendMessage sender (format "REPL started on port %d"
                                               (:port new-server))))
                new-server)))))

  ([plugin] (start! plugin nil)))

(defn repl-command
  [plugin]
  (reify CommandExecutor
    (onCommand [this
                sender
                cmd
                label
                args]
      (condp = (vec args)
        ["on"]  (do (start! plugin sender) true)
        ["off"] (do (stop! plugin sender) true)
        false))))
