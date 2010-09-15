(ns ring.middleware.gzip
  (:require [clojure.contrib.io :as io])
  (:import (java.util.zip GZIPOutputStream)
           (java.io InputStream
                    File
                    ByteArrayInputStream
                    ByteArrayOutputStream)))

(defn gzipped-response [resp]
  (let [body (resp :body)
        bout (ByteArrayOutputStream.)
        out (GZIPOutputStream. bout)
        resp (assoc-in resp [:headers "content-encoding"] "gzip")]
    (io/copy (resp :body) out)
    (.close out)
    (if (instance? InputStream body)
      (.close body))
    (assoc resp :body (ByteArrayInputStream. (.toByteArray bout)))))

(defn wrap-gzip [handler]
  (fn [req]
    (let [{body :body
           status :status
           :as resp} (handler req)]
      (if (and (= status 200)
               (not (get-in resp [:headers "content-encoding"]))
               (or
                (and (string? body) (> (count body) 200))
                (instance? InputStream body)
                (instance? File body)))
        (let [accepts (get-in req [:headers "accept-encoding"] "")
              match (re-find #"(gzip|\*)(;q=((0|1)(.\d+)?))?" accepts)]
          (if (and match (not (contains? #{"0" "0.0" "0.00" "0.000"}
                                         (match 3))))
            (gzipped-response resp)
            resp))
        resp))))