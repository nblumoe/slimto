(ns slimto.app
  (:require [reagent.core :as reagent :refer [atom]]))

(def app-state (atom {:page :main}))

(defn swap-page [target]
  (swap! app-state assoc :page target))


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
   :size "sm"
   :contents "back"
   :click-handler #(swap-page target)])

(defn main-page []
  [:div
   [:h1.text-center "slimto"]
   [:p.text-center "Gemeinsam abnehmen"]
   [:div.main-menu.text-center
    [pages-button :entry "Eintrag" :icon "edit"]
    [pages-button :progress "Fortschritt" :icon "stats"]
    [pages-button :settings "Einstellungen" :icon "cog"]]])

(defn page-component []
  (let [pages {:main [main-page]}]
    ((:page @app-state) pages)))

(defn init []
  (reagent/render-component [page-component]
    (.getElementById js/document "container")))
