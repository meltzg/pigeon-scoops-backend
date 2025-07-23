(defproject pigeon-scoops-backend "0.1.0-SNAPSHOT"
  :description "Pigeon Scoops manager API"
  :url "http://api.pigeon-scoops.com/"
  :min-lein-version "2.0.0"
  :main pigeon-scoops-backend.server
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [ring "1.14.2"]
                 [integrant "0.13.1"]
                 [environ "1.2.0"]
                 [metosin/reitit "0.9.1"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.postgresql/postgresql "42.7.7"]
                 [clj-http "3.13.1"]
                 [ovotech/ring-jwt "2.3.0"]
                 [camel-snake-kebab "0.4.3"]
                 [com.zaxxer/HikariCP "6.3.1"]
                 [org.flywaydb/flyway-database-postgresql "11.10.3"]
                 [ring-cors "0.1.13"]
                 [ring/ring-codec "1.3.0"]
                 [com.github.seancorfield/honeysql "2.7.1310"]
                 [org.clojure/tools.cli "1.1.230"]

                 ;; Logging
                 [ch.qos.logback/logback-classic "1.5.18"]
                 [ch.qos.logback/logback-core "1.5.18"]
                 [org.slf4j/slf4j-api "2.0.17"]
                 [org.slf4j/jcl-over-slf4j "2.0.17"]
                 [org.slf4j/log4j-over-slf4j "2.0.17"]
                 [org.slf4j/osgi-over-slf4j "2.0.17"]
                 [org.slf4j/jul-to-slf4j "2.0.17"]
                 [org.apache.logging.log4j/log4j-to-slf4j "2.25.1"]]
  :plugins [[lein-ancient "0.7.0"]
            [com.github.clj-kondo/lein-clj-kondo "0.2.5"]]
  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths   ["dev/src"
                                        "test"]
                       :resource-paths ["dev/resources"]
                       :dependencies   [[ring/ring-mock "0.6.1"]
                                        [integrant/repl "0.4.0"]
                                        [org.testcontainers/postgresql "1.21.3"]]}}
  :uberjar-name "pigeon-scoops.jar")
