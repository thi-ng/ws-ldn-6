(ns ws-ldn-6.webgl
  (:require
   [reagent.core :as r]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.fx :as fx]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.typedarrays.core :as ta]))

(def dummy-shader
  "void mainImage(vec2 pos, vec2 aspect) { gl_FragColor = vec4(0.0,0.0,0.0, 1.0); }")

(def shader-spec
  {:vs "void main(){vUV=uv;gl_Position=model*vec4(position,0.0,1.0);}"
   :fs "void main() {
  vec2 aspect = vec2(1.0, resolution.y / resolution.x);
  vec2 pos = gl_FragCoord.xy / resolution;
  mainImage(pos, aspect);
}"
   :uniforms {:tex        [:sampler2D 0]
              :time       [:float 0]
              :resolution [:vec2 [1280 720]]
              :mpos       [:vec2 [0 0]]
              :model      [:mat4 mat/M44]}
   :varying  {:vUV :vec2}
   :attribs  {:position [:vec2 0]
              :uv       [:vec2 1]}
   :state    {:depth-test false}})

(defn init-fx-quad
  [gl]
  {:attribs      (gl/make-attribute-buffers
                  gl glc/static-draw
                  {:position {:data (ta/float32 [-1 -1, 1 -1, -1 1, 1 1])
                              :size 2}
                   :uv       {:data (ta/float32 [0 0, 1 0, 0 1, 1 1])
                              :size 2}})
   :uniforms     {:tex 0}
   :num-vertices 4
   :mode         glc/triangle-strip})

(defn compile-shader
  [gl user-code]
  (let [spec (update shader-spec :fs #(str user-code %))]
    (sh/make-shader-from-spec gl spec)))

(defn init
  [component]
  (let [canvas (r/dom-node component)
        gl     (gl/gl-context canvas)
        quad   (-> (init-fx-quad gl)
                   (assoc :shader (compile-shader gl dummy-shader)))]
    ))

(defn redraw
  [component]
)