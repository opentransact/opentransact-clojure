(ns opentransact.test.assets.memory
  (:use [opentransact.core]
        [opentransact.assets.memory ]
        [bux.currencies :only [$]])
  (:use [clojure.test]))

  (let  [ url "http://test.com"]

  (deftest empty-memory-asset
    (let  [ asset (create-memory-asset url)]

    (is (.url asset) "http://test.com")
    (is (.currency asset) $)
    (is (= (circulation asset) 0.00M))
    (let [ issuer (issuer-account asset)]

      (is issuer "has issuer account")
      (is (= 0.00M (balance issuer)))
      (is (= -10000.00M (:min-balance issuer)))
      (is (= 0.00M (balance issuer)))
      (is (= 0.00M (reserved issuer)))
      (is ( :id issuer) "has account id"))))

  (deftest transfer-funds

    (let  [ asset (create-memory-asset url)
            receipt (transfer! asset { :from "issuer" :to "bob" :amount 1.23M :note "Test payment" })
            issuer (issuer-account asset)
            bob    (account asset "bob")]
      (is (= (circulation asset) 1.23M))
      (is (= -1.23M (balance issuer)))
      (is (= 0.00M (reserved issuer)))

      (is (= 1.23M (balance bob)))
      (is (= 0.00M (reserved bob)))
      (is (= (find-transaction asset (:tx_id receipt)) receipt ))
      (is (= (history asset) [receipt] ))
      (is (= (history asset "bob") [receipt] ))
      (is (= (history asset "issuer") [receipt] ))
      (is (= (history asset "alice") [] ))

      (try
        (transfer! asset { :from "alice" :to "bob" :amount 1.23M :note "Test payment" })
        (is false "could transfer funds below balance")
        (catch Exception e
          (is true "could not transfer funds")
          )
        )
      (is (transfer! asset { :from "bob" :to "alice" :amount 1.23M :note "Test payment" }))
    ))


  )