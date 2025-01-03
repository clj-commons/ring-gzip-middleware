(ns ring.middleware.gzip
  (:require [clojure.java.io :as io]
            clojure.reflect)
  (:import (java.util.zip GZIPOutputStream)
           (java.io InputStream
                    OutputStream
                    Closeable
                    File
                    PipedInputStream
                    PipedOutputStream)))

; only available on JDK7
(def ^:private flushable-gzip?
  (delay (->> (clojure.reflect/reflect GZIPOutputStream)
           :members
           (some (comp '#{[java.io.OutputStream boolean]} :parameter-types)))))

; only proxying here so we can specialize io/copy (which ring uses to transfer
; InputStream bodies to the servlet response) for reading from the result of
; piped-gzipped-input-stream
(defn- piped-gzipped-input-stream*
  []
  (proxy [PipedInputStream] []))

; exactly the same as do-copy for [InputStream OutputStream], but
; flushes the output on every chunk; this allows gzipped content to start
; flowing to clients ASAP (a reasonable change to ring IMO)
(defmethod @#'io/do-copy [(class (piped-gzipped-input-stream*)) OutputStream]
  [^InputStream input ^OutputStream output opts]
  (let [buffer (make-array Byte/TYPE (or (:buffer-size opts) 1024))]
    (loop []
      (let [size (.read input buffer)]
        (when (pos? size)
          (.write output buffer 0 size)
          (.flush output)
          (recur))))))

(defn piped-gzipped-input-stream [in]
  (let [pipe-in (piped-gzipped-input-stream*)
        pipe-out (PipedOutputStream. pipe-in)]
    ; separate thread to prevent blocking deadlock
    (future
      (with-open [out (if @flushable-gzip?
                        (GZIPOutputStream. pipe-out true)
                        (GZIPOutputStream. pipe-out))]
        (if (seq? in)
          (doseq [string in]
            (io/copy (str string) out)
            (.flush out))
          (io/copy in out)))
      (when (instance? Closeable in)
        (.close ^Closeable in)))
    pipe-in))

(defn set-response-headers
  [headers]
  (-> headers
      (assoc "Content-Encoding" "gzip")
      (dissoc "Content-Length")))

(defn gzipped-response [resp]
  (-> resp
      (update :headers set-response-headers)
      (update :body piped-gzipped-input-stream)))

(defn accepts-gzip?
  "Tests if the request indicates that the client can accept a gzipped response"
  [{:keys [headers]}]
  (let [accepts (or (get headers "accept-encoding")
                    (get headers "Accept-Encoding")
                    "")
        match (re-find #"(gzip|\*)(;q=((0|1)(.\d+)?))?" accepts)]
    (and match (not (contains? #{"0" "0.0" "0.00" "0.000"} (match 3))))))

(def ^:private default-status 200)

(def supported-status? #{200 201 202 203 204 205})

(def min-length 200)

(defn content-encoding?
  "Tests if the provided response has a content-encoding header"
  [{:keys [headers]}]
  (or (get headers "Content-Encoding")
      (get headers "content-encoding")))

(defn supported-response?
  [{:keys [body status] :as resp}]
  (and (supported-status? (or status default-status))
       (not (content-encoding? resp))
       (or
        (and (string? body) (> (count body) min-length))
        (instance? InputStream body)
        (instance? File body)
        (and (seq? body) @flushable-gzip?))))

(defn gzip-response [req resp]
  (if (and (supported-response? resp)
           (accepts-gzip? req))
    (gzipped-response resp)
    resp))

(defn wrap-gzip
  "Ring middleware that GZIPs response if client can handle it."
  [handler]
  (fn
    ([request]
     (gzip-response request (handler request)))
    ([request respond raise]
     (handler
      request
      (fn [response]
        (respond (gzip-response request response)))
      raise))))
