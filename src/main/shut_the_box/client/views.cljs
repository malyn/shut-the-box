(ns shut-the-box.client.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [shut-the-box.client.events :as events]
    [shut-the-box.client.subs :as subs]
    [shut-the-box.client.ui :refer [modal]]
    [shut-the-box.common.logic.tile :as tile-logic]
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
  [player-id video?]
  [:div.avatar
   [:video
    {:id (str "video-" player-id)
     :style {:display (if (true? video?) "block" "none")}
     ;; For some reason we need this for iOS (Safari) to actually play
     ;; the video; otherwise it just shows a static frame.
     :playsInline true}]])

(defn die
  [index x]
  ;; TODO Can we apply the metadata during the map? It's such a hassle
  ;; to have to pass the `index` around to all of these things...
  (with-meta
    [:div.die
     {:class (str "die" x)}]
    {:key (str "dice-" index)}))

(defn dice
  [xs]
  [:div.dice
   (map-indexed die xs)])

(defn tile
  [n up?]
  (with-meta
    [:div.tile
     {:class [(if up? "up" "down")]}
     [:div.number
      (if up? n "")]]
    {:key (str "tile-" n)}))

;; 5x2 matrix of bordered numbers
(defn tile-set
  [tiles]
  [:div.tiles
   (map-indexed (fn [tile-index up?]
                  (tile (inc tile-index) up?)) tiles)])

;; Small, horizontal line of numbers
#_(defn tile-set
  [tiles]
  [:div
   {:style {:display "flex"}}
   (map-indexed
     (fn [tile-index up?]
       [:div
        {:style (merge {:width "6vw"
                        :height "6vw"
                        ;;:color "#a16639"
                        ;;:background-color "#a16639"
                        ;;:border-radius "2px"
                        :margin-left "2px"
                        :font-weight "700"
                        :display "flex"
                        :justify-content "center"
                        :align-items "center"
                        }
                       (if up?
                         {:color "#454343"
                          :background-color "#a16639"
                          :border-radius "2px"}
                         {:color "#a16639"
                          :font-weight "400"}))}
        [:div
         (inc tile-index)]])
     tiles)])

(defn player-tile
  [player-index [player-id {:keys [state name tiles last-roll video?]}]]
  (with-meta
    [:div.player
     {:class (str "avatar" player-index)}
     [player-icon player-id video?]
     [:div.player-name
      name]
     [tile-set tiles]
     (case state
       :waiting [:div.state
                 [:div.state-title "Waiting"]
                 [:div.state-value "- -"]]
       :rolling [:div.state
                 [:div.state-title "Rolling"]
                 [:div.state-value ""]]
       :thinking [:div.state
                  [:div.state-title "Roll"]
                  [:div.state-value (dice last-roll)]]
       :no-moves [:div.state
                  [:div.state-title "Last Roll"]
                  [:div.state-value (dice last-roll)]]
       :done [:div.state
              [:div.state-title "Score"]
              [:div.state-value (tile-logic/score tiles)]]
       :shut-box [:div.state
                  [:div.state-title "SHUT BOX!!!"]
                  [:div.state-value "0"]])]
    {:key (str "player-tile-" player-index)}))

(defn game-waiting-actions
  []
  [:div.actions
   [:div.start-round
    {:on-click #(dispatch [::events/start-round])}
    "Start Round"]])

(defn game-done-actions
  []
  [:div.actions
   [:div.start-round
    {:on-click #(dispatch [::events/start-round])}
    "Start Next Round"]])

(defn rolling-player-actions
  [player]
  [:div.actions
   [:div.roll-dice
    {:on-click #(dispatch [::events/roll-dice])}
    "Roll Dice"]])

(defn selectable-tiles
  [{:keys [tiles last-roll]} selected-tiles]
  [:div.selectable-tiles
   ;; TODO Need to get player-index from the player data (per TODO.md)
   {:class (str "avatar" 0)}
   (map-indexed
     (fn [index up?]
       (let [tile-num (inc index)]
         (with-meta
           [:div.possible-tile
            (cond
              (not up?)
              {:class "down"}

              (contains? selected-tiles tile-num)
              {:class "selected"
               :on-click #(dispatch [::events/deselect-tile tile-num])}

              :else
              {:class "up"
               :on-click #(dispatch [::events/select-tile tile-num])})
            [:div.number
             tile-num]]
           {:key (str "tile-" tile-num)})))
     tiles)])

(defn thinking-player-actions
  [{:keys [tiles last-roll] :as player} selected-tiles]
  [:div.actions
   [:div.left-well
    [dice last-roll]]
   [selectable-tiles player selected-tiles]
   [:div.ok
    (if (tile-logic/valid-combination? tiles
                                       (apply + last-roll)
                                       selected-tiles)
      {:class "enabled"
       :on-click #(dispatch [::events/shut-tiles])}
      {:class "disabled"})
    [:i.fas.fa-check]]])

(defn no-moves-player-actions
  [{:keys [last-roll] :as player}]
  [:div.actions.no-moves
   [:div.left-well
    [dice last-roll]]
   [selectable-tiles player #{}]
   [:div.ok
    {:class "enabled"
     :on-click #(dispatch [::events/end-turn])}
    [:i.fas.fa-times]]])

(defn done-player-actions
  [player]
  [:div.actions])

(defn waiting-player-actions
  [player]
  [:div.actions])

(defn playing-view
  [game-id game player selected-tiles]
  [:div.playing
   [:div.nav
    [:div.left-nav]
    [:div.center-nav
     [:span.title (str "Game #" game-id)]]
    [:div.right-nav
     [:button.video-toggle
      {:on-click #(dispatch [::events/enable-video])}
      [:i.fas.fa-video]]]]

   [:div.players
    (map-indexed player-tile (:players game))]

   (cond
     (= :waiting (:state game)) (game-waiting-actions)
     (= :done (:state game)) (game-done-actions)
     (#{:waiting} (:state player)) (waiting-player-actions player)
     (#{:rolling} (:state player)) (rolling-player-actions player)
     (#{:thinking} (:state player)) (thinking-player-actions player selected-tiles)
     (#{:no-moves} (:state player)) (no-moves-player-actions player)
     (#{:done} (:state player)) (done-player-actions player))])

(defn main-panel
  []
  (let [state @(subscribe [::subs/state])]
    (case state
      :unjoined (join-view)
      :joining (joining-view)
      :joined (playing-view @(subscribe [::subs/game-id])
                            @(subscribe [::subs/game])
                            @(subscribe [::subs/player])
                            @(subscribe [::subs/selected-tiles])))))
