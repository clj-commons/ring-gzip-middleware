(defproject amalloy/ring-gzip-middleware "0.1.3"
  :description "Ring gzip encoding middleware"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:1.2.0 {:dependencies [[org.clojure/clojure "1.2.0"]]}
             :1.3.0 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4.0 {}}
  :aliases {"all" ["with-profile" "1.2.0:1.3.0:1.4.0"]}
  :min-lein-version "2.0.0")
