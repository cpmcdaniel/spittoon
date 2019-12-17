(ns org.kowboy.bukkit.repl
  (:require [nrepl.server :as nrepl]
            [org.kowboy.bukkit.scratch :as scratch])
  (:import [org.bukkit.command CommandExecutor]
           [org.bukkit.plugin Plugin]))

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

(defn nrepl-handler []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(defn start!
  "If the REPL is not running, start it."
  ([^Plugin plugin sender]
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
              (let [port (.. plugin (getConfig) (getInt "repl.port" 0))
                    new-server (nrepl/start-server :handler (nrepl-handler)
                                                   :port port)]
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
