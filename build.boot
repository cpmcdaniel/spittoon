(set-env!
 :dependencies '[[org.spigotmc/spigot-api "1.13.2-R0.1-SNAPSHOT" :scope "provided"]
                 [onetom/boot-lein-generate "0.1.3" :scope "test"]
                 [org.clojure/clojure "1.10.0" :scope "runtime"]
                 [org.clojure/core.match "0.3.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [quil "3.0.0" :exclusions [com.lowagie/itext
                                            org.bouncycastle/bctsp-jdk14]]
		         [cider/cider-nrepl "0.21.1"]]
 :source-paths #{"src/clojure" "src/java"}
 :resource-paths #{"src/resources"}
 :repositories [["central" {:url "https://repo1.maven.org/maven2/"}]
                ["clojars" {:url "https://clojars.org/repo/"}]
                ["spigot"  {:url "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"}]
                ["bungeecord" {:url "https://oss.sonatype.org/content/repositories/snapshots"}]])

(task-options!
 pom {:project 'org.kowboy/spittoon
      :version "0.1.1"
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
  (comp (build)
        (sift :include [#"spittoon.*\.jar"])
        (target)))
