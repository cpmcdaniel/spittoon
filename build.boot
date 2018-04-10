(require 'boot.repl)
(swap! boot.repl/*default-dependencies*
       concat '[[cider/cider-nrepl "0.17.0-snapshot"]])

(swap! boot.repl/*default-middleware*
       concat '[cider.nrepl/cider-middleware])

(set-env!
 :dependencies '[[org.spigotmc/spigot-api "1.12.2-R0.1-SNAPSHOT" :scope "provided"]
                 [onetom/boot-lein-generate "0.1.3" :scope "test"]
                 [org.clojure/clojure "1.8.0" :scope "runtime"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/core.match "0.3.0-alpha5"]
                 [cider/cider-nrepl "0.17.0-snapshot"]]
 :source-paths #{"src/clojure" "src/java"}
 :resource-paths #{"src/resources"}
 :repositories [["clojars" {:url "https://clojars.org/repo/"}]
                ["central" {:url "http://repo.maven.apache.org/maven2/"}]
                ["spigot"  {:url "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"}]])

(task-options!
 pom {:project 'org.kowboy/spittoon
      :version "0.1.0"
      :description "KowboyMac's Clojure Minecraft Plugin"
      :url "https://github.com/cpmcdaniel/spittoon"
      :scm {:url "https://github.com/cpmcdaniel/spittoon"}
      :license {"Eclipse Public License"
                "http://www.eclipse.org/legal/epl-v10.html"}}
 uber {:exclude-scope #{"provided"}}
 aot {:all true}
 install {:pom "org.kowboy/spittoon"}
 push {:repo "clojars"
       :pom "org.kowboy/spittoon"}
 )

(require 'boot.lein)
(boot.lein/generate)


(deftask build
  []
  (comp (speak) (pom) (aot) (javac) (uber) (jar) (install)))

(deftask deploy
  []
  (comp (build) (push)))

(deftask dev
  []
  (comp (watch) (build)
        (sift :include [#"spittoon.*\.jar"])
        (target)))
