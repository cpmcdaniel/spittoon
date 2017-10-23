(ns org.kowboy.spigot-repl
  (:require [clojure.tools.namespace.repl :as tn]
            [clojure.tools.nrepl.server :as nrepl]))

(def repl-server (atom nil))
(def plugin-ref (atom nil))
(def server (atom nil))

(defn start-repl
  [repl]
  (when (not repl)
    (nrepl/start-server :port 7788)))

(defn stop-repl
  [repl]
  (nrepl/stop-server repl))

(defn go
  ([plugin]
   (swap! repl-server start-repl)
   (reset! plugin-ref plugin)
   (reset! server (.getServer plugin))
   :ready)
  ([]
   (swap! repl-server start-repl)
   :ready))

(defn stop
  []
  (swap! repl-server stop-repl))

(defn reset
  []
  (tn/refresh :after 'org.kowboy.spigot-repl/go))

(defn enable [plugin]
  (go plugin)
  (.. plugin (getLogger) (info "enabled")))

(defn disable [plugin]
  (stop)
  (.. plugin (getLogger) (info "disabled")))
