(ns cljs.user)

;; This namespace only exists to fix a (recent) bug in Shadow CLJS.
;; Basically, if you interact with the Shadow CLJS REPL (via Vim and/or
;; Fireplace only?) then Shadow CLJS needs to find (and be able to
;; compile) a `cljs.user` namespace.
;;
;; <https://github.com/thheller/shadow-cljs/issues/667>
