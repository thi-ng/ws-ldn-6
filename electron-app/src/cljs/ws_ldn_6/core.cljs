(ns ws-ldn-6.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-6.app :as app]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [reagent.core :as r]))

(defonce app (r/atom {}))

(def fs (js/require "fs"))
(def ipc (.-ipcRenderer (js/require "electron")))

(comment
  (.writeFile fs "./cljs-file-output.txt" "Hello File system!"))

(defn set-dir-root!
  [root]
  (let [root (str root "/")]
    (.readdir
     fs root
     (fn [err files]
       (if-not err
         (do
           (swap! app assoc :dir
                  {:root  root
                   :files (mapv
                           (fn [f]
                             {:name f
                              :path (str root f)
                              :dir? (.. fs (lstatSync (str root f)) isDirectory)})
                           files)})
           (.send ipc "set-badge" (str (count files))))
         (warn err))))))

(defn set-curr-file!
  [path]
  (.readFile
   fs path
   (fn [err body]
     (swap! app assoc :curr-file
            {:path path
             :body (if err
                     "error loading file"
                     (.toString body "utf-8"))}))))

(defn file-list
  []
  (let [files (reaction (-> @app :dir :files))]
    (fn []
      (if (seq @files)
        [:ul
         (doall
          (for [{:keys [name path dir?]} @files]
            [:li {:key path}
             (if dir?
               [:a {:href "#" :on-click #(set-dir-root! path)} name]
               [:a {:href "#" :on-click #(set-curr-file! path)} name])]))]))))

(defn editor
  []
  (let [curr (reaction (:curr-file @app))]
    (fn []
      [:div
       [:h3 "Editor area" (if @curr [:small (:path @curr)])]
       [:textarea {:cols 120 :rows 25 :read-only true :value (:body @curr)}]])))

(defn app-component
  []
  [:div.container-fluid
   [:h1 "WS-LDN-6"]
   [:div.row
    [:div.col-md-3 [file-list]]
    [:div.col-md-9 [editor]]]])

(defn mount-root
  []
  (js/Notification. (str (js/Date.)) #js {:body "Reloaded"})
  (.send ipc "bounce-dock")
  (r/render-component [app-component] (.getElementById js/document "app")))

(defn init!
  []
  (.initializeTouchEvents js/React)
  (set-dir-root! ".")
  (mount-root))
