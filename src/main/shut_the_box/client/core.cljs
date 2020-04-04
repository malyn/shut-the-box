(ns shut-the-box.client.core
  (:require [reagent.dom :as reagent]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [shut-the-box.client.events]
            [shut-the-box.client.subs]
            [shut-the-box.client.views :as views]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn prevent-page-scrolling []
  (.addEventListener (.getElementById js/document "app")
                     "touchmove"
                     (fn [e] (.preventDefault e))
                     false))

(defn init! []
  (prevent-page-scrolling)
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
