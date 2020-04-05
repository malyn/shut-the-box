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
  [client elem-id uid audio? video? on-success on-failure]
  (let [stream (.createStream AgoraRTC #js {:streamID uid
                                            :audio audio?
                                            :video video?
                                            :screen false})]
    (.init stream
           (fn []
             (log/info "Local stream initialized")
             (.play stream elem-id)
             (.publish client
                       stream
                       (fn [err]
                         (log/error "Unable to publish local stream; err=" err)
                         (on-failure err)))
             ;; FIXME This should only happen after the stream was
             ;; published; always listen for "stream-published" and use
             ;; that to release the on-success callback.
             (on-success))
           (fn [err]
             (log/error "Failed to initialize local stream; err=" err)
             (on-failure err)))))

(defn subscribe-stream!
  [client stream on-failure]
  (.subscribe client
              stream
              (fn [err]
                (log/error "Failed to subscribe to stream; err=" err)
                (on-failure err))))

(defn play-stream!
  [stream elem-id]
  (.play stream elem-id))
