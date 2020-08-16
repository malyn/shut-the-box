(ns shut-the-box.server.config
  (:require
    [macchiato.env :as config]
    [macchiato.fs :as fs]
    [mount.core :refer [defstate]]))

(defstate env
  ;; Note that `config.edn` is optional since, in some environments
  ;; (Prod) we *may* only use environment variables. macchiato.env/env
  ;; crashes if it cannot find the config file, so we check for that
  ;; first and then just load from the environment if the file does not
  ;; exist.
  :start (-> (if (fs/exists? (or (:conf (config/env-props)) "config.edn"))
               (config/env)
               (config/env-props))
             (update-in [:protocol] (fnil identity :https))
             (update-in [:host] (fnil identity "0.0.0.0"))
             (update-in [:port] (fnil identity 3000))
             (update-in [:private-key] (fnil identity "certs/dev-privkey.pem"))
             (update-in [:certificate] (fnil identity "certs/dev-fullchain.pem"))))
