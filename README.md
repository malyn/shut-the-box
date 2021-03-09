# Shut the Box

Multiplayer web-based version of [Shut the Box][] with real-time video
chat. Created in April 2020 to bring friends and family together during
the pandemic.

Uses [ClojureScript][] on both the client ([re-frame][]) and the server
([Macchiato][]). [Twilio's Network Traversal Service][] provides
STUN/TURN for the WebRTC connections. [shadow-cljs][] is used for
builds, [GitHub Actions][] for CI, and [nginx][] for deployment. The
player icons and dice are from the [Kenney][] [Animal][] and
[Boardgame][] packs.

Why is there no public version of this game available? Because there are
some [open issues](#open-issues) related to STUN/TURN auth (or the lack
thereof...) which means that the current game acts as an open TURN
relay. That is not ideal, since it costs money to relay those packets.
Long term the right approach would be to require user accounts and time
limits on usage, but this is a prototype and those are not prototype-y
things.

[Animal]: https://kenney.nl/assets/animal-pack-redux
[Boardgame]: https://kenney.nl/assets/boardgame-pack
[ClojureScript]: https://clojurescript.org/
[GitHub Actions]: https://github.com/features/actions
[Kenney]: https://kenney.nl/
[Macchiato]: https://macchiato-framework.github.io/
[nginx]: http://nginx.org/
[re-frame]: https://github.com/Day8/re-frame
[shadow-cljs]: https://github.com/thheller/shadow-cljs
[Shut the Box]: https://en.wikipedia.org/wiki/Shut_the_Box
[Twilio's Network Traversal Service]: https://www.twilio.com/docs/stun-turn


# Development Environment

## Building & Running

Make sure you have a [local HTTPS certificate](#local-https-certificates),
created a [Twilio account](#twilio-account), and set up your
[config.edn file](#config.edn). You then have to edit `:devtools-url` in
the `shadow-cljs.edn`.

Compile and watch the client and server builds:

```shell
$ clojure -A:watch
```

Run the Node.js server in another window:

```shell
$ node server.js
```

Point your web browser at the (HTTPS!) version of the domain name that
you created earlier, port 3000. You can do that from multiple devices in
order to test the multiplayer aspects of the game.


## Local HTTPS Certificates

The game has an embedded video chat feature, which means that we need
access to the user's camera. Web browsers [only allow access to that API
from a secure context][] (such as a page hosted over HTTPS). This
significantly complicates the development experience, since you need to
host the local app from an HTTPS server.

One way to do that is to create a self-signed certificate and then add
it to your computer's trust store. That would have to be done on every
device that you plan to use with the game, which, since this is a
multiplayer game might be difficult (on mobile devices, for example).

The mechanism that I went with is to use a Let's Encrypt certificate
that maps to my development machine, which I can then access from
anywhere on my home network. This approach requires you to have *both* a
public DNS server (for the Let's Encrypt validation) and an internal
server (so that your computers can find each other). These could be the
same DNS server, depending on your network configuration.

Here are the steps that I used to create that certificate (on NixOS, but
the resulting certificates can be used on any OS):

1. Get a Nix shell with the [Certbot][], OpenSSL, and Java.
2. Perform a Let's Encrypt [manual DNS challenge][]. Pick a domain name
   like `mydevbox.example.com` (where `mydevbox` is the name of your
   computer on your local network and `example.com` is your domain
   name).
3. The resulting key and certificate can be used as-is with Macchiato's
   Node.js-based HTTPS server. (put the files in
   `certs/dev-fullchain.pem` and `certs/dev-privkey.pem`)
4. Shadow CLJS *also* has a web server (used to communicate with the
   client for triggering reloads) and that server *also* needs the key
   and certificate, but in a Java KeyStore file. You can use OpenSSL to
   convert the PEM files into a Java KeyStore.

This repo includes a shell script that can do all of that for you. Here
is how you use it on NixOS:

```shell
$ nix-shell -p certbot jdk14 openssl
$ ./certs/generate.sh
```

[Certbot]: https://certbot.eff.org/
[manual DNS challenge]: https://certbot.eff.org/docs/using.html#manual
[only allow access to that API from a secure context]: https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia#Encryption_based_security

## Twilio Account

The game uses [Twilio's Network Traversal Service][] to connect clients
together even when they are behind NATs. This requires you to have a
Twilio account, which then automatically provisions a Twilio
"Programmable Video" Network Traversal Service. There are other options
for STUN/TURN services, including open source self-hosted options like
[coturn][]. Twilio is cheap and very easy to get running, so I went with
that.

[coturn]: https://github.com/coturn/coturn

## config.edn

The server uses [macchiato-env][] to load runtime configuration data.
Configuration can be sourced from environment variables, a config file,
or both. For development, using a `config.edn` file is the simplest
approach (but do not check this in!).

Here is a sample file:

```clojure
{:dev true
 :host "0.0.0.0"
 :twilio-account-sid "TWILIO SID HERE"
 :twilio-auth-token "TWILIO AUTH TOKEN HERE"}
```

[macchiato-env]: https://github.com/macchiato-framework/macchiato-env

## REPL

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

## CLJS DevTools

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


# Production Deployment

This will be heavily dependent on your deployment approach. I use nginx
with a custom NixOS Module to run the Node.js server. The continuous
integration action in this repo builds a batteries-included tarball that
can be run as-is in many environments, since it includes the production
`node_modules` used by the server.

You will need to inject your production config as well, which may use
different Twilio credentials, ports, etc. This can be done with
environment variables or via a `config.edn` file. See [macchiato-env][]
for more options.

The following keys are mandatory and have no defaults:

- `:twilio-account-sid` (aka `TWILIO_ACCOUNT_SID` as an environment
  variable)
- `:twilio-auth-token` (aka `TWILIO_AUTH_TOKEN` as an environment
  variable)

If you use nginx (and a UNIX socket for communicating with the Macchiato
application), then you will need a proxy section like this in your
virtual host config:

```nginx
proxyPass = "http://unix:/run/shutthebox/socket/ipc.socket";
proxyWebsockets = true;
```


# Open Issues

- There is no authentication at all. Anyone can join anyone else's game
  (which is trivial, because game ids are monotonically-increasing
  integers).
- No authentication also means that the service ultimately acts as an
  open STUN/TURN relay, so someone could rack up significant charges
  against your Twilio account.


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
