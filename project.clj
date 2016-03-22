(defproject scrip "0.0.1"
  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.19"]
                 [ring/ring-core "1.4.0"]
                 [pandect "0.5.4"]
                 [environ "1.0.2"]
                 [clj-time "0.11.0"]]

  :plugins [[lein-environ "1.0.2"]]

  :main scrip.core

  :profiles {:uberjar {:aot :all}}
)
