(ns slimto.app
  (:require [reagent.core      :as reagent :refer [atom]]
            [slimto.utils.time :as time]
            [slimto.plotting   :as plot]
            ))


;; state
(def app-state (atom {:page :entry
                      :entries {(time/date 2015 3 1) {:weight 83 :circ 110}
                                (time/date 2015 3 2) {:weight 80 :circ 110}
                                (time/date 2015 3 11) {:weight 82 :circ 110}
                                (time/date 2015 3 12) {:weight 79 :circ 110}
                                }
                      :new-weight 72
                      :goals {(time/date 2015 04 30) {:weight 80 :circ 100}}
                      :slimtos 123}))

(defn swap-page [target]
  (swap! app-state assoc :page target))

(defn save-weight []
  (swap! app-state update-in [:entries]
    assoc (time/now) {:weight (:new-weight @app-state)}))

;; UI

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

;; pages

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

(defn progress-page []
  [:div
   [:h3 "Fortschritte"]
   [slimtos (:slimtos @app-state)]
   [plot/weight-plot (:entries @app-state) (:goals @app-state)]
   [back-button :main]
   ])

(defn settings-page []
  [:div
   [:h3 "Einstellungen"]
   [login-button]
   [back-button :main]
   ])

(defn page-router []
  (let [pages {:main [main-page]
               :entry [entry-page]
               :progress [progress-page]
               :settings [settings-page]
               }]
    ((:page @app-state) pages)))

(defn init []
  (reagent/render-component [page-router]
    (.getElementById js/document "container")))
