(ns scrip.worker
  (:require [clojure.java.io :as io]
            [scrip.common :refer [config req->url store! fetch! create-dirs!]]))

(defn update-cache []
  (println "updating cache")
  (create-dirs!)
  (let [dir (io/file ((config) :dir) "./meta/")
        files (rest (file-seq dir))]
    (doseq [f files]
      (let [frontmatter (read-string (.readLine (io/reader f)))
            expired? (> (.getTime (new java.util.Date)) (frontmatter :expires-at))]
        (when expired?
          (let [req frontmatter]
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

