(ns opentransact.core
  (:use [hiccup.util :only [url-encode]]))


(defprotocol Asset
  (asset-url [this] "Return unique url for asset")
  (transfer! [this params] "Transfer funds and return map of receipt")
  (request [this params] "Request funds with given parameters and return url")
  (authorize [this params] "Authorize funds with given parameters and return url"))


(defn asset-request [asset params]
  (str (asset-url asset) "?" (url-encode params)))