(ns shut-the-box.server.db
  (:require
    [cljs.core.async :refer [close! put!] :as async]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]
    [shut-the-box.server.config :refer [env]]))

(defstate ^{:on-reload :noop} conn
  :start (atom {:last-game-id 0
                :games {}}))

(defn create-game!
  [conn game]
  (let [ch (async/chan)
        game-id (-> conn
                    (swap! update :last-game-id inc)
                    :last-game-id)]
    (swap! conn assoc-in [:games game-id] game)
    (put! ch game-id)
    ch))

(defn get-game
  [conn game-id]
  (let [ch (async/chan)]
    (if-let [game (get-in @conn [:games game-id])]
      (put! ch game)
      (close! ch))
    ch))

(defn put-game!
  [conn game-id game]
  (let [ch (async/chan)]
    (swap! conn assoc-in [:games game-id] game)
    (put! ch :ok)
    ch))
