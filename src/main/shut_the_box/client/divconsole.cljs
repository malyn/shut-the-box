(ns shut-the-box.client.divconsole
  (:require
    [taoensso.timbre :as log])
  (:import
    [goog.debug DivConsole Logger LogRecord]))

(defn logger-level [timbre-level]
  (case timbre-level
    :trace Logger.Level.FINEST
    :debug Logger.Level.FINE
    :info Logger.Level.INFO
    :warn Logger.Level.WARNING
    :error Logger.Level.SEVERE
    :fatal Logger.Level.SEVERE
    Logger.Level.INFO))

(defn divconsole-appender [el]
  (let [divconsole (DivConsole. el)]
    {:enabled? true
     :async? false
     :min-level nil
     :output-fn :inherit
     :fn (fn [{:keys [?ns-str instant level output_]}]
           (.addLogRecord
             divconsole
             (LogRecord.
               (logger-level level)
               (force output_)
               ?ns-str
               instant)))}))

(defn init! []
  (let [el (.createElement js/document "div")
        _ (doto (-> el .-style)
                (set! -position "fixed")
                (set! -bottom "0")
                (set! -left "0")
                (set! -zIndex "1000")
                (set! -color "white")
                (set! -backgroundColor "rgba(255,255,255,0.8)")
                (set! -maxHeight "25vh")
                (set! -width "100%")
                (set! -overflow "auto"))]
    (-> js/document .-body (.appendChild el))
    (log/merge-config!
      {:appenders
       ;; Add the divconsole-appender, but at the :info level (no need
       ;; to spam the div console with debug messages).
       {:closure-div-console (merge (divconsole-appender el)
                                    {:min-level :info})}})))
