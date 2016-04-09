(ns ws-ldn-6.webgl
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ws-ldn-6.state :as state]
   [reagent.core :as r]
   [thi.ng.math.core :as m]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.fx :as fx]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.typedarrays.core :as ta]))

(def dummy-shader
  "void mainImage(vec2 pos, vec2 aspect) {
 gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
}")

(def shader-spec
  {:vs "void main(){vUV=uv;gl_Position=vec4(position,0.0,1.0);}"
   :fs "void main() {
  vec2 aspect = vec2(1.0, resolution.y / resolution.x);
  vec2 pos = gl_FragCoord.xy / resolution;
  mainImage(pos, aspect);
}"
   :uniforms {:tex        [:sampler2D 0]
              :time       [:float 0]
              :resolution [:vec2 [1280 720]]
              :mpos       [:vec2 [0 0]]}
   :varying  {:vUV :vec2}
   :attribs  {:position [:vec2 0]
              :uv       [:vec2 1]}
   :state    {:depth-test false}})

(defn compile-shader
  [gl user-code]
  (let [spec (update shader-spec :fs #(str user-code %))]
    (sh/make-shader-from-spec gl spec)))

(defn init
  [component]
  (let [canvas (r/dom-node component)
        gl     (gl/gl-context canvas)
        quad   (-> (fx/init-fx-quad gl)
                   (assoc :shader (compile-shader gl dummy-shader)))]
    (swap! state/app assoc :webgl {:gl gl :quad quad})))

(defn redraw
  [component]
  (fn [time frame]
    (if (:active (r/state component))
      (let [{:keys [gl quad mpos]} (:webgl @state/app)
            res  (v/vec2 400 300)
            mpos (m/div (v/vec2 (or mpos 0)) res)
            mpos (assoc mpos :y (- 1.0 (:y mpos)))]
        (gl/draw-with-shader
         gl (update quad :uniforms merge
                    {:time       time
                     :resolution res
                     :mpos       mpos}))
        true))))

(defn update-shader
  []
  (let [{:keys [gl quad]} (:webgl @state/app)]
    (.deleteProgram gl (-> quad :shader :program))
    (swap! state/app assoc-in [:webgl :quad :shader] (compile-shader gl (-> @state/app :curr-file :body)))))
