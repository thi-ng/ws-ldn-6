(ns ws-ldn-6.app
  (:require-macros
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [reagent.core :as reagent]))

(defn init
  [this]
  )

(defn redraw
  [this]
  (fn [t frame]
    (:active (reagent/state this))))
