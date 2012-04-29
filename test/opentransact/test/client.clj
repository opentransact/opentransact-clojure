(ns opentransact.test.client
  (:use [opentransact.client]
        [opentransact.core]
        [clj-http.fake]
        [clojure.test]))


(let 
  [ asset (ot-asset "https://picomoney.com/dev_credits" 
                    { :client-id "CLIENT" 
                      :client-secret "SECRET" 
                      :token "TOKEN"
                      :token-url "https://picomoney.com/oauth/token"})]

  (deftest client-has-asset-properties
    (is (= (:client-id asset) "CLIENT"))
    (is (= (:client-secret asset) "SECRET"))
    (is (= (:token asset) "TOKEN"))
    (is (= (:token-url asset) "https://picomoney.com/oauth/token"))
    (is (= (:url asset) "https://picomoney.com/dev_credits" ))
    (is (= (asset-url asset) "https://picomoney.com/dev_credits" )))

  (deftest opentransact-transfer-request
    (is (= (request asset { :to "bob@sample.com" 
                            :from "alice@sample.com"
                            :amount 123
                            :note "Thanks for this"})
           "https://picomoney.com/dev_credits?note=Thanks+for+this&from=alice%40sample.com&to=bob%40sample.com&amount=123"))
    )

  (deftest opentransact-authorization-request
    (is (= (authorize asset { :to "bob@sample.com" 
                            :from "alice@sample.com"
                            :amount 123
                            :note "Thanks for this"})
           "https://picomoney.com/dev_credits?response_type=code&client_id=CLIENT&note=Thanks+for+this&from=alice%40sample.com&to=bob%40sample.com&amount=123"))
    )

  (deftest opentransact-authorization-request
    (with-fake-routes
      {"https://picomoney.com/dev_credits" (fn [req] {:status 200 :headers {} :body "{\"tx_id\":\"ABCDEF\"}"}) }

        (is (= (transfer! asset { :to "bob@sample.com" 
                                :from "alice@sample.com"
                                :amount 123
                                :note "Thanks for this"})
            {:tx_id "ABCDEF"}))
    ))

  )
