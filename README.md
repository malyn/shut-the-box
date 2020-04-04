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
