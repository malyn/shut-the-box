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

(def avatars
  ["bear"
   "chick"
   "cow"
   "dog"
   "elephant"
   "hippo"
   "horse"
   "narwhal"
   "parrot"
   "penguin"
   "pig"
   "rhino"])

(def avatar-colors
  ;; TODO Also pick the "low" color from the image and use that for the
  ;; text color?
  ["#a16639"   ;; bear
   "#ffcc00"   ;; chick
   "#ffaaff"   ;; cow
   "#9f9898"   ;; dog
   "#d2d0d0"   ;; elephant
   "#4178c2"   ;; hippo
   "#f3dbc8"   ;; horse
   "#528cdb"   ;; narwhal
   "#d23007"   ;; parrot
   "#365a66"   ;; penguin
   "#ee9fee"   ;; pig
   "#bdb6b6"]) ;; rhino

(defn player-icon
  [player-index]
  #_[:div.video
     {:id (str "video-" id)
      :style {:width "120px"
              :height "120px"}}]
  [:img.avatar
   {:src (str "/img/avatars/" (nth avatars player-index) ".png")
    :width "128"
    :height "128"
    :style {:grid-row "1/3"
            :grid-column "1/1"
            :width "100%"
            :height "100%"
            }}])

(defn die
  [x]
  (with-meta
    [:img.die
     {:src (str "/img/dice/dieWhite" x ".png")
      :width "64"
      :height "64"
      :style {:display "block"
              ;; Set max height, then auto-scale width to keep aspect
              ;; ratio.
              :height "100%"
              :width "auto"
              :margin-left "4px"}}]
    {:key (str "dice-" x)}))

(defn dice
  [xs]
  [:div.dice
   {:style {:display "flex"
            :justify-content "flex-end"
            :height "100%"
            :padding "0 2px 4px 0"}}
   (map die xs)])

(defn tile
  [player-index n up?]
  (with-meta
    [:div.tile
     {:style {:width "10%"
              :height (if up? "100%" "16px")
              :margin "0 2px 0px 2px"
              :color "#414040"
              :border-radius "5px"
              :background-color (nth avatar-colors player-index)
              :display "flex"
              :justify-content "center"
              :align-items "flex-end"}}
     [:div
      {:style {:text-align "center"
               :font-weight "700"
               :font-size "1.25em"}}
      (if up? n "")]]
    {:key (str "player-" player-index "-tile-" n)}))

(defn tile-set
  [player-index tiles]
  [:div.tiles
   {:style {:grid-row "2/3"
            :grid-column "2/4"
            :display "flex"
            :align-items "flex-end"}}
   (map-indexed (fn [tile-index up?]
                  (tile player-index (inc tile-index) up?)) tiles)])

(defn player-tile
  [player-index [id {:keys [state tiles last-roll]}]]
  (with-meta
    [:div.player
     {:style {:margin "16px 8px 0px 8px"
              :display "grid"
              :grid-template-columns "72px auto auto"
              :grid-template-rows "24px 48px"
              :grid-column-gap "4px"}}
     [player-icon player-index]
     [:div.player-id
      {:style {:grid-row "1/2"
               :grid-column "2/3"
               :font-size "1.25em"}}
      (str "Player #" id)]
     [:div
      {:style (merge {:grid-row "1/2"
                      :grid-column "3/4"
                      :text-align "right"}
                     (when (= :done state)
                       {"filter" "opacity(0.2)"}))}
      (case state
        :rolling "Rolling..."
        :thinking (dice last-roll)
        :done (dice last-roll)
        "")]
     [tile-set player-index tiles]]
    {:key (str "player-tile-" player-index)}))

(defn playing-view
  [game-id game]
  [:div
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
