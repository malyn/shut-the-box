(ns shut-the-box.client.ui)

(defn- modal-button
  [{:keys [class style on-click label]}]
  [:button
   {:class class
    :style (merge {:border "none"
                   :outline "none"
                   :padding "0"
                   :margin "0"
                   :width "100%"
                   :height "3.5rem"
                   :font-size "1.25rem"
                   :font-weight "600"
                   :background-color "#333"
                   :color "#fff"}
                  style)
    :on-click on-click}
   label])

(defn modal
  "Modal dialog."
  [& {:keys [title buttons child]}]
  [:div.modal-mask
   {:style {:position "fixed"
            :z-index "9998"
            :top "0"
            :left "0"
            :width "100%"
            :height "100%"
            :background-color "rgba(0, 0, 0, 0.8)"
            :transition "opacity 0.3s ease"
            :display "flex"
            :align-items "center"
            :justify-content "center"}}
   [:div.modal-container
    {:style {:display "flex"
             :flex-direction "column"
             :width "75vw"
             :background-color "#fff"
             :border-radius "16px"
             :transition "all 0.3s ease"}}
    [:div.modal-header
     {:style {:display "flex"
              :align-items "center"
              :justify-content "center"
              :border-radius "16px 16px 0 0"
              :height "3.5rem"
              :font-size "1.25rem"
              :font-weight "600"
              :background-color "#666"}}
     [:span.modal-title title]]
    (into [:div.modal-content
           {:style {:color "black"
                    :font-size "1.25rem"
                    :padding "1rem"}}]
          child)
    (into [:div.modal-buttons
           {:style {:width "100%"
                    :display "flex"}}]
          (map modal-button buttons))]])
