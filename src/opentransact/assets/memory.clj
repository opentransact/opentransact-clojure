(ns opentransact.assets.memory
  (:use  [opentransact.core]
         [bux.currency]))

(deftype MemoryAsset [url currency]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this params))

    (authorize [this params] 
      (asset-request this (assoc params :client_id client_id :response_type "code" )))

    (transfer! [this params] 
      (client/post url {  :headers 
                            {"Authorization" (str "Bearer " token)}
                          :form-data params})))