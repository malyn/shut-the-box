(ns shut-the-box.client.views
  (:require [re-frame.core :refer [subscribe]]))

(defn main-panel []
  [:div
   [:h1 "Shut the Box"]
   [:div.player1
    "Player 1"]])
