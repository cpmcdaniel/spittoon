(ns org.kowboy.bukkit.plugin
  (:require [org.kowboy.bukkit.util :refer [info]]
            [org.kowboy.bukkit.launcher :as launcher]
            [org.kowboy.bukkit.zeus :as zeus]
            [org.kowboy.bukkit.repl :as repl]
            [org.kowboy.bukkit.block-finder :as finder]
            [org.kowboy.bukkit.xray :as xray]
            [org.kowboy.bukkit.excavator :as excavator]
            [org.kowboy.bukkit.eraser :as eraser]
            [org.kowboy.bukkit.journal :as journal]
            [org.kowboy.bukkit.listener :as listener])
  (:import [org.bukkit.command CommandSender]))

(defn register-command [plugin command-name executor]
  (.. plugin
      (getCommand command-name)
      (setExecutor executor)))

(defn register-commands
  [plugin]
  (register-command plugin "repl"     (repl/repl-command plugin))
  (register-command plugin "zeus"     (zeus/zeus-command plugin))
  (register-command plugin "launcher" (launcher/launcher-command plugin))
  (register-command plugin "xray"     (xray/xray-command plugin))
  (register-command plugin "find"     (finder/find-command plugin))
  (register-command plugin "excavate" (excavator/excavate-command plugin))
  (register-command plugin "eraser"   (eraser/eraser-command plugin))
  (register-command plugin "journal"  (journal/journal-command plugin))
  
  ;; tab completions
  (listener/register-tab-completer 
    plugin
    {"repl" repl/token-tree
     "zeus" zeus/token-tree
     "launcher" launcher/token-tree
     "find" finder/token-tree
     "eraser" eraser/token-tree
     "journal" journal/token-tree}))

(defn register-listeners
  [plugin]
  (launcher/register-listeners plugin)
  (zeus/register-listeners plugin)
  (eraser/register-listeners plugin))

(defn enable [plugin]
  ;; be sure to write out plugins/Spittoon/config.yaml
  ;; does not overwrite if it already exists.
  (.saveDefaultConfig plugin)
  
  ;; Start a REPL and log the port number.
  (.. plugin (getLogger) (info (format "REPL started on port %d."
                                       (:port (repl/start! plugin)))))
  (register-commands plugin)
  (register-listeners plugin))

(defn disable [plugin]
  (repl/stop! plugin)
  (.saveConfig plugin))
