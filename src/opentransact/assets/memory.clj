(ns opentransact.assets.memory
  (:use  [opentransact.core]
         [bux.currency]
         [bux.currencies :only [$]]))


(defn account 
  "returns the account for given id"
  [asset id]
  (let [$ (.currency asset)]
    ((deref (.accounts asset)) id { :id id :min-balance ($ 0M) :balance ($ 0M)})))

(defn issuer-account 
  "returns the issuers account for current asset"
  [asset]
  (let [$ (.currency asset)]
    ((deref (.accounts asset)) (.issuer asset) { :id (.issuer asset) :min-balance ($ -10000M) :balance ($ 0M)})))

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
          (if (>= (- (balance from) amount) (:min-balance from))
            (do

              (alter accounts (fn [accs]
                  (let [
                        naccs (assoc accs 
                          from-id (assoc from :balance (- (balance from ) amount))
                          to-id (assoc to :balance (+ (balance to ) amount)))]
                      naccs )))
              (alter transactions conj receipt)
              receipt )
            (throw (Exception. "Insufficient Funds"))
            ))))
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
    (MemoryAsset. url (ref (assoc accounts issuer 
                              { :id issuer :min-balance (currency -10000M) 
                                :balance (currency 0M)})) (ref transactions) issuer currency)))

