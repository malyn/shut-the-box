(ns shut-the-box.client.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [shut-the-box.client.events :as events]
    [shut-the-box.client.subs :as subs]
    [taoensso.timbre :as log]))

(defn join-view []
  [:div
   [:h1 "Join a Game"]
   [:div
    {:on-click #(dispatch
                  [::events/join-channel
                   {:channel "toasty-pirates"
                    :uid 0
                    :on-success ::events/join-succeeded
                    :on-failure ::events/join-failed}])}
    "JOIN"]])

(defn joining-view []
  [:div
   [:h1 "Joining..."]])

(defn player-tile [uid]
  [:div.player
   [:div
    {:id (str "video-" uid)
     :style {:width "120px"
             :height "120px"}}]
   (str "Player #" uid)])

(defn playing-view []
  [:div
   [:h1 "Play Shut the Box"]
   [:div.player-me
    [:div#video-me
     {:style {:width "120px"
              :height "120px"}}]
    "Me"]
   (map player-tile @(subscribe [::subs/peers]))])

(defn main-panel []
  (let [state @(subscribe [::subs/state])]
    (case state
      :unjoined (join-view)
      :joining (joining-view)
      :joined (playing-view))))
