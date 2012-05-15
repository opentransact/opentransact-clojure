(ns opentransact.core
  (:use [hiccup.util :only [url-encode]])
  (:require [oauthentic.core :as o]))


(defprotocol Asset
  (asset-url [this] "Return unique url for asset")
  (transfer! [this params] "Transfer funds and return map of receipt")
  (request [this params] "Request funds with given parameters and return url")
  (authorize [this params] "Authorize funds with given parameters and return url"))

(defn asset-request [this params]
  (o/assoc-query-params (asset-url this) params))