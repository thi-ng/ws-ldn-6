(ns ws-ldn-6.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [reagent.core :as r]
   [clojure.string :as str]))

(defonce app (r/atom {:curr-file {:body "void mainImage(vec2 pos, vec2 aspect) {\n  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n}"}}))

(def fs (js/require "fs"))
(def ipc (.-ipcRenderer (js/require "electron")))

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

(defn set-dir-root-parent!
  []
  (let [root (-> @app :dir :root)]
    (when-not (= "./" root)
      (let [parent (str/join "/" (butlast (str/split root #"/")))]
        (set-dir-root! parent)))))

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

(defn update-editor-body!
  [body]
  (swap! app assoc-in [:curr-file :body] body))

(defn set-mouse-pos!
  [e]
  (let [r (.. e -target getBoundingClientRect)]
    (swap! app assoc-in [:webgl :mpos] [(- (.-clientX e) (.-left r)) (- (.-clientY e) (.-top r))])))
