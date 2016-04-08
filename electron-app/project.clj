(defproject ws-ldn-6 "0.1.0-SNAPSHOT"
  :description "WS-LDN-6 workshop"
  :url "http://workshop.thi.ng/"
  :license {:name "Apache Software License"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :source-paths ["src/cljs"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [cljsjs/react "0.13.3-1"]
                 [cljsjs/nodejs-externs "1.0.4-1"]
                 [reagent "0.5.1"]
                 [cljsjs/codemirror "5.7.0-3"]
                 [thi.ng/geom "0.0.1146-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.1.3"]]

  :min-lein-version "2.5.3"

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs"]
                :compiler {:output-to      "app/js/p/app.js"
                           :output-dir     "app/js/p/out"
                           :asset-path     "js/p/out"
                           :optimizations  :none
                           :pretty-print   true
                           :cache-analysis true}}}}

  :clean-targets ^{:protect false} [:target-path "out" "app/js/p"]

  :figwheel {:css-dirs ["app/css"]}

  :profiles {:dev
             {:cljsbuild
              {:builds
               {:app {:source-paths ["env/dev/cljs"]
                      :compiler {:source-map true
                                 :main       "ws-ldn-6.dev"
                                 :verbose    true}
                      :figwheel {:on-jsload "ws-ldn-6.core/mount-root"}}}}

              :source-paths ["env/dev/cljs"]

              :dependencies [[figwheel-sidecar "0.5.0-6"]]

              :plugins [[lein-ancient "0.6.8"]
                        [lein-kibit "0.1.2"]
                        [lein-cljfmt "0.4.1"]
                        [lein-figwheel "0.5.0-6"]]}

             :production
             {:cljsbuild
              {:builds
               {:app {:compiler {:optimizations   :advanced
                                 :main            "ws-ldn-6.prod"
                                 :parallel-build  true
                                 :cache-analysis  false
                                 :closure-defines {"goog.DEBUG" false}
                                 :externs         ["externs/misc.js"]
                                 :pretty-print    false}
                      :source-paths ["env/prod/cljs"]}}}}}
  )
