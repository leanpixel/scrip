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

(defn- create-dirs! []
  (.mkdir (io/file (config :dir)))
  (.mkdir (io/file (config :dir) "./meta/"))
  (.mkdir (io/file (config :dir) "./body/")))

(create-dirs!)

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
    (let [f1 (io/file (config :dir) "./meta/" (req->cache-key req))
          f2 (io/file (config :dir) "./body/" (req->cache-key req))]
      (with-open [wrtr (io/writer f1)]
        (.write wrtr (str (req->front-matter req) "\n"))
        (.write wrtr (str (dissoc resp :body) "\n")))
      (with-open [wrtr (io/output-stream f2)]
        (.write wrtr (if (string? (resp :body))
                       (.getBytes (resp :body))
                       (.bytes (resp :body))))))))

(defn read! [req]
  (let [f1 (io/file (config :dir) "./meta/" (req->cache-key req))
        f2 (io/file (config :dir) "./body/" (req->cache-key req))]
    (when (.exists f1)
      (let [m (with-open [r (io/reader f1)]
                (read-string (nth (line-seq r) 1)))]
        (merge m {:body (io/input-stream f2)})))))

(defn- stringify-headers [req]
  (assoc req :headers (reduce (fn [m [k v]] (assoc m (name k) v)) {} (req :headers))))

(defn fetch! [req]
  (println "fetching " (req->url req))
  (-> @(http/request {:url (req->url req)
                      :method (req :request-method)
                      :body (req :body)})
      (select-keys [:body :headers :status])
      (update-in [:headers] select-keys [:content-type])
      (stringify-headers)))
