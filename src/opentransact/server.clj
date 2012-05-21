(ns opentransact.server
  (use [clauth.middleware :only [wrap-bearer-token csrf-protect!]]
        [ring.util.response ]
        [opentransact.core]
        [cheshire.core :only [ generate-string]]
        )
  (:require
        [clauth.client :as c]
        [clauth.token :as t]
        [clauth.user :as u]
        [clauth.endpoints :as end]))


(defrecord ServerAsset [url asset]
  Asset
    (asset-url [_] url)

    (request [_ params] 
      {})

    (authorize [this params] 
      {})

    (transfer! [this params] 
      (transfer! asset params))
)



(defn asset-resource 
  "Handle the HTTP side of an OpenTransact server"
  [base-asset auth-view]
  (let [asset (ServerAsset. (asset-url base-asset) base-asset)]
    (wrap-bearer-token
      (fn [req]
        (let [params (req :params)]
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
              (if (:access-token req)
                (if (:response_type params)
                  (authorize asset params)
                  (try
                    (content-type 
                      (response 
                        (generate-string 
                          (transfer! asset (assoc params :from (:login (:subject (:access-token req))))))) "application/json")
                    (catch Exception e 
                      (if (= (.getMessage e) "Insufficient Funds")
                        (status (response "Insufficient funds") 402)
                        (throw e)))) ;; TODO don't just catch all exceptions
                  )
                (status (response "Not allowed") 401))
              
            (= :options (req :request-method))
              (response "GET,POST")
            :else 
              (status (response "Method not supported") 406))))
        )))
    