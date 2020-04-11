(ns shut-the-box.client.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [shut-the-box.client.events :as events]
    [shut-the-box.client.subs :as subs]
    [taoensso.timbre :as log]))

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
     "A friendly game by Michael Alyn Miller"]]])

(defn joining-view
  []
  [:div
   [:h1 "Joining..."]])

(defn player-icon
  [player-index]
  #_[:div.video
     {:id (str "video-" id)
      :style {:width "120px"
              :height "120px"}}]
  [:div.avatar
   {:class (str "avatar" player-index)}])

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
  [player-index n up?]
  (with-meta
    [:div.tile
     {:class [(if up? "up" "down")
              (str "avatar" player-index)]}
     [:div.number
      (if up? n "")]]
    {:key (str "player-" player-index "-tile-" n)}))

(defn tile-set
  [player-index tiles]
  [:div.tiles
   (map-indexed (fn [tile-index up?]
                  (tile player-index (inc tile-index) up?)) tiles)])

(defn player-tile
  [player-index [id {:keys [state tiles last-roll]}]]
  (with-meta
    [:div.player
     [player-icon player-index]
     [:div.player-id
      (str "Player #" id)]
     [:div.player-state
      {:class state}
      (case state
        :rolling "Rolling..."
        :thinking (dice last-roll)
        :done (dice last-roll)
        "")]
     [tile-set player-index tiles]]
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
