(ns shut-the-box.server.core
  (:require
    [macchiato.fs.path :as fs-path]
    [macchiato.middleware.node-middleware :refer [wrap-node-middleware]]
    [macchiato.middleware.proxy-headers :refer [wrap-forwarded-remote-addr]]
    [macchiato.middleware.ssl :refer [wrap-forwarded-scheme wrap-ssl-redirect]]
    [macchiato.server :as http]
    [macchiato.util.response :as response]
    [mount.core :as mount :refer [defstate]]
    [reitit.ring :as ring]
    ["serve-static" :as serve-static]
    [taoensso.timbre :as log]
    [shut-the-box.server.config :refer [env]]
    [shut-the-box.server.websocket :as websocket]))

(defn ->abspath
  "Returns the given service-relative path as an absolute path, enabling
  deployed files and directories to be accessed regardless of Node's
  working directory. Returns the path as-is if it is already absolute
  (or nil)."
  [path]
  (when path
    (if (fs-path/absolute? path)
      path
      (fs-path/join js/__dirname path))))

(defn wrap-connection-close
  [handler]
  (fn [request respond raise]
    (handler request
             #(respond (response/header % "Connection" "close"))
             raise)))

(defn handler
  []
  (ring/ring-handler
    (ring/router [])
    (-> (ring/create-default-handler)
        (wrap-node-middleware (serve-static
                                (->abspath
                                  "node_modules/@fortawesome/fontawesome-free")))
        (wrap-node-middleware (serve-static
                                (->abspath
                                  "resources/public")))
        (wrap-node-middleware (serve-static
                                (->abspath
                                  (if (:dev @env)
                                    "target/dev/public"
                                    "target/release/public")))))
    ;; Force connections to close immediately in dev so that we can get
    ;; instant feedback from server-side changes.  In prod, force SSL
    ;; and support x-forwarded-proto.
    (if (:dev @env)
      {:middleware [wrap-connection-close]}
      {:middleware [wrap-forwarded-scheme
                    wrap-forwarded-remote-addr
                    wrap-ssl-redirect]})))

(defstate ^{:on-reload :noop} server
  :start (doto (http/start
                 {:handler     (handler)
                  :protocol    (:protocol @env)
                  :host        (:host @env)
                  :port        (:port @env)
                  :private-key (->abspath (:private-key @env))
                  :certificate (->abspath (:certificate @env))
                  :on-success  #(log/info "ShutTheBox started on"
                                          (case (:protocol @env)
                                            :http (str "http://" (:host @env) ":" (:port @env))
                                            :https (str "https://" (:host @env) ":" (:port @env))))})
               (http/start-ws (wrap-forwarded-remote-addr
                                websocket/handler)))
  :stop (.close @server))

(defn -main [& args]
  (mount/start))
