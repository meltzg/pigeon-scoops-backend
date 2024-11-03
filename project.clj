(defproject pigeon-scoops-backend "0.1.0-SNAPSHOT"
  :description "Pigeon Scoops manager API"
  :url "http://api.pigeon-scoops.com/"
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [ring "1.13.0"]
                 [integrant "0.13.1"]
                 [environ "1.2.0"]
                 [metosin/reitit "0.7.2"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.postgresql/postgresql "42.7.4"]
                 [clj-http "3.13.0"]
                 [ovotech/ring-jwt "2.3.0"]]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:source-paths ["dev/src"]
                   :resource-paths ["dev/resources"]
                   :dependencies [[ring/ring-mock "0.4.0"]
                                  [integrant/repl "0.3.3"]]}}
  :uperjar-name "pigeon-scoops.jar")
