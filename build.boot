(set-env!
 :dependencies '[[org.spigotmc/spigot-api "1.12.2-R0.1-SNAPSHOT" :scope "provided"]
                 [org.clojure/clojure "1.8.0" :scope "runtime"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/tools.namespace "0.2.11"]]
 :source-paths #{"src/clojure" "src/java"}
 :resource-paths #{"src/resources"}
 :repositories [["clojars" {:url "https://clojars.org/repo/"}]
                ["central" {:url "http://repo.maven.apache.org/maven2/"}]
                ["spigot"  {:url "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"}]])

(task-options!
 pom {:project 'org.kowboy/spigot-repl
      :version "0.1.0"
      :description "Clojure REPL plugin for Spigot"
      :url "https://github.com/cpmcdaniel/SpigotREPL"
      :scm {:url "https://github.com/cpmcdaniel/SpigotREPL"}
      :license {"Eclipse Public License"
                "http://www.eclipse.org/legal/epl-v10.html"}}
 uber {:exclude-scope #{"provided"}}
 jar {:main 'org.kowboy.spigot-repl}
 aot {:namespace #{'org.kowboy.spigot-repl}}
 install {:pom "org.kowboy/spigot-repl"}
 push {:repo "clojars"
       :pom "org.kowboy/spigot-repl"}
 )

(deftask build
  []
  (comp (pom) (aot) (javac) (uber) (jar) (install)))

(deftask deploy
  []
  (comp (build) (push)))

(deftask dev
  []
  (comp (watch) (speak) (aot) (javac) (uber) (jar) (install)
        (sift :include [#"spigot.*\.jar"])
        (target)))
