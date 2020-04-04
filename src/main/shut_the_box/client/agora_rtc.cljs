(ns shut-the-box.client.agora-rtc
  (:require
    [agora-rtc-sdk :as AgoraRTC]
    [taoensso.timbre :as log]))

(defn connect!
  [app-id]
  (doto (.createClient AgoraRTC #js {:mode "rtc" :codec "h264"})
        (.init app-id
               (fn []
                 (log/info "AgoraRTC client initialized."))
               (fn [err]
                 (log/error "AgoraRTC client failed to initialize:" err)))))

(defn join-channel!
  [client channel uid on-success on-failure]
  (.join client
         nil
         channel
         uid
         (fn [uid]
           (log/info "Joined channel" channel "; uid=" uid)
           (on-success channel uid))
         (fn [err]
           (log/error "Join channel failed; channel=" channel "; err=" err)
           (on-failure channel err))))

(defn create-stream!
  [elem-id uid audio? video? on-success on-failure]
  (doto (.createStream AgoraRTC #js {:streamID uid
                                     :audio audio?
                                     :video video?
                                     :screen false})
        (.init (fn []
                 (log/info "Local stream initialized")
                 (on-success))
               (fn [err]
                 (log/info "Failed to initialize local stream; err=" err)
                 (on-failure err)))))
