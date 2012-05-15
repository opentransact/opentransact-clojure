(ns opentransact.client
  (:use [opentransact.core])
  (:require [clj-http.client :as client]
            [oauthentic.core :as o]))

(defrecord RemoteAsset [url client-id client-secret token token-url]
  Asset
    (asset-url [_] url)

    (request [_ params] 
      (o/assoc-query-params url params))

    (authorize [this params] 
      (o/build-authorization-url (assoc this :authorization-url url) params))

    (transfer! [this params] 
      (:body (client/post url  { :oauth-token token
                          :accept :json
                          :as :json
                          :form-params params })))
  HistoricalAsset
    (find-transaction [this tx-id]
        (:body (client/get (str url "/" tx-id) { :oauth-token token
                                                  :accept :json
                                                  :as :json })))

    (history
      [this] [])
    (history [this account]
        []
    )

    )


(defn ot-asset 
  ([url options]
    (apply ot-asset url (map options [:client-id :client-secret :token :token-url ])))
  ([url client-id client-secret token token-url]
    (RemoteAsset. url client-id client-secret token token-url)))
