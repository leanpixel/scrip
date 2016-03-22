(ns scrip.worker
  (:require [clojure.java.io :as io]
            [scrip.common :refer [config req->url store! fetch! create-dirs!]]
            [ring.util.time :as ring-time]))

(defn update-cache []
  (println "updating cache")
  (create-dirs!)
  (let [dir (io/file ((config) :dir) "./meta/")
        files (rest (file-seq dir))]
    (doseq [f files]
      (with-open [r (io/reader f)]
        (let [req (read-string (.readLine r))
              resp-meta (read-string (.readLine r))
              expires (ring-time/parse-date (or (get-in resp-meta [:headers "expires"]) ""))
              expired?  (if expires
                          (> (.getTime (new java.util.Date)) (.getTime expires))
                          true)]
          (when expired?
            (println "expired: " (req->url req))
            (store! req (fetch! req))))))))

(defonce worker (atom nil))

(defn start-worker! []
  (loop []
    (let [f (future (update-cache)
                    (Thread/sleep 60000)
                    (recur))]
     (reset! worker (fn [] (future-cancel f))))))

(defn stop-worker! []
  (when-let [stop-fn @worker]
    (stop-fn)))

