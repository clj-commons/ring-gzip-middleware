(defproject org.clj-commons/ring-gzip-middleware
  (or (System/getenv "PROJECT_VERSION") "0.1.5")
  :url "https://github.com/clj-commons/ring-gzip-middleware"
  :description "Ring gzip encoding middleware"
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:1.12 {:dependencies [[org.clojure/clojure "1.12.0"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.9  {}
             :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6  {:dependencies [[org.clojure/clojure "1.6.0"]]}}
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_org_clj_commons_password
                                    :sign-releases true}]]
  :aliases {"all" ["with-profile" "1.6:1.7:1.8:1.9:1.10:1.11:1.12"]}
  :min-lein-version "2.0.0")
