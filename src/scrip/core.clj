(ns scrip.core
  (:require [scrip.server :refer [start-server! stop-server!]]
            [scrip.worker :refer [start-worker! stop-worker!]]))

(def test-req
  {:server-port 80
   :uri "/"
   :server-name "http://google.com"
   :query-string ""
   :scheme :http
   :request-method :get})

(defn start! []
  (start-server!)
  (start-worker!))

(defn stop! []
  (stop-server!)
  (stop-worker!))

(defn -main  []
  (start!))
