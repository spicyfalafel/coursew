(defproject coursew "0.1.0-SNAPSHOT"
    :main coursew.server
    :aot [coursew.server]
    :dependencies [
                   [ring/ring-core            "1.9.5"]
                   ;[puppetlabs/ring-middleware "1.3.1"]
                   [compojure                 "1.6.2"]
                   [org.clojure/clojure       "1.10.3"]
                   [org.immutant/web          "2.1.10"]
                   [rum                       "0.12.8"]
                   [org.clojure/clojurescript "1.11.4" :scope "provided"]
                   [ring/ring-json "0.5.1"]
                   ;[org.clojure/data.json "2.4.0"]
                   [cheshire "5.10.2"]
                   [ring-cors "0.1.13"]
                   ;; db
                   [hikari-cp "2.13.0"]
                   ;[com.github.seancorfield/next.jdbc "1.2.772"]
                   [org.clojure/java.jdbc "0.7.9"]
                   [org.postgresql/postgresql "42.3.3"]
                   [com.github.seancorfield/honeysql "2.2.868"]
                   [clojure.java-time "0.3.3"]])
