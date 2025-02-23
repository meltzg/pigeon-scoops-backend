(defproject pigeon-scoops-backend "0.1.0-SNAPSHOT"
  :description "Pigeon Scoops manager API"
  :url "http://api.pigeon-scoops.com/"
  :min-lein-version "2.0.0"
  :main pigeon-scoops-backend.server
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [ring "1.13.0"]
                 [integrant "0.13.1"]
                 [environ "1.2.0"]
                 [metosin/reitit "0.7.2"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [org.postgresql/postgresql "42.7.5"]
                 [clj-http "3.13.0"]
                 [ovotech/ring-jwt "2.3.0"]
                 [camel-snake-kebab "0.4.3"]
                 [com.zaxxer/HikariCP "6.2.1"]
                 [org.flywaydb/flyway-database-postgresql "11.3.3"]
                 [ring-cors "0.1.13"]
                 [com.github.seancorfield/honeysql "2.6.1270"]

                 ;; Logging
                 [ch.qos.logback/logback-classic "1.5.16"]
                 [ch.qos.logback/logback-core "1.5.16"]
                 [org.slf4j/slf4j-api "2.0.16"]
                 [org.slf4j/jcl-over-slf4j "2.0.16"]
                 [org.slf4j/log4j-over-slf4j "2.0.16"]
                 [org.slf4j/osgi-over-slf4j "2.0.16"]
                 [org.slf4j/jul-to-slf4j "2.0.16"]
                 [org.apache.logging.log4j/log4j-to-slf4j "2.24.3"]]
  :plugins [[lein-ancient "0.7.0"]]
  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths   ["dev/src"]
                       :resource-paths ["dev/resources"]
                       :dependencies   [[ring/ring-mock "0.4.0"]
                                        [integrant/repl "0.4.0"]
                                        [org.testcontainers/postgresql "1.20.5"]]}}
  :uberjar-name "pigeon-scoops.jar")
