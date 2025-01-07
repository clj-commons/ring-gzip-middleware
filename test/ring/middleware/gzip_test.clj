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

(defn set-encoding
  ([resp] (set-encoding resp false))
  ([resp caps?]
   (let [content-encoding (if caps? "Content-Encoding" "content-encoding")]
     (assoc-in resp [:headers content-encoding] "text"))))

(deftest test-basic-gzip
  (let [resp (app (accepting "gzip"))]
    (is (= 200 (:status resp)))
    (is (= "gzip" (encoding resp)))
    (is (Arrays/equals (unzip (resp :body)) (.getBytes output)))))

(deftest test-basic-gzip-async
  (testing "middleware should work with 3-arg async handlers as well"
    (let [app (wrap-gzip
               (fn [request respond raise]
                 (respond {:status 200
                           :body output
                           :headers {}})))
          resp (app (accepting "gzip") identity identity)]
      (is (= 200 (:status resp)))
      (is (= "gzip" (encoding resp)))
      (is (Arrays/equals (unzip (resp :body)) (.getBytes output))))))

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
  (testing "appropriate requests will be zipped"
    (doseq [ctype ["gzip" "*" "gzip,deflate" "gzip,deflate,sdch"
                   "gzip, deflate" "gzip;q=1" "deflate,gzip"
                   "deflate,gzip,sdch" "deflate,gzip;q=1"
                   "deflate,gzip;q=1,sdch"
                   "gzip;q=0.5"]]
      (is (= "gzip" (encoding (app (accepting ctype)))))
      (is (accepts-gzip? (accepting ctype)))))
  (testing "requests that ask for a zip, but not the supported type of zip are not zipped"
    (doseq [ctype ["" "gzip;q=0" "deflate" "deflate,sdch"
                   "deflate,gzip;q=0" "deflate,gzip;q=0,sdch"
                   "gzip;q=0,deflate" "*;q=0"]]
      (is (nil? (encoding (app (accepting ctype))))))))

(deftest test-min-length
  (testing "Compress string bodies greater than the min-length (200) characters long"
    (let [output (apply str (repeat (inc min-length) "a"))
          resp {:status 200
                :body output
                :headers {}}
          app (wrap-gzip (fn [req] resp))]
      (is (= "gzip" (encoding (app (accepting "gzip")))))
      (is (supported-response? resp))
      (testing ", but not string bodies at or below min-length"
        (let [resp (update resp :body subs 1)
              app (wrap-gzip (fn [req] resp))]
          (is (nil? (encoding (app (accepting "gzip")))))
          (is (not (supported-response? resp))))))))

(deftest test-wrapped-encoding
  (testing "don't compress responses which already have a content-encoding header"
    (let [response {:status 200
                    :body output
                    :headers {"Content-Encoding" "text"}}
          app (wrap-gzip (fn [req] response))
          resp (app (accepting "gzip"))]
      (is (= "text" (encoding resp)))
      (is (= output (:body resp))))))

(deftest test-supported
  (testing "responses that already have an encoding cannot be zipped"
    (doseq [ctype ["gzip" "*" "gzip,deflate" "gzip,deflate,sdch"
                   "gzip, deflate" "gzip;q=1" "deflate,gzip"
                   "deflate,gzip,sdch" "deflate,gzip;q=1"
                   "deflate,gzip;q=1,sdch"
                   "gzip;q=0.5" "" "gzip;q=0" "deflate" "deflate,sdch"
                   "deflate,gzip;q=0" "deflate,gzip;q=0,sdch"
                   "gzip;q=0,deflate" "*;q=0"]]
      (is (not (supported-response? (set-encoding (accepting ctype)))))
      (is (not (supported-response? (set-encoding (accepting ctype) true)))))))

(deftest test-status
  (testing "don't compress non-2xx responses"
    (let [app (wrap-gzip (fn [req] {:status 404
                                    :body output
                                    :headers {}}))
          resp (app (accepting "gzip"))]
      (is (nil? (encoding resp)))
      (is (= output (:body resp))))))

(deftest test-setting-headers
  (testing "updating the headers of a response to indicate that they have been gziped"
    (is (= {"Content-Encoding" "gzip"} (set-response-headers {"Content-Length" 201})))
    (is (= {"Content-Encoding" "gzip" "Age" 24} (set-response-headers {"Age" 24})))))
