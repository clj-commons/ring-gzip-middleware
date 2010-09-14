(ns ring.middleware.gzip
  (:import (java.util.zip GZIPOutputStream)
           (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defn gzipped-response [resp]
  (let [bout (ByteArrayOutputStream.)
        out (GZIPOutputStream. bout)
        headers (assoc (resp :headers) "content-encoding" "gzip")]
    (.write out (.getBytes (resp :body)))
    (.close out)
    (merge resp {:body (ByteArrayInputStream. (.toByteArray bout))
                 :headers headers})))

(defn wrap-gzip [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (and (not ((resp :headers) "content-encoding"))
               (string? (resp :body)))
        (let [accepts (get (req :headers) "accept-encoding" "")
              match (re-find #"(gzip|\*)(;q=((0|1)(.\d+)?))?" accepts)]
          (if (and match (not (contains? #{"0" "0.0" "0.00" "0.000"}
                                         (match 3))))
            (gzipped-response resp)
            resp))
        resp))))