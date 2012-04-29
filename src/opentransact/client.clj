(ns opentransact.client
  (:use [opentransact.core])
  (:require [clj-http.client :as client]))

(defprotocol OAuthResource
  (fetch-token [this] [this code] "Fetch token from OAuth protected service. Use code if provided, if not perform client authenticated token request"))

(defn token-get 
  "Perform HTTP GET request authenticated with token"
  [this url params]
  (:body (client/get url {  :accept :json
                            :as :json
                            :headers 
                              {"Authorization" (str "Bearer " (:token this))}
                            :form-params params })))

(defn token-post 
  "Perform HTTP POST request authenticated with token"
  [this url params]
  (:body (client/post url { :accept :json
                            :as :json
                            :headers 
                              {"Authorization" (str "Bearer " (:token this))}
                            :form-params params })))

(defn client-post 
  "Perform HTTP POST authenticated with client credentials"
  [this url params] 
  (:body (client/post url { :accept :json
                            :as :json
                            :basic-auth [(:client-id this) (:client-secret this)]}
                            :form-params params)))


(defrecord RemoteAsset [url client-id client-secret token token-url]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this params))

    (authorize [this params] 
      (asset-request this (assoc params :client_id client-id :response_type "code" )))

    (transfer! [this params] 
      (token-post this url params))

  OAuthResource
    (fetch-token [this]
      (:access_token (client-post this token-url { :grant_type "client_credentials" })))

    (fetch-token [this code]
      (:access_token (client-post this token-url { :grant_type "authorization_code" :authorization_code code }))))

(defn ot-asset 
  ([url options]
    (apply ot-asset url (map options [:client-id :client-secret :token :token-url ])))
  ([url client-id client-secret token token-url]
    (RemoteAsset. url client-id client-secret token token-url)))
