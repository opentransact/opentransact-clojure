(defproject opentransact "0.0.1-SNAPSHOT"
  :description "Library for paying with and creating OpenTransact assets."
  :dependencies [[org.clojure/clojure "1.4.0"]
                  [bux "0.2.1"]
                  [clauth "1.0.0-rc1"]
                  [crypto-random "1.1.0"]
                  [commons-codec "1.6"]
                  [ring/ring-core "1.1.0"]
                  [cheshire "4.0.0"]
                  [clj-time "0.3.7"]
                  [hiccup "1.0.0"]
                  [clj-http "0.4.0"]]
  :dev-dependencies 
                  [[clj-http-fake "0.3.0"]])