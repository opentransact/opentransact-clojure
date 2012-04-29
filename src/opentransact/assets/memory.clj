(ns opentransact.assets.memory
  (:use  [opentransact.core]
         [bux.currency]))



(deftype MemoryAsset [url data currency]
  Asset
    (asset-url [_] url)

    (request [this params] 
      (asset-request this params))

    (authorize [this params] 
      (asset-request this params))

    (transfer! [this params] ))