# opentransact

Clojure library for interacting with and creating [OpenTransact](http://opentransact.org) assets.

This is under heavy development right now and does therefore not have a stable API yet.

## Install

Add the following dependency to your `project.clj` file:

```clojure
[opentransact "0.0.1"]
```

## Usage

```clojure
(use 'opentransact.core)
```

The Asset protocol contains the following 4 functions:

- asset-url return the [OpenTransact Transaction URL for asset](http://www.opentransact.org/core.html#transaction-url)
- request creates a URL containing an [OpenTransact Transfer Request](http://www.opentransact.org/core.html#transfer-request)
- authorize creates a URL containing an [OpenTransact Transfer Authorization Request](http://www.opentransact.org/core.html#transfer-authorization)
- transfer! performs an actual [OpenTransact Transfer](http://www.opentransact.org/core.html#transfer)

We have a RemoteAsset client which performs this over http.

However internal assets can be created adhearing to the same Asset protocol that can be wrapped in an OpenTransact Handler for Ring allowing you to create your own custom OpenTransact services.

Transfer directly:

```clojure
(transfer asset {:to "AAAA" :amount 20M :note "For tomatos"})
=> {:xid "http://asset.com/transactions/123123123" :from "BBBB" :to "AAAA" :amount 20M :note "For tomatos"}
```

Create a Transfer Request for a html link or redirection:

```clojure
(request asset {:to "AAAA" :amount 20M :note "For tomatos"})
=> "http://asset.com/usd?to=AAAA&amount=20&note=For%20tomatos"
```

Create a Transfer Authorization Request for a html link or redirection:

```clojure
(authorize asset {:to "AAAA" :amount 20M :note "For tomatos"})
=> "http://asset.com/usd?to=AAAA&amount=20&note=For%20tomatos&client_id=abcdefg"
```

Create an OpenTransact RemoteAsset asset type:
  
```clojure
(use 'opentransact.client)
(def asset (ot-asset "http://asset.com/usd" { :client-id "abcdefg" :client-secret "ssh" :token "my-oauth-token" :token-url "http://asset.com/token"}))
```

### Implementing your own OpenTransact asset.

You create an asset handler by passing in an asset and a view handler to be displayed to the user for transfer-request and transfer-authorizations:

```clojure
(opentransact.server/asset-handler asset auth-view)
```

This requires a backend that will implement the above transfer and authorize multimethods locally.

## License

Copyright (C) 2012 Pelle Braendgaard and PicoMoney Company

Distributed under the Eclipse Public License, the same as Clojure.
