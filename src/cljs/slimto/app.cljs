(ns slimto.app
  (:require [reagent.core      :as reagent :refer [atom]]
            [slimto.utils.time :as time]
            [slimto.plotting   :as plot]
            [slimto.config     :as config]))

;; firebase
(def data-ref (js/Firebase. config/firebase-url))

;; state
(def app-state (atom {:page       :entry
                      :new-weight 84
                      :user-id    nil
                      :email      nil
                      :password   nil
                      :users      nil}))

(defn swap-page [target]
  (swap! app-state assoc :page target))

(defn save-email [user]
  (swap! app-state assoc :email user))

(defn save-user-id [id]
  (swap! app-state assoc :user-id id))

(defn save-pwd [pwd]
  (swap! app-state assoc :password pwd))

(defn current-email []
  (:email @app-state))

(defn current-user-id []
  (.-uid (.getAuth data-ref)))

(defn current-user-data []
  (get-in @app-state [:users (current-user-id)]))

(defn current-slimtos []
  (get-in @app-state [:users (current-user-id) :slimtos]))

(defn save-weight []
  (swap! app-state update-in [:users (current-user-id) :entries]
    assoc (time/now) {:weight (:new-weight @app-state)})
  (.update (.child  data-ref (str (current-user-id) "/entries"))
    (clj->js (get-in @app-state [:users (current-user-id) :entries]))))

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
   :click-handler (fn [] (save-weight)
                    (swap-page target))
   ])

(defn cancel-button [target]
  [styled-button
   :icon "remove"
   :style "danger"
   :contents "abbrechen"
   :click-handler #(swap-page target)])

(defn login-button []
  [styled-button
   :icon "log-in"
   :contents "Anmelden"
   :click-handler #(.authWithPassword data-ref
                     (clj->js {:email    (:email @app-state)
                               :password (:password @app-state)})
                     (fn [error auth-data] (if error
                                            (.log js/console error)
                                            (save-user-id (.-uid auth-data)))
                                            ))])

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
  (let [user-data (current-user-data)
        entries   (:entries user-data)
        goals     (:goals user-data)]
    [:div
     [:h3 "Fortschritt"]
     [slimtos (current-slimtos)]
     [plot/weight-plot entries goals]
     [back-button :main]
     ]))

(defn settings-page []
  [:div
   [:h3 "Einstellungen"]
   [:div.input-group
    [:div.input-group-addon [:span.glyphicon.glyphicon-envelope]]
    [:input.form-control {:type "text"
                          :placeholder "E-Mail"
                          :value (:email @app-state)
                          :on-change #(save-email (-> % .-target .-value))
                          }]]
   [:div.input-group
    [:div.input-group-addon [:span.glyphicon.glyphicon-lock]]
    [:input.form-control {:type "password" :placeholder "Passwort"
                          :on-change #(save-pwd (-> % .-target .-value))
                          }]]
   [login-button]
   [back-button :main]])

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
