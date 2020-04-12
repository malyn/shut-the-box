(ns shut-the-box.client.db
  (:require [clojure.spec.alpha :as s]))

;; spec of app-db
(s/def ::app-db
  (s/keys :req-un []))

(def default-db
  {:state :unjoined
   :selected-tiles #{}
   :peers #{}})
