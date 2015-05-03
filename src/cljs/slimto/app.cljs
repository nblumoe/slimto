(ns slimto.app
  (:require [reagent.core      :as reagent :refer [atom]]
            [slimto.utils.time :as time]
            [slimto.plotting   :as plot]
            [slimto.config     :as config]))

(def activity-levels
  {:none    0
   :low    10
   :medium 20
   :high   30})

;; firebase
(def data-ref (js/Firebase. config/firebase-url))

(defn firebase-session-user
  "Returns the user ID from the current firebase sessions if it exists, otherwise returns nil"
  []
  (if-let [auth (.getAuth data-ref)] (keyword (.-uid auth))))

;; state
(def app-state (atom {:page         :main
                      :new-weight   80
                      :new-activity (:none activity-levels)
                      :user-id      (firebase-session-user)
                      :email        nil
                      :password     nil
                      :users        nil}))

(defn update-from-snapshot [snapshot]
  (let [data  (js->clj (.val snapshot) :keywordize-keys true)
        weights (get-in data [(current-user-id) :entries :weights])]
    (swap! app-state assoc-in [:users (current-user-id) :entries :weights] weights)))

(defn save-user-id [id]
  (swap! app-state assoc :user-id id))

(defn- on-authentication [auth-data]
  (when auth-data
    (save-user-id (firebase-session-user))
    (.on data-ref "value" update-from-snapshot
      (fn [err] (.log js/console "Error when reading from Firebase: " err)))))

(.onAuth data-ref on-authentication)

(defn swap-page [target]
  (swap! app-state assoc :page target))

(defn swap-new-weight [weight]
  (swap! app-state assoc :new-weight weight))

(defn swap-new-activity [activity]
  (swap! app-state assoc :new-activity activity))

(defn save-email [user]
  (swap! app-state assoc :email user))

(defn save-pwd [pwd]
  (swap! app-state assoc :password pwd))

(defn current-user-id []
  (:user-id @app-state))

(defn current-email []
  (:email @app-state))

(defn current-user-data []
  (get-in @app-state [:users (current-user-id)]))

(defn current-slimtos []
  (get-in @app-state [:users (current-user-id) :slimtos]))

;; TODO refactor save-weight and save-activity to make them more DRY
(defn save-weight []
  (swap! app-state update-in [:users (current-user-id) :entries :weights]
    assoc (time/now) {:weight (:new-weight @app-state)})
  (.update (.child  data-ref (str (name (current-user-id)) "/entries/weights"))
    (clj->js (get-in @app-state [:users (current-user-id) :entries :weights]))))

(defn save-activity []
  (swap! app-state update-in [:users (current-user-id) :entries :activities]
    assoc (time/now) {:activity (:new-activity @app-state)})
  (.update (.child  data-ref (str (name (current-user-id)) "/entries/activities"))
    (clj->js (get-in @app-state [:users (current-user-id) :entries :activities]))))

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

(defn save-button [save-fn target]
  [styled-button
   :icon "ok"
   :style "success"
   :contents "speichern"
   :click-handler (fn []
                    (save-fn)
                    (swap-page target))])

(defn cancel-button [target]
  [styled-button
   :icon "remove"
   :style "danger"
   :contents "abbrechen"
   :click-handler #(swap-page target)])

(defn login []
  (.authWithPassword data-ref
    (clj->js {:email    (:email @app-state)
              :password (:password @app-state)})
    (fn [error auth-data] (if error (.log js/console error)))))

(defn login-button [target]
  [styled-button
   :icon "log-in"
   :contents "Anmelden"
   :click-handler login])

;; pages

(defn main-page []
  [:div
   [:h1 "slimto"]
   [:p "Gemeinsam abnehmen"]
   [pages-button :weight "Gewicht" :icon "scale"]
   [pages-button :activity "Aktivität" :icon "dashboard"]
   [pages-button :progress "Fortschritt" :icon "stats"]
   [pages-button :settings "Einstellungen" :icon "cog"]])

(defn weight-page []
  [:div
   [:h3 "heutiges Gewicht"]
   [:form
    [:div.input-group
     [:div.input-group-addon [:span.glyphicon.glyphicon-scale]]
     [:input.form-control.input-lg {:type "number"
                                    :placeholder "Gewicht"
                                    :value (:new-weight @app-state)
                                    :on-change #(swap-new-weight (-> % .-target
                                                                   .-value
                                                                   js/parseFloat))}]
     [:div.input-group-addon "kg"]]]
   [:p]
   [:div.btn-group
    [back-button :main]
    [save-button save-weight :progress]]])

(defn activity-page []
  [:div
   [:h3 "heutige Aktivität"]
   [:form
    [:div.input-group
     [:div.input-group-addon [:span.glyphicon.glyphicon-dashboard]]
     [:select.form-control.input-lg
      {:on-change #(swap-new-activity (-> % .-target .-value js/parseInt))}
      [:option {:value (:none activity-levels)} "keine"]
      [:option {:value (:low activity-levels)}  "geringe"]
      [:option {:value (:medium activity-levels)} "mittlere"]
      [:option {:value (:high activity-levels)} "hohe"]]
     [:div.input-group-addon "Anstrengung"]
     ]]
   [:p]
   [:div.btn-group
    [back-button :main]
    [save-button save-activity :progress]]])

(defn progress-page []
  (let [user-data (current-user-data)
        weights   (get-in user-data [:entries :weights])
        goals     (:goals user-data)]
    [:div
     [:h3 "Fortschritt"]
     [slimtos (current-slimtos)]
     [plot/weight-plot weights goals]
     [back-button :main]]))

(defn settings-page []
  [:div
   [:h3 "Einstellungen"]
   [:img.avatar.img-circle {
          :src "data:image/jpg;base64,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"}]
   [:div.alert.alert-info "Noch keine Einstellungen verfügbar"]
   [back-button :main]])

(defn login-page []
  [:div
   [:h3 "Anmelden"]
   [:div.input-group
    [:div.input-group-addon [:span.glyphicon.glyphicon-envelope]]
    [:input.form-control {:type "text"
                          :placeholder "E-Mail"
                          :value (:email @app-state)
                          :on-change #(save-email (-> % .-target .-value))}]]
   [:div.input-group
    [:div.input-group-addon [:span.glyphicon.glyphicon-lock]]
    [:input.form-control {:type "password" :placeholder "Passwort"
                          :on-change #(save-pwd (-> % .-target .-value))}]]
   [login-button :main]])

(defn page-router []
   (let [pages {:main     [main-page]
                :weight   [weight-page]
                :activity [activity-page]
                :progress [progress-page]
                :settings [settings-page]
                :login    [login-page]}]
        (fn []
          (if (current-user-id)
            ((@app-state :page) pages)
            (:login pages)))))


(defn init []
  (reagent/render-component [page-router]
    (.getElementById js/document "container")))
