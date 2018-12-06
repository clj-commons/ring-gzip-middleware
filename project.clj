(defproject amalloy/ring-gzip-middleware "0.1.4-SNAPSHOT"
  :url "https://github.com/clj-commons/ring-gzip-middleware"
  :description "Ring gzip encoding middleware"
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:1.10 {:dependencies [[org.clojure/clojure "1.10.0-RC3"]]}
             :1.9  {}
             :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6  {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :aliases {"all" ["with-profile" "1.6:1.7:1.8:1.9:1.10"]}
  :min-lein-version "2.0.0")
