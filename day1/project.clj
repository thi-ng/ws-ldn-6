(defproject day1 "0.1.0-SNAPSHOT"
  :description "WS-LDN-6 day1 exercises"
  :url "http://workshop.thi.ng/"
  :license {:name "Apache Software License"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [thi.ng/geom "0.0.1151-SNAPSHOT"]
                 [org.jogamp.gluegen/gluegen-rt "2.3.2" :classifier "natives-macosx-universal"]
                 [org.jogamp.jogl/jogl-all "2.3.2" :classifier "natives-macosx-universal"]]
  :aot [ws-ldn-6.ex06]
  :main ws-ldn-6.ex06)
