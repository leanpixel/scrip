(ns scrip.common
  (:require [clojure.java.io :as io]
            [pandect.algo.sha1 :refer [sha1]]
            [org.httpkit.client :as http]
            [environ.core :refer [env]]))

(def default-config
  {:port 8008
   :target-scheme :http
   :target-server "example.com"
   :dir "./cache/"
   :default-expiry 8640000})

(def config
  (merge default-config
         (-> env
             (select-keys (keys default-config))
             (update :target-scheme keyword)
             (update :default-expiry #(Long. %))
             (update :port #(Integer. %)))))

(defn- req->front-matter [req]
  (merge {:expires-at (+ (config :default-expiry)
                         (.getTime (new java.util.Date)))}
         (select-keys req [:request-method :scheme :server-name :server-port :uri :query-string :body])))

(defn req->cache-key [req]
  (sha1 (str (req :request-method)
             (req :scheme)
             (req :server-name)
             (req :server-port)
             (req :uri)
             (req :query-string))))

(defn req->url [req]
  (str (name (req :scheme)) "://" (req :server-name) (req :uri) "?" (req :query-string)))

(defn store! [req resp]
  (println "storing " (req->url req))
  (when (= 200 (resp :status))
    (let [f (io/file (config :dir) (req->cache-key req))]
      (with-open [wrtr (io/writer f)]
        (.write wrtr (str (req->front-matter req) "\n"))
        (.write wrtr (str resp))))))

(defn- stringify-headers [req]
  (assoc req :headers (reduce (fn [m [k v]] (assoc m (name k) v)) {} (req :headers))))

(defn fetch! [req]
  (println "fetching " (req->url req))
  (-> @(http/request {:url (req->url req)
                      :method (req :request-method)
                      :body (req :body)})
      (select-keys [:body :headers :status])
      (stringify-headers)
      (dissoc :headers) ; TODO should not have to remove headers
      ))
