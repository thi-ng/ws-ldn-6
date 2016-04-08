(ns ws-ldn-6.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [reagent.core :as r]))

(defonce app (r/atom {}))

(def fs (js/require "fs"))
(def ipc (.-ipcRenderer (js/require "electron")))
(def shell (js/require "shelljs"))

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
