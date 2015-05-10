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

(defn save-user-id [id]
  (swap! app-state assoc :user-id id))

(defn save-users-data [data]
  (swap! app-state assoc :users data))

(defn update-from-snapshot [snapshot]
  (let [data  (js->clj (.val snapshot) :keywordize-keys true)
        entries (get-in data [(current-user-id) :entries])
        most-recent-weight (->> entries
                             :weights
                             (into (sorted-map-by <))
                             last
                             last
                             :weight)]
    (save-users-data data)
    (swap-new-weight (js/parseFloat most-recent-weight))))

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

(defn activities-sum [activities]
  (reduce (fn [a b] (+ a (-> b last :activity))) 0 activities))

(defn current-slimtos []
  (let [entries     (get-in @app-state [:users (current-user-id) :entries])
        num-weights (count (:weights entries))
        activities  (:activities entries)]
    (+ (* num-weights 10)
      (activities-sum activities))))

;; TODO refactor save-weight and save-activity to make them more DRY
(defn save-weight []
  (swap! app-state update-in [:users (current-user-id) :entries :weights]
    assoc (time/today) {:weight (:new-weight @app-state)})
  (.update (.child  data-ref (str (name (current-user-id)) "/entries/weights"))
    (clj->js (get-in @app-state [:users (current-user-id) :entries :weights]))))

(defn save-activity []
  (swap! app-state update-in [:users (current-user-id) :entries :activities]
    assoc (time/today) {:activity (:new-activity @app-state)})
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
                                    :step "0.01"
                                    :min 0
                                    :defaultValue (:new-weight @app-state)
                                    :placeholder "Gewicht"
                                    :on-change #(swap-new-weight (-> %
                                                                   .-target
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
  (let [user-data  (current-user-data)
        activities (get-in user-data [:entries :activities])
        users-data (:users @app-state)]
    [:div
     [:h3 "Fortschritt"]
     [slimtos (current-slimtos)]
     [plot/weight-plot users-data]
     [:hr]
     [plot/activity-plot activities]
     [back-button :main]]))

(defn settings-page []
  [:div
   [:h3 "Einstellungen"]
   [:img.avatar.img-circle {:src (:image (current-user-data))}]
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
