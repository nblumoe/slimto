(ns slimto.app
  (:require [reagent.core :as reagent :refer [atom]]))

(def app-state (atom {:page :entry
                      :entries [{:date 10 :weight 83 :circ 110}
                                {:date 40 :weight 85 :circ 120}]
                      :new-weight 14
                      :goal {:date 100 :weight 74 :circ 100}
                      :slimtos 123}))

(defn swap-page [target]
  (swap! app-state assoc :page target))

(defn slimtos [num]
  [:div.slimtos
   [:span.glyphicon.glyphicon-grain]
   [:span num]])

(defn styled-button [& opts]
  (let [{:keys [style size icon block click-handler contents]} opts
        classes (clojure.string/join " " [(when style (str "btn-" style))
                                          (when size  (str "btn-" size))
                                          (when block  (str "btn-block"))
                                          ])]
    [:button.btn {:class classes :on-click click-handler :type "button"}
     [:div
      (when icon [:span {:class (str "glyphicon glyphicon-" icon)}])
      [:span contents]]]))

(defn pages-button [target text & opts]
  (let ({:keys [icon]} opts)
    [styled-button
     :contents text
     :icon icon
     :style "primary"
     :block true
     :size "lg"
     :click-handler #(swap-page target)]))

(defn back-button [target]
  [styled-button
   :icon "chevron-left"
   :contents "zurück"
   :click-handler #(swap-page target)])

(defn save-weight []
  (swap! app-state update-in [:entries]
    concat [{:date 40 :weight (:new-weight @app-state)}]))

(defn save-button [target]
  [styled-button
   :icon "ok"
   :style "success"
   :contents "speichern"
   :click-handler #((save-weight)
                    (swap-page target))
   ])

(defn cancel-button [target]
  [styled-button
   :icon "remove"
   :style "danger"
   :contents "abbrechen"
   :click-handler #(swap-page target)])

(defn login-button [target]
  [styled-button
   :icon "user"
   :contents "Anmelden"
   :click-handler #(swap-page target)])

(defn main-page []
  [:div
   [:h1 "slimto"]
   [:p "Gemeinsam abnehmen"]
   [pages-button :entry "Eintrag" :icon "edit"]
   [pages-button :progress "Fortschritt" :icon "stats"]
   [pages-button :settings "Einstellungen" :icon "cog"]])

(defn entry-page []
  [:div
   [:h3 "Heutiger Eintrag"]
   [:form
    [:div.input-group
     [:div.input-group-addon [:span.glyphicon.glyphicon-scale]]
     [:input#weight-input.form-control {:type "number"
                                        :placeholder "Gewicht"
                                        :value (:new-weight @app-state)
                                        :on-change #(swap! app-state
                                                         assoc
                                                         :new-weight
                                                         (js/parseFloat (-> % .-target .-value)))
                                        }]
     [:div.input-group-addon "kg"]]
    [:div.input-group
     [:div.input-group-addon [:span.glyphicon.glyphicon-repeat]]
     [:input#weight-input.form-control {:type "number" :placeholder "Bauchumfang"}]
     [:div.input-group-addon "cm"]]]
   [:p]
   [:div.btn-group
    [cancel-button :main]
    [save-button :progress]]])

(defn circle-component [x y color]
  [:circle {:cx x :cy y :r 3 :style {:fill color}}])

(defn plot-entry [entry]
  (let [x (:date entry)
        y (:weight entry)]
    (circle-component x y "red")))

(defn weight-plot []
  [:div
   [:h5 "Gewicht"]
   [:svg#progress-plot {:width "100%"
                        :preserveAspectRatio "xMidYMid meet"
                        :viewBox "0 0 120 50"
                        :id "canvas"}
    [:rect {:width "100%"
            :height "100%"
            :fill "#EEE"}]
    [:g {:transform (str "scale(1,-1), translate(0,-100)")}
     (map plot-entry (:entries @app-state))
     (let [goal (:goal @app-state)]
       [circle-component (:date goal) (:weight goal) "blue"])]
    ]]
  )


(defn progress-page []
  [:div
   [:h3 "Fortschritte"]
   [slimtos (:slimtos @app-state)]
   [weight-plot]
   [back-button :main]
   ])

(defn settings-page []
  [:div
   [:h3 "Einstellungen"]
   [login-button]
   [back-button :main]
   ])

(defn page-component []
  (let [pages {:main [main-page]
               :entry [entry-page]
               :progress [progress-page]
               :settings [settings-page]
               }]
    ((:page @app-state) pages)))

(defn init []
  (reagent/render-component [page-component]
    (.getElementById js/document "container")))
