(ns opentransact.test.server
  (:use [opentransact.core]
        [opentransact.server]
        [opentransact.assets.memory ]
        [bux.currencies :only [$]]
        [clojure.test]
        [cheshire.core :only [ parse-string]])
  (:require
        [clauth.client :as c]
        [clauth.token :as t]
        [clauth.user :as u]
        [clauth.auth-code :as ac]))


(defn dummy-authview [req]
  { :status 200 :body "AUTH VIEW"})

(let  [ url "http://test.com" 
        asset (create-memory-asset url)
        handler (asset-resource asset dummy-authview)]

        (t/reset-token-store!)
        (c/reset-client-store!)
        (u/reset-user-store!)


        (deftest transfer-request
          (let [request { :request-method :get 
                          :params { :to "bob" :amount "1.23" :note "Test payment" :for "http://test.com/product" :redirect_uri "http://mysite.com/redirect"}
                          :headers { "accept" "text/html" }}]

            (let [response (handler request)]
              (is (= (:status response) 200 ) "Should not require authentication")
              (is (= (:body response) "AUTH VIEW" ) "Should not require authentication"))

          ))

        (deftest transfer-authorization-request
          (let [  client (c/register-client)
                  request { :request-method :get 
                          :params { :response_type "code" :client_id (:client-id client) :to "bob" :amount "1.23" :note "Test payment" :for "http://test.com/product" :redirect_uri "http://mysite.com/redirect"}
                          :headers { "accept" "text/html" }}]

            (let [response (handler request)]
              (is (= (:status response) 200 ) "Should not require authentication")
              (is (= (:body response) "AUTH VIEW" ) "Should not require authentication"))


            (let [response (handler (assoc-in request [ :params :response_type ] "abcde"))]
              (is (= (response :status) 302))
              (is (= (response :headers) { "Location" "http://mysite.com/redirect?error=unsupported_response_type" }) 
                  "should return error on unsupported response type"))

            (let [response (handler (assoc-in request [ :params :client_id ] "baddy"))]
              (is (= (response :status) 302))
              (is (= (response :headers) { "Location" "http://mysite.com/redirect?error=invalid_request" }) 
                  "should return error on with wrong client_id"))

          ))


        (deftest transfer-funds
          (let [ client (c/register-client)
                 user (u/register-user "john@example.com" "password")
                 token (t/create-token client user)
                 request {:request-method :post 
                          :params { :to "bob" :amount "1.23" :note "Test payment" }
                          :headers { "accept" "application/json" }}]


            (let [response (handler request)]
              (is (= 401 (:status response)) "should require authentication")
              )

            (let [response (handler (assoc-in request [:headers "authorization"] (str "Bearer " (:token token))))]
              (is (= 402 (:status response)) "should fail due to lack of funds")
              )

            

            (let [_ (transfer! asset { :from "issuer" :to "john@example.com" :amount 1.23M :note "Test payment" })
                  response (handler (assoc-in request [:headers "authorization"] (str "Bearer " (:token token))))
                  receipt (parse-string (:body response) true)]

              (is (= 200 (:status response)) "should be a success")
              (is (= 0.00M  (balance (account asset "john@example.com"))))
              (is (= 1.23M (balance (account asset "bob"))))
              (is (= (:from receipt) "john@example.com"))
              (is (= (:to receipt) "bob")))
          )))

(let  [ url "http://test.com" 
        asset (create-memory-asset url)
        handler (asset-resource asset dummy-authview)]

        (t/reset-token-store!)
        (c/reset-client-store!)
        (u/reset-user-store!)

        (deftest transfer-funds-after-request
          (let [ client (c/register-client)
                 user (u/register-user "john@example.com" "password")
                 token (t/create-token client user)
                 request {:request-method :post 
                          :params { :to "alice" :amount "1.23" :note "Test payment" :redirect_uri "http://myshop.com/cart"}
                          :session { :access_token (:token token)}
                          :headers { "accept" "text/html" }}]



            (let [response (handler (dissoc request :session))]
              (is (= 401 (:status response)) "should require authentication"))

            (let [response (handler request)]
              (is (= 402 (:status response)) "should fail due to lack of funds"))            

            (let [_ (transfer! asset { :from "issuer" :to "john@example.com" :amount 1.23M :note "Test payment" })
                  response (handler request)
                  receipt (last (history asset))]

              (is (= 302 (:status response)) "should be a redirect")
              (is (= 0.00M  (balance (account asset "john@example.com"))))
              (is (= 1.23M (balance (account asset "alice"))))
              (is (= (str "http://myshop.com/cart?tx_id=" (:tx_id receipt)) ((:headers response) "Location" )))
              )
          )))

(let  [ url "http://test.com" 
        asset (create-memory-asset url)
        handler (asset-resource asset dummy-authview)]

        (t/reset-token-store!)
        (c/reset-client-store!)
        (u/reset-user-store!)

        (deftest authorize-funds-after-request
          (let [ client (c/register-client)
                 user (u/register-user "john@example.com" "password")
                 token (t/create-token (c/register-client) user)
                 request {:request-method :post 
                          :params { :response_type "code" :client_id (:client-id client) :to "carol" :amount "1.23" :note "Test payment" :redirect_uri "http://myshop.com/cart"}
                          :session { :access_token (:token token)}
                          :headers { "accept" "text/html" }}]



            (let [response (handler (dissoc request :session))]
              (is (= 401 (:status response)) "should require authentication"))

            (let [response (handler request)]
              (is (= 402 (:status response)) "should fail due to lack of funds"))            

            (let [_ (transfer! asset { :from "issuer" :to "john@example.com" :amount 1.23M :note "Test payment" })
                  response (handler request)
                  receipt (last (history asset)) 
                  code  (last (ac/auth-codes))]

              (is (= 302 (:status response)) "should be a redirect")
              (is (= 1.23M  (balance (account asset "john@example.com"))))
              (is (= 0.00M  (available-balance (account asset "john@example.com"))))
              (is (= 0.00M (balance (account asset "carol"))))
              (is (= (str "http://myshop.com/cart?code=" (:code code) "&tx_id=" (:tx_id receipt)) ((:headers response) "Location" )))
              )
          )))


