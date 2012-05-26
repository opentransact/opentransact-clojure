(ns opentransact.core
  (:use [hiccup.util :only [url-encode]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [oauthentic.core :as o]))


(defprotocol Asset
  (asset-url [this] "Return unique url for asset")
  (transfer! [this params] "Transfer funds and return map of receipt")
  (request [this params] "Request funds with given parameters and return url")
  (authorize [this params] "Authorize funds with given parameters and return url"))

(defprotocol Authorizable
  (authorize! [this params] "Authorize and hold funds"))

(defprotocol HistoricalAsset
  (history [this] [this a] "Return transaction history")
  (find-transaction [this tx-id] "Find a transaction by it's id")
  )

(defn asset-request [this params]
  (o/assoc-query-params (asset-url this) params))


;; Errors
(defn insufficient-funds!
  "Throw exception for insufficient funds"
  [] (throw+ {:type ::insufficient-funds }))

(defn not-allowed!
  "Throw exception for insufficient funds"
  [] (throw+ {:type ::not-allowed }))

(defn invalid-request!
  "Throw exception for insufficient funds"
  [] (throw+ {:type ::invalid-request }))
