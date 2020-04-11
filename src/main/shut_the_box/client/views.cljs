(ns shut-the-box.client.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [shut-the-box.client.events :as events]
    [shut-the-box.client.subs :as subs]
    [shut-the-box.client.ui :refer [modal]]
    [taoensso.timbre :as log]))

(defn new-game-modal
  []
  [modal
   :title "New Game"
   :child [[:div.enter-your-name
            [:div "Your Name"]
            [:input
             {:type "text"
              :auto-focus "autofocus"
              :style {:font-size "2rem"
                      :width "100%"}
              :on-change #(dispatch [::events/update-player-name
                                     (-> % .-target .-value)])}]]]
   :buttons [{:class "cancel"
              :style {:background-color "red"
                      :border-radius "0 0 0 16px"}
              :on-click #(dispatch [::events/cancel-new-game])
              :label "Cancel"}
             {:class "ok"
              :style {:background-color "green"
                      :border-radius "0 0 16px 0"}
              :on-click #(dispatch [::events/start-new-game])
              :label "Ok"}]])

(defn join-game-modal
  []
  [modal
   :title "Join Game"
   :child [[:div.enter-your-name
            [:div "Game Id"]
            [:input
             {:type "text"
              :auto-focus "autofocus"
              :style {:font-size "2rem"
                      :width "100%"}
              :on-change #(dispatch [::events/update-game-id
                                     (-> % .-target .-value js/parseInt)])}]
            [:div "Your Name"]
            [:input
             {:type "text"
              :style {:font-size "2rem"
                      :width "100%"}
              :on-change #(dispatch [::events/update-player-name
                                     (-> % .-target .-value)])}]]]
   :buttons [{:class "cancel"
              :style {:background-color "red"
                      :border-radius "0 0 0 16px"}
              :on-click #(dispatch [::events/cancel-join-game])
              :label "Cancel"}
             {:class "ok"
              :style {:background-color "green"
                      :border-radius "0 0 16px 0"}
              :on-click #(dispatch [::events/start-join-game])
              :label "Ok"}]])

(defn join-view
  []
  [:div.join
   [:div.title
    [:div "Shut the Box"]]
   [:div.buttons
    [:button.new-game
     {:on-click #(dispatch [::events/new-game])}
     "New Game"]
    [:button.join-game
     {:on-click #(dispatch [::events/join-game])}
     "Join Game"]]
   [:div.credits
    [:div
     "A friendly game by Michael Alyn Miller"]]
   (when-let [creating-new-game? @(subscribe [::subs/creating-new-game?])]
     (new-game-modal))
   (when-let [joining-game? @(subscribe [::subs/joining-game?])]
     (join-game-modal))])

(defn joining-view
  []
  [:div
   [:h1 "Joining..."]])

(defn player-icon
  []
  #_[:div.video
     {:id (str "video-" id)
      :style {:width "120px"
              :height "120px"}}]
  [:div.avatar])

(defn die
  [x]
  (with-meta
    [:img.die
     {:src (str "/img/dice/dieWhite" x ".png")
      :width "64"
      :height "64"
      :style {;; Set max height, then auto-scale width to keep aspect
              ;; ratio.
              :height "100%"
              :width "auto"
              :margin-left "4px"}}]
    {:key (str "dice-" x)}))

(defn dice
  [xs]
  [:div.dice
   (map die xs)])

(defn tile
  [n up?]
  (with-meta
    [:div.tile
     {:class [(if up? "up" "down")]}
     [:div.number
      (if up? n "")]]
    {:key (str "tile-" n)}))

(defn tile-set
  [tiles]
  [:div.tiles
   (map-indexed (fn [tile-index up?]
                  (tile (inc tile-index) up?)) tiles)])

(defn player-tile
  [player-index [player-id {:keys [state name tiles last-roll]}]]
  (with-meta
    [:div.player
     {:class (str "avatar" player-index)}
     [player-icon]
     [:div.player-name
      name]
     [:div.player-state
      {:class state}
      (case state
        :rolling "Rolling..."
        :thinking (dice last-roll)
        :done (dice last-roll)
        "")]
     [tile-set tiles]]
    {:key (str "player-tile-" player-index)}))

(defn playing-view
  [game-id game]
  [:div.playing
   [:h1 (str "Game #" game-id)]
   [:div
    (map-indexed player-tile (:players game))]])

(defn main-panel
  []
  (let [state @(subscribe [::subs/state])]
    (case state
      :unjoined (join-view)
      :joining (joining-view)
      :joined (playing-view @(subscribe [::subs/game-id])
                            @(subscribe [::subs/game])))))
