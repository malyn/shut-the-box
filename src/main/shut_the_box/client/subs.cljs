(ns shut-the-box.client.subs
  (:require [medley.core :refer [filter-vals map-vals]]
            [re-frame.core :refer [reg-sub]]))

;; =====================================================================
;; LAYER 2 Subscriptions
;;
;; *Every* Layer 2 subscription is re-run *every time* app-db changes
;; (there is no filtering, in other words).  Layer 2 subscriptions must
;; therefore be very lightweight and should only be extractors.

(reg-sub
  ::state
  (fn [{:keys [state]}]
    state))

(reg-sub
  ::creating-new-game?
  (fn [{:keys [creating-new-game?]}]
    creating-new-game?))

(reg-sub
  ::joining-game?
  (fn [{:keys [joining-game?]}]
    joining-game?))

(reg-sub
  ::game-id
  (fn [{:keys [game-id]}]
    game-id))

(reg-sub
  ::game
  (fn [{:keys [game]}]
    game))

(reg-sub
  ::player
  (fn [{:keys [player-id game]}]
    (-> game :players (get player-id))))

(reg-sub
  ::selected-tiles
  (fn [{:keys [selected-tiles]}]
    selected-tiles))

(reg-sub
  ::video-players
  (fn [{:keys [video-players]}]
    video-players))


;; =====================================================================
;; LAYER 3 Subscriptions
;;
;; Although these *could* directly access app-db, they should instead
;; subscribe to Layer 2 subscriptions.  This ensures that we only re-run
;; these (potentially expensive) calculations when the data for which
;; they are actually subscribed has changed.  That is why Layer 3
;; subscriptions only access app-db through a Layer 2 subscription.
