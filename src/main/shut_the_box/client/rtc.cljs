(ns shut-the-box.client.rtc
  (:require
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch]]
    [taoensso.timbre :as log]
    [shut-the-box.client.agora-rtc :as rtc]
    #_[shut_the_box.client.config :refer [env]]))

(def app-id
  "")

(defstate ^{:on-reload :noop} client
  :start (rtc/connect! app-id))

(defstate on-peer-online
  :start (let [handler (fn [evt]
                         (log/info "Peer joined:" (.-uid evt))
                         (dispatch [:shut-the-box.client.events/peer-joined
                                    (.-uid evt)]))]
           (.on @client "peer-online" handler)
           handler)
  :stop (.off @client "peer-online" @on-peer-online))

(defstate on-peer-leave
  :start (let [handler (fn [evt]
                         (log/info "Peer" (.-uid evt) "left; reason" (.-reason evt))
                         (dispatch [:shut-the-box.client.events/peer-left
                                    (.-uid evt)]))]
           (.on @client "peer-leave" handler)
           handler)
  :stop (.off @client "peer-leave" @on-peer-leave))
