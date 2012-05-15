(ns opentransact.assets.memory
  (:use  [opentransact.core]
         [bux.currency]
         [bux.currencies :only [$]]))


(defn account 
  "returns the account for given id"
  [asset id]
  ((deref (.accounts asset)) id { :id id }))

(defn issuer-account 
  "returns the issuers account for current asset"
  [asset]
  (account asset (.issuer asset)))

(defn balance
  "balance of given account"
  ([a] ($ (a :balance 0)))
  ([asset id] (balance (account asset id))))

(defn reserved
  "balance of given account"
  ([a] ($ (a :reserved 0)))
  ([asset id] (reserved (account asset id))))

(defn circulation
  "circulation of asset"
  [asset]
  (- (balance (issuer-account asset))))

(deftype MemoryAsset 
  [url accounts transactions issuer currency]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this params))

    (authorize [this params] 
      (asset-request this params))

    (transfer! [this { from-id :from
                       to-id :to
                       note :note
                       raw-amount :amount }]
      (dosync
        (let [  
            amount (currency raw-amount)
            from (account this from-id) 
            to (account this to-id) 
            tx-id (str (java.util.UUID/randomUUID))
            receipt {:tx_id tx-id :from from-id :to to-id :amount amount :note note }]
          (do
            (alter accounts (fn [accs]
                (let [
                      naccs (assoc accs 
                        from-id (assoc from :balance (- (balance from ) amount))
                        to-id (assoc to :balance (+ (balance to ) amount)))]
                    naccs )))
            (alter transactions conj receipt)
            receipt ))))
    ; )

  HistoricalAsset  
    (find-transaction [_ tx-id]
      (first (filter #(= tx-id (:tx_id %)) @transactions)))

    (history
      [this] @transactions)
    (history [this a] 
      (filter #( or 
                  (= a (:from %)) 
                  (= a (:to %))) 
        @transactions ))
  )

(defn create-memory-asset 
  "Create a new in memory asset"

  ([url]
    (create-memory-asset url {} [] "issuer" $ ))
  ([url accounts transactions issuer currency]
    (MemoryAsset. url (ref accounts) (ref transactions) issuer currency)))

