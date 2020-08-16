# Azure

Add an environment variable:

```
az webapp config appsettings set --resource-group ShutTheBox --name shutthebox --settings TWILIO_ACCOUNT_SID="..."
```

Manually deploy using the [run directly from package](https://docs.microsoft.com/en-us/azure/app-service/deploy-run-package) flow:

```
clojure -A:release
npx gulp

cd target\dist
npm install --production --ignore-scripts
7z a -r ..\dist.zip .
cd ..\..

az webapp deployment source config-zip --resource-group ShutTheBox --name shutthebox --src target\dist.zip
```

Tail logs:

```
az webapp log tail --resource-group ShutTheBox --name shutthebox
```

Using [this approach](https://docs.microsoft.com/en-us/azure/app-service/deploy-github-actions) for deploying from GitHub.


# Convert Let's Encrypt Keys to JKS

From [here](https://maximilian-boehm.com/en-gb/blog/create-a-java-keystore-jks-from-lets-encrypt-certificates-1884000/):

```
openssl pkcs12 -export -in dev-fullchain.pem -inkey dev-privkey.pem -out dev.pkcs12
keytool -importkeystore -deststorepass password -destkeypass password -destkeystore keystore.jks -srckeystore dev.pkcs12 -srcstoretype PKCS12 -srcstorepass password
```

# Shadow CLJS

Compile and watch the client and server builds:

```shell
$ clojure -A:watch
```

Run the Node.js server in another window:

```shell
$ node server.js
```

# REPL

When `watch` is running, you can access the REPL from vim-fireplace
(more info [here](https://clojureverse.org/t/shadow-cljs-and-leinigen-and-npm-oh-my/2735/19)):

```
:Piggieback :client
cqp (js/alert "Hi")
```

View re-frame app DB:

```clojure
(in-ns 'shut-the-box.client.views)
@re-frame.db/app-db
```

Send events from the REPL:

```clojure
(in-ns 'shut-the-box.client.views)
(re-frame/dispatch [:initialize-db])
```

# CLJS DevTools

CLJS DevTools only works in normal browser mode, *not* in the "device
emulation" (aka responsive browser) mode. You can disable the check in
CLJS DevTools that prevents that though (or just use CLJS DevTools in
normal mode):

```clojure
:dev {:compiler-options
      {:external-config
       {:devtools/config
        {:bypass-availability-checks true}}}
```


# Resources

## ClojureScript

- ClojureScript's new [core.async promise interop](https://clojurescript.org/guides/promise-interop#using-promises-with-core-async);
  code [here](https://github.com/clojure/core.async/blob/master/src/main/clojure/cljs/core/async/interop.clj).

## clojure.spec:

- Guide: <https://clojure.org/guides/spec>
- Tips: <https://conan.is/blogging/clojure-spec-tips.html>
- Beginner's FAQ: <https://blog.taylorwood.io/2018/10/15/clojure-spec-faq.html>
- spec for functions: <https://blog.taylorwood.io/2017/10/15/fspec.html>
- `defn-spec` for defining a function and providing a spec: <https://github.com/jeaye/orchestra>

## CSS

- iOS *browser window* sizes (as opposed to just raw screen sizes): <https://www.kylejlarson.com/blog/iphone-6-screen-size-web-design-tips/>
- iOS design cheat-sheet (status bar and nav bar heights): <https://kapeli.com/cheat_sheets/iOS_Design.docset/Contents/Resources/Documents/index>

## STUN/TURN

- Twilio's TURN API: <https://www.twilio.com/docs/stun-turn>

## Browser Audio/Video

- Safari bug where the microphone may not capture any input if the
  microphone is in use (perhaps because *Safari* is using it after a
  site did not cleanly give up the microphone).
  <https://github.com/twilio/twilio-video.js/issues/941>
- Detailed information on the new (autoplay) `<video>` policies in
  Safari iOS: <https://webkit.org/blog/6784/new-video-policies-for-ios/>
- Docs on [MediaDevices.getUserMedia](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)
  which has information on constraining the size, FPS, etc.

## WebRTC Libraries

- Good overview of WebRTC, with a discussion of all of the protocols:
  <https://hpbn.co/webrtc/>
- Alternatives to [simple-peer](https://github.com/feross/simple-peer):
  - [PeerJS](https://peerjs.com/) is a peer-to-peer (only) helper for
  easily connecting WebRTC clients, but does require an open-source
  "PeerServer" to connect those clients. I don't think that this helps
  us, because we need the server anyway for sign in, notifications, etc.
  and so we can just exchange ICE data ourselves (and trigger new peer
  connections).
  - [Geckos.io](https://github.com/geckosio/geckos.io) looks like a
  "batteries included" approach to building client-server games using
  WebRTC. It uses the same `node-webrtc` library used by `simple-peer`
  for the Node.js server. I don't think we need this either, because we
  are not terminating any WebRTC connections on our server.
- [node-webrtc](https://github.com/node-webrtc/node-webrtc) is the
  underlying Node.js library that powers all of the server-side WebRTC
  termination. It uses Google's WebRTC native library to do all of the
  complex stuff (DTLS, SCTP, WebRTC, etc.). This might be an interesting
  way to try out a server-based peer (as opposed to WebSockets).
  - Important bug fix/issue (which I think has now been integrated?)
  related to large numbers of WebRTC connections:
  <https://github.com/node-webrtc/node-webrtc/issues/362> and
  [here](https://bugs.chromium.org/p/webrtc/issues/detail?id=7585) and
  [this](https://github.com/node-webrtc/node-webrtc/issues/590). Note
  that the Google WebRTC library apparently uses 7 file descriptors per
  connection, which seems really high...
