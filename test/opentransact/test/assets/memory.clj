(ns opentransact.test.assets.memory
  (:use [opentransact.core]
        [opentransact.assets.memory ]
        [bux.currencies :only [$]]
        [slingshot.slingshot :only [throw+ try+]])
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
      (is (= 10000.00M (available-balance issuer)))
      (is (= 0.00M (reserved issuer)))
      (is ( :id issuer) "has account id"))))

  (deftest transfer-funds

    (let  [ asset (create-memory-asset url)
            receipt (transfer! asset { :from "issuer" :to "bob" :amount 1.23M :note "Test payment" })
            issuer (issuer-account asset)
            bob    (account asset "bob")]
      (is (= (circulation asset) 1.23M))
      (is (= -1.23M (balance issuer)))
      (is (= 9998.77M (available-balance issuer)))
      (is (= 0.00M (reserved issuer)))

      (is (= 1.23M (balance bob)))
      (is (= 0.00M (reserved bob)))
      (is (= (find-transaction asset (:tx_id receipt)) receipt ))
      (is (= (history asset) [receipt] ))
      (is (= (history asset "bob") [receipt] ))
      (is (= (history asset "issuer") [receipt] ))
      (is (= (history asset "alice") [] ))

      (try+
        (transfer! asset { :from "alice" :to "bob" :amount 1.23M :note "Test payment" })
        (is false "could transfer funds below balance")
        (catch [:type :opentransact.core/insufficient-funds] _
          (is true "could not transfer funds")
          )
        )
      (is (transfer! asset { :from "bob" :to "alice" :amount 1.23M :note "Test payment" }))
    ))

  (deftest authorize-funds

    (let  [ asset (create-memory-asset url)
            reserve (authorize! asset { :from "issuer" :to "bob" :amount 1.23M :note "Test payment" })
            issuer (issuer-account asset)
            bob    (account asset "bob")]
      (is (= (circulation asset) 0.00M))
      (is (= 0.00M (balance issuer)))
      (is (= 1.23M (reserved issuer)))
      (is (= 9998.77M (available-balance issuer)))

      (is (= 0.00M (balance bob)))
      (is (= 0.00M (available-balance bob)))
      (is (= 0.00M (reserved bob)))
      (is (= (find-transaction asset (:tx_id reserve)) reserve ))
      (is (= (history asset) [reserve] ))
      (is (= (:from reserve) "issuer"))
      (is (= (:to reserve) "bob"))
      (is (= (history asset "bob") [reserve] ))
      (is (= (history asset "issuer") [reserve] ))
      (is (= (history asset "alice") [] ))

      (try+
        (transfer! asset { :from "bob" :to "alice" :amount 1.23M :note "Test payment" })
        (is false "could transfer funds below balance")
        (catch [:type :opentransact.core/insufficient-funds] _
          (is true "could not transfer funds")
          )
        )

      (let [receipt (transfer! reserve {})
            issuer (issuer-account asset)
            bob    (account asset "bob")]

        (is (= (circulation asset) 1.23M))
        (is (= -1.23M (balance issuer)))
        (is (= 0.00M (reserved issuer)))
        (is (= 9998.77M (available-balance issuer)))

        (is (= 1.23M (balance bob)))
        (is (= 0.00M (reserved bob)))
        (is (= (:note receipt) "Test payment"))
        (is (= (:from receipt) "issuer"))
        (is (= (:to receipt) "bob"))
        (is (= (find-transaction asset (:tx_id receipt)) receipt ))
        (is (= (history asset) [reserve receipt] ))
        (is (= (history asset "bob") [reserve receipt] ))
        (is (= (history asset "issuer") [reserve receipt] ))
        (is (= (history asset "alice") [] ))
        (is (transfer! asset { :from "bob" :to "alice" :amount 1.23M :note "Test payment" }))))

    (let  [ asset (create-memory-asset url)
            reserve (authorize! asset { :from "issuer" :to "bob" :amount 1.23M :note "Test payment" })
            receipt (transfer! reserve { :to "alice" :amount 1.10M :note "Revised"}) ;; Change default parameters
            issuer (issuer-account asset)
            bob    (account asset "bob")
            alice  (account asset "alice")]

        (is (= (circulation asset) 1.10M))
        (is (= -1.10M (balance issuer)))
        (is (= 9998.77M (available-balance issuer)))
        (is (= 0.13M (reserved issuer)))

        (is (= 0.00M (balance bob)))
        (is (= 0.00M (reserved bob)))

        (is (= 1.10M (balance alice)))
        (is (= 0.00M (reserved alice)))

        (is (= (:note receipt) "Revised"))
        (is (= (:from receipt) "issuer"))
        (is (= (:to receipt) "alice"))
        (is (= (find-transaction asset (:tx_id receipt)) receipt ))
        (is (= (history asset) [reserve receipt] ))
        (is (= (history asset "alice") [ receipt] ))
        (is (= (history asset "issuer") [reserve receipt] ))
        (is (= (history asset "bob") [reserve] ))
        (is (transfer! asset { :from "alice" :to "bob" :amount 1.10M :note "Test payment" }))))

  )