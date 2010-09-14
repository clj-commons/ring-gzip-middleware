(ns ring.middleware.gzip
  (:require [clojure.contrib.io :as io])
  (:import (java.util.zip GZIPOutputStream)
           (java.io InputStream ByteArrayInputStream ByteArrayOutputStream)))

(defn gzipped-response [resp]
  (let [body (resp :body)
        bout (ByteArrayOutputStream.)
        out (GZIPOutputStream. bout)
        headers (assoc (resp :headers) "content-encoding" "gzip")]
    (io/copy (resp :body) out)
    (.close out)
    (if (instance? InputStream body) (.close body))
    (merge resp {:body (ByteArrayInputStream. (.toByteArray bout))
                 :headers headers})))

(defn wrap-gzip [handler]
  (fn [req]
    (let [resp (handler req)
          body (resp :body)]
      (if (and (not ((get resp :headers {}) "content-encoding"))
               (or
                (and (string? body) (> (count body) 200))
                (instance? InputStream body)))
        (let [accepts (get (req :headers) "accept-encoding" "")
              match (re-find #"(gzip|\*)(;q=((0|1)(.\d+)?))?" accepts)]
          (if (and match (not (contains? #{"0" "0.0" "0.00" "0.000"}
                                         (match 3))))
            (gzipped-response resp)
            resp))
        resp))))