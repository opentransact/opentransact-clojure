# opentransact

Clojure library for interacting with and creating [OpenTransact](http://opentransact.org) assets.

## Usage

  (use 'opentransact.core)

The Asset protocol contains the following 4 functions:

- asset-url return the [OpenTransact Transaction URL for asset](http://www.opentransact.org/core.html#transaction-url)
- request creates a URL containing an [OpenTransact Transfer Request](http://www.opentransact.org/core.html#transfer-request)
- authorize creates a URL containing an [OpenTransact Transfer Authorization Request](http://www.opentransact.org/core.html#transfer-authorization)
- transfer! performs an actual [OpenTransact Transfer](http://www.opentransact.org/core.html#transfer)

We will implement a RemoteAsset client which performs this over http.

However internal assets can be created adhearing to the same Asset protocol that can be wrapped in an OpenTransact Handler for Ring allowing you to create your own custom OpenTransact services.

Transfer directly:

  (transfer asset {:to "AAAA" :amount 20M :note "For tomatos"})
  => {:xid "http://asset.com/transactions/123123123" :from "BBBB" :to "AAAA" :amount 20M :note "For tomatos"}

Create a Transfer Request for a html link or redirection:

  (request asset {:to "AAAA" :amount 20M :note "For tomatos"})
  => "http://asset.com/usd?to=AAAA&amount=20&note=For%20tomatos"

  (authorize asset {:to "AAAA" :amount 20M :note "For tomatos"})
  => "http://asset.com/usd?to=AAAA&amount=20&note=For%20tomatos&client_id=abcdefg"

Create an OpenTransact RemoteAsset asset type:
  
  (use 'opentransact.client)
  (def asset (ot-asset "http://asset.com/usd" { :client_id "abcdefg" :client_secret "ssh" :token "my-oauth-token" }))

### Implementing your own OpenTransact asset.

The plan is to have a handler:

  opentransact.server/ot-handler

This will require a backend that will implement the above transfer and authorize multimethods locally.

## License

Copyright (C) 2012 Pelle Braendgaard and PicoMoney Company

Distributed under the Eclipse Public License, the same as Clojure.
