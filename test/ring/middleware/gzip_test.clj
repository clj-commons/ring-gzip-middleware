(ns ring.middleware.gzip-test
  (:use clojure.test
        ring.middleware.gzip)
  (:require [clojure.java.io :as io])
  (:import (java.util Arrays))
  (:import (java.io StringBufferInputStream ByteArrayOutputStream))
  (:import (java.util.zip GZIPInputStream)))

(defn- to-byte-array [inputstream]
  (let [buffer (ByteArrayOutputStream.)]
    (io/copy inputstream buffer)
    (.toByteArray buffer)))

(defn unzip [in]
  (let [in (GZIPInputStream. in)
        bytes (to-byte-array in)]
    (.close in)
    bytes))

(defn encoding [resp]
  ((:headers resp) "Content-Encoding"))

(def output (apply str (repeat 300 "a")))

(def app (wrap-gzip (fn [req] {:status 200
                               :body output
                               :headers {}})))

(defn accepting [ctype]
  {:headers {"accept-encoding" ctype}})

(deftest test-basic-gzip
  (let [resp (app (accepting "gzip"))]
    (is (= 200 (:status resp)))
    (is (= "gzip" (encoding resp)))
    (is (Arrays/equals (unzip (resp :body)) (.getBytes output)))))

(deftest test-inputstream-gzip
  (let [app (wrap-gzip (fn [req] {:status 200
                                  :body (StringBufferInputStream. output)
                                  :headers {}}))
        resp (app (accepting "gzip"))]
    (is (= 200 (:status resp)))
    (is (= "gzip" (encoding resp)))
    (is (Arrays/equals (unzip (resp :body)) (.getBytes output)))))

(deftest test-string-seq-gzip
  (let [seq-body (->> (partition-all 20 output)
                      (map (partial apply str)))
        app (wrap-gzip (fn [req] {:status 200
                                  :body seq-body
                                  :headers {}}))
        resp (app (accepting "gzip"))]
    (is (= 200 (:status resp)))
    (if @@#'ring.middleware.gzip/flushable-gzip?
      (do
        (println "Running on JDK7+, testing gzipping of seq response bodies.")
        (is (= "gzip" (encoding resp)))
        (is (Arrays/equals (unzip (resp :body)) (.getBytes output))))
      (do 
        (println "Running on <=JDK6, testing non-gzipping of seq response bodies.")
        (is (nil? (encoding resp)))
        (is (= seq-body (resp :body)))))))

(deftest test-accepts
  (doseq [ctype ["gzip" "*" "gzip,deflate" "gzip,deflate,sdch"
                 "gzip, deflate" "gzip;q=1" "deflate,gzip"
                 "deflate,gzip,sdch" "deflate,gzip;q=1"
                 "deflate,gzip;q=1,sdch"
                 "gzip;q=0.5"]]
    (is (= "gzip" (encoding (app (accepting ctype))))))
  (doseq [ctype ["" "gzip;q=0" "deflate" "deflate,sdch"
                 "deflate,gzip;q=0" "deflate,gzip;q=0,sdch"
                 "gzip;q=0,deflate" "*;q=0"]]
    (is (nil? (encoding (app (accepting ctype)))))))

(deftest test-multiple-accepts
  (is (= "gzip" (encoding (app (accepting ["gzip,deflate" "deflate"])))))
  (is (nil? (encoding (app (accepting ["deflate" "sdch"]))))))

(deftest test-min-length
  "don't compress string bodies less than 200 characters long"
  (let [output (apply str (repeat 10 "a"))
        app (wrap-gzip (fn [req] {:status 200
                                  :body output
                                  :headers {}}))
        resp (app (accepting "gzip"))]
    (is (nil? (encoding (app (accepting "gzip")))))))

(deftest test-wrapped-encoding
  "don't compress responses which already have a content-encoding header"
  (let [app (wrap-gzip (fn [req] {:status 200
                                  :body output
                                  :headers {"Content-Encoding" "text"}}))
        resp (app (accepting "gzip"))]
    (is (= "text" (encoding resp)))
    (is (= output (:body resp)))))

(deftest test-status
  "don't compress non-200 responses"
  (let [app (wrap-gzip (fn [req] {:status 404
                                  :body output
                                  :headers {}}))
        resp (app (accepting "gzip"))]
    (is (nil? (encoding resp)))
    (is (= output (:body resp)))))
