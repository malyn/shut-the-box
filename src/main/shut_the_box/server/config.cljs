(ns shut-the-box.server.config
  (:require
    [macchiato.env :as config]
    [mount.core :refer [defstate]]))

(defstate env
  :start (-> (config/env)
             (update-in [:host] (fnil identity "localhost"))
             (update-in [:port] (fnil identity 3000))))
