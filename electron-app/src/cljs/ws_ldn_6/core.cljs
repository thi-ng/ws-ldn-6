(ns ws-ldn-6.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-6.webgl :as gltoy]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [reagent.core :as r]
   [cljsjs.codemirror :as cm]
   [cljsjs.codemirror.addon.edit.matchbrackets]
   [cljsjs.codemirror.addon.edit.closebrackets]
   [cljsjs.codemirror.addon.selection.active-line]
   [cljsjs.codemirror.mode.clojure]
   [cljsjs.codemirror.mode.clike]
   ))

(defonce app (r/atom {}))

(def fs (js/require "fs"))
(def ipc (.-ipcRenderer (js/require "electron")))
(def shell (js/require "shelljs"))

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
            {:path      path
             :edit-mode (if (re-find #"\.(edn|clj[cs]?)$" path) "text/x-clojure" "text/x-squirrel")
             :body      (if err
                          "error loading file"
                          (.toString body "utf-8"))}))))

(defn update-editor-body
  [body]
  (swap! app assoc-in [:curr-file :body] body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dir-header
  []
  (let [dir (reaction (-> @app :dir :root))]
    [:div "TODO header"]))

(defn file-list
  []
  (let [files (reaction (-> @app :dir :files))]
    (fn []
      (if (seq @files)
        [:ul
         (doall
          (for [{:keys [name path dir?]} @files
                ;;:when dir?
                ;;:while (re-find #"^[\.a-e]" name)
                :let [handler (if dir? #(set-dir-root! path) #(set-curr-file! path))]]
            [:li {:key path} [:a {:href "#" :on-click handler} name]]))]))))

(defn cm-editor
  "CodeMirror react component wrapper. Takes map of component props
  and map of CM config opts. Props map MUST include an :on-change
  handler and a :state value, which must be dereferencable (atom or
  reaction)."
  [props cm-opts]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (let [editor (.fromTextArea js/CodeMirror (r/dom-node this) (clj->js cm-opts))]
        (.on editor "change" #((:on-change props) (.getValue %)))
        (r/set-state this {:editor editor})))

    :should-component-update
    (fn [this]
      (let [editor  (:editor (r/state this))
            val     (:body @(:state props))
            update? (not= val (.getValue editor))]
        (when update?
          (.setOption editor "mode" (:edit-mode @(:state props)))
          (.setValue editor val))
        update?))
    
    :reagent-render
    (fn [_] [:textarea {:default-value (:default-value props)}])}))

(defn editor
  []
  (let [curr (reaction (:curr-file @app))
        body (reaction (-> @app :curr-file :body))]
    (fn []
      [cm-editor
       {:on-change     update-editor-body
        :default-value @body
        :state         curr}
       {:mode              (:edit-mode @curr)
        :theme             "material"
        :matchBrackets     true
        :autoCloseBrackets true
        :styleActiveLine   true
        :lineNumbers       true
        :autofocus         true}])))

(defn gl-component
  [props]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (r/set-state this {:active true})
      ((:init props) this)
      (when (:redraw props)
        (anim/animate ((:redraw props) this))))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (r/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width (.-innerWidth js/window)
         :height (.-innerHeight js/window)}
        props)])}))

(defn app-component
  []
  [:div.container-fluid
   [:h1 "WS-LDN-6"]
   [:div.row
    [:div.col-md-2
     [dir-header]
     [file-list]]
    [:div.col-md-5 [editor]]
    [:div.col-md-5
     [gl-component
      {:init     (fn [this] (debug :init))
       :redraw   (fn [this]
                   (fn [time frame]
                     (:active (r/state this))))
       :width    400
       :height   300
       :on-click #(js/alert "bingo")}]]]])

(defn mount-root
  []
  (js/Notification. (str (js/Date.)) (clj->js {:body (str "Hello " "people")}))
  (.send ipc "bounce-dock" "critical")
  (r/render-component [app-component] (.getElementById js/document "app")))

(defn init!
  []
  (.initializeTouchEvents js/React)
  (set-dir-root! ".")
  (mount-root))
