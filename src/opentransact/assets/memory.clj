(ns opentransact.assets.memory
  (:use  [opentransact.core]
         [bux.currency]
         [bux.currencies :only [$]]))


(defn account 
  "returns the account for given id"
  [asset id]
  ((deref (:accounts asset)) id { :id id }))

(defn issuer-account 
  "returns the issuers account for current asset"
  [asset]
  (account asset (:issuer asset)))

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

(defrecord MemoryAsset [url accounts issuer currency]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this params))

    (authorize [this params] 
      (asset-request this params))

    (transfer! [this params] 
      (swap! accounts (fn [accs]
        (let [  _ (prn accs)
                _ (prn this)
                from_id (params :from)
                to_id (params :to)
                amount (currency (params :amount))
                from (account this from_id) 
                to (account this to_id) 
                _ (prn "transfer!!!")]
                (let [
                  naccs (assoc accs 
                      from_id (assoc from :balance (- (balance from ) amount))
                      to_id (assoc to :balance (+ (balance to ) amount)))
                    _ (prn naccs)]
                    naccs)
        )))))

(defn create-memory-asset 
  "Create a new in memory asset"

  ([url]
    (create-memory-asset url {} "issuer" $)
    )
  ([url accounts issuer currency]
    (MemoryAsset. url (atom accounts) issuer currency)))

