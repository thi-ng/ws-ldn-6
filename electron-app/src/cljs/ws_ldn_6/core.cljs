(ns ws-ldn-6.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-6.state :as state]
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

(def fs (js/require "fs"))
(def ipc (.-ipcRenderer (js/require "electron")))

(comment
  (.writeFile fs "./cljs-file-output.txt" "Hello File system!"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dir-header
  []
  (let [dir (reaction (-> @state/app :dir :root))]
    [:div @dir ":"]))

(defn file-list
  []
  (let [dir   (reaction (-> @state/app :dir))
        files (reaction (:files @dir))]
    (fn []
      (if (seq @files)
        [:ul
         (when-not (= "./" (:root @dir))
           [:li
            [:span.glyphicon.glyphicon-menu-up]
            [:a {:href "#" :on-click state/set-dir-root-parent!} " parent"]])
         (doall
          (for [{:keys [name path dir?]} @files
                ;;:when dir?
                ;;:while (re-find #"^[\.a-e]" name)
                :let [handler (if dir? #(state/set-dir-root! path) #(state/set-curr-file! path))]]
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
        (.setSize editor "100%" "80vh")
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
  (let [curr (reaction (:curr-file @state/app))
        body (reaction (-> @state/app :curr-file :body))]
    (fn []
      [:div
       [cm-editor
        {:on-change     state/update-editor-body!
         :default-value @body
         :state         curr}
        {:mode              (:edit-mode @curr)
         :theme             "material"
         :matchBrackets     true
         :autoCloseBrackets true
         :styleActiveLine   true
         :lineNumbers       true
         :autofocus         true}]
       [:button.btn.btn-primary
        {:on-click gltoy/update-shader}
        "Compile!"]])))

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

(defn mouse-pos
  []
  (let [mp (reaction (-> @state/app :webgl :mpos))]
    (fn [] (let [[x y] @mp] [:div (int x) ";" (int y)]))))

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
      {:init     gltoy/init
       :redraw   gltoy/redraw
       :width    400
       :height   300
       :on-mouse-move state/set-mouse-pos!}]
     [mouse-pos]]]])

(defn mount-root
  []
  (js/Notification. (str (js/Date.)) (clj->js {:body (str "Reloaded")}))
  (.send ipc "bounce-dock" "critical")
  (r/render-component [app-component] (.getElementById js/document "app")))

(defn init!
  []
  (.initializeTouchEvents js/React)
  (state/set-dir-root! ".")
  (mount-root))
