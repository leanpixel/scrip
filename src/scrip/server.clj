(ns scrip.server
  (:require [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [scrip.common :refer [config req->cache-key store! fetch!]]))

(defn- from-cache [req]
  (println "from-cache")
  (let [f (io/file (config :dir) (req->cache-key req))]
    (when (.exists f)
      (with-open [r (io/reader f)]
        (.readLine r) ; skip first line
        (read-string (.readLine r))))))

(defn- from-source [req]
  (println "from-source")
  (let [resp (fetch! req)]
    (store! req resp)
    resp))

(defn app [req]
  (let [req (merge req (config :target))]
    (or (from-cache req)
        (from-source req))))

(defonce server (atom nil))

(defn stop-server!  []
  (when-let [stop-fn @server]
    (stop-fn :timeout 100)))

(defn start-server! []
  (stop-server!)
  (reset! server (run-server #'app {:port (config :port)})))


