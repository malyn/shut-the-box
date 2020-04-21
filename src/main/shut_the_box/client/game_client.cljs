(ns shut-the-box.client.game-client
  (:require
    [cljs.core.async :refer [<! put!] :as async]
    [clojure.edn :as edn]
    [taoensso.timbre :as log])
  (:import
    [goog.net WebSocket]))

(defn connect!
  []
  (let [conn (atom {:recv-ch (async/chan)
                    :send-ch (async/chan)
                    :websocket nil})
        websocket (WebSocket. true)]
    (async/go-loop []
      (let [websocket (WebSocket. true)
            url (str "wss://" (-> js/document .-location .-host))]
        (doto websocket
              (.addEventListener
                WebSocket.EventType.OPENED
                (fn [e]
                  (log/info "WebSocket opened.")
                  (swap! conn assoc :websocket websocket)))
              (.addEventListener
                WebSocket.EventType.CLOSED
                (fn [e]
                  (log/error "WebSocket closed.")
                  ;; TODO May need to re-get the AuthKey during
                  ;; reconnect..?
                  (swap! conn assoc :websocket nil)))
              (.addEventListener
                WebSocket.EventType.MESSAGE
                (fn [e]
                  (when-let [msg (.-message e)]
                    (log/debug "WebSocket message:" msg)
                    (put! (:recv-ch @conn)
                          (-> msg
                              edn/read-string)
                          #_(-> msg
                                js/JSON.parse
                                (js->clj :keywordize-keys true))))))
              (.open url))))
    conn))

(defn disconnect! [conn]
  (async/close! (:recv-ch @conn))
  (when-let [websocket (:websocket @conn)]
    (.close websocket)))

(defn recv-ch [conn]
  (:recv-ch @conn))

(defn send!
  [conn msg]
  (let [data (prn-str msg) #_(.stringify js/JSON (clj->js msg))]
    (when-let [websocket (:websocket @conn)]
      (try
        (log/debug "Sending:" data)
        (.send websocket data)
        (catch js/Object e
          (log/error "WebSocket send failed:" e))))))
