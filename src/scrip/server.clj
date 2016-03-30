(ns scrip.server
  (:require [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [scrip.common :refer [config store! fetch! read!]]))

(defn- from-cache [req]
  (println "from-cache")
  (read! req))

(defn- from-source [req]
  (println "from-source")
  (let [resp (fetch! req)]
    (store! req resp)
    resp))

(defn should-purge? [req]
  (or (= "no-cache" (get-in req [:headers "pragma"]))
      (= "no-cache" (get-in req [:headers "cache-control"]))))

(defn app [req]
  (let [req (merge req {:scheme ((config) :target-scheme)
                        :server-name ((config) :target-server)})]
    (if (should-purge? req)
      (from-source req)
      (or (from-cache req)
          (from-source req)))))

(defonce server (atom nil))

(defn stop-server!  []
  (when-let [stop-fn @server]
    (stop-fn :timeout 100)))

(defn start-server! []
  (stop-server!)
  (reset! server (run-server #'app {:port ((config) :port)})))


