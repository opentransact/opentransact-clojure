(ns opentransact.client
  (:use [opentransact.core])
  (:require [clj-http.client :as client]))

(defprotocol OAuthResource
  (fetch-token [this] [this code] "Fetch token from OAuth protected service. Use code if provided, if not perform client authenticated token request"))

(defrecord RemoteAsset [url client_id secret token token_url]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this params))

    (authorize [this params] 
      (asset-request this (assoc params :client_id client_id :response_type "code" )))

    (transfer! [this params] 
      (:body (client/post url {  :accept :json
                          :as :json
                          :headers 
                            {"Authorization" (str "Bearer " token)}
                          :form-params params})))
  OAuthResource
    (fetch-token [this]
      (:access_token (client/post token_url 
          {:basic-auth [client_id secret]}
          :form-data { :grant_type "client_credentials" }
      )))

    (fetch-token [this code]
      (:access_token (client/post token_url 
          {:basic-auth [client_id secret]}
          :form-data { :grant_type "authorization_code" :authorization_code code}
      ))))

(defn ot-asset 
  ([url options]
    (apply ot-asset url (map options [:client_id :secret :token :token_url ])))
  ([url client_id secret token token_url]
    (RemoteAsset. url client_id secret token token_url)))