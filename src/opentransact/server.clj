(ns opentransact.server
  (use [clauth.middleware :only [wrap-bearer-token csrf-protect! if-html]]
        [ring.util.response ]
        [opentransact.core]
        [cheshire.core :only [ generate-string]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require
        [clauth.client :as c]
        [clauth.token :as t]
        [clauth.auth-code :as ac]
        [clauth.user :as u]
        [clauth.endpoints :as end]))


(defrecord ServerAsset [url asset]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this (merge {:amount 1} params)))

    (authorize [this params] 
      (asset-request this (merge {:amount 1} params)))

    (transfer! [this params] 
      (transfer! asset (merge {:amount 1} params)))

  Authorizable
    (authorize! [this params]
      (if (extends? Authorizable (class asset)) ;; don't understand why isa? doesn't work
          (authorize! asset (merge {:amount 1} params))
          params
        )))




(defn asset-resource 
  "Handle the HTTP side of an OpenTransact server"
  [base-asset auth-view]
  (let [asset (ServerAsset. (asset-url base-asset) base-asset)]
    (wrap-bearer-token
      (fn [req]
        (let [params (req :params)
              token  (req :access-token)]
          (cond 
            (= :get (req :request-method))
              (if (:response_type params)
                (if (= "code" (:response_type params))
                  (if-let [client (c/fetch-client (:client_id params))]        
                    (auth-view req)
                    (end/authorization-error-response req "invalid_request")) 
                  (end/authorization-error-response req "unsupported_response_type"))
                (auth-view req))

            (= :post (req :request-method))
              (if token
                (try+
                  (if (:response_type params)
                    (if (= "code" (:response_type params))

                      (if-let [ client (c/fetch-client (:client_id params))]        

                        (let [  user ( :subject (t/fetch-token (:access_token (req :session)))) ;; Make sure token comes from session
                                receipt (authorize! asset (assoc params :from (:login user)))
                                code (ac/create-auth-code client user (:redirect_uri params) (asset-url asset) receipt)]
                                
                          (end/authorization-response req {:code (:code code) :tx_id (:tx_id receipt)}))
                          
                        (end/authorization-error-response req "invalid_request"))

                      (end/authorization-error-response req "unsupported_response_type"))

                    ;; Perform transfer
                    (let [  user ( :subject token) 
                            token-object (:object token)
                            reserve (if (and token-object (extends? Asset (class token-object))) token-object)
                            asset (or reserve asset)
                            receipt (transfer! asset (assoc params :from (:login user)))]
                      (if-html req
                        (end/authorization-response req { :tx_id (:tx_id receipt) })
                        (content-type 
                          (response 
                            (generate-string receipt)) "application/json"))))
                (catch [:type :opentransact.core/insufficient-funds] _
                  (status (response "Insufficient funds") 402)))
              (status (response "Not allowed") 401 ))

              
            (= :options (req :request-method))
              (response "GET,POST")
            :else 
              (status (response "Method not supported") 406))))
        )))
    