(ns shut-the-box.client.core
  (:require
    [mount.core :as mount]
    [reagent.dom :as reagent]
    [re-frame.core :as re-frame]
    [taoensso.timbre :as log]
    [shut-the-box.client.events]
    [shut-the-box.client.server]
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
                     false)
  ;; iOS 13 (?) changed the behavior and now we also have to prevent
  ;; "click" events.
  (.addEventListener (.getElementById js/document "app")
                     "click"
                     (fn [e] (.preventDefault e))
                     false))

(defn ensure-max-size [id]
  (.scrollTo js/window 0 0)
  ;; This was innerWidth/innerHeight, but we changed it since it seemed
  ;; like something in iOS (13?) broke how this used to work. Just an
  ;; FYI, since we may need to adjust this (for example, in standalone
  ;; mode).
  (let [viewport-width (.-clientWidth js/window)
        viewport-height (.-clientHeight js/window)]
    (doto (-> js/document (.getElementById id) .-style)
          (set! -position "fixed")
          (set! -top "0")
          (set! -left "0")
          (set! -width (str viewport-width "px"))
          (set! -height (str viewport-height "px")))))

(defn is-standalone-app? []
  ;; We have to check window.navigator.standalone this way (specifically
  ;; the "standalone" part) because otherwise advanced compilation will
  ;; rename the "standalone" property name.  (I guess this is not part
  ;; of whatever default externs come with Closure/ClojureScript?)
  (some-> js/window (aget "navigator") (aget "standalone")))

(defn init! []
  (mount/start)
  ;; When running in "browser" mode (i.e., Safari on iOS, as opposed to
  ;; as a "Home Screen" app), we need to fix an issue that can crop up
  ;; with using 100vh/100vw: in landscape mode Safari will start the app
  ;; behind the tab bar.  We need to fix that, and then monitor for
  ;; orientation changes so that we can keep it fixed.  This is not
  ;; necessary in "Home Screen" (standalone) mode; the CSS works as
  ;; expected there.
  (when-not (is-standalone-app?)
    (ensure-max-size "app")
    (.addEventListener js/window "resize" #(ensure-max-size "app"))
    (.addEventListener js/window "scroll" #(ensure-max-size "app")))
  (prevent-page-scrolling)
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
