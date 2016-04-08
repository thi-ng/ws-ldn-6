(ns ws-ldn-6.ex06
  (:gen-class)
  (:import
   [com.jogamp.opengl GL3 GLAutoDrawable]
   [com.jogamp.newt.event MouseEvent KeyEvent])
  (:require
   [thi.ng.math.core :as m]
   [thi.ng.color.core :as col]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.fx :as fx]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.jogl.core :as jogl]
   [thi.ng.geom.gl.jogl.constants :as glc]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def app
  (atom {:version 330
         :example-id :sky
         :mpos (v/vec2)}))

(def shader-spec
  {:vs fx/passthrough-vs
   :fs "
//layout(origin_upper_left) in vec4 gl_FragCoord;
out vec4 fragColor;

{{user-code}}

void main() {
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

(defn prepare-example
  [id]
  (let [code (slurp (io/resource (str "shader/" (name id) ".frag")))]
    (update shader-spec :fs str/replace "{{user-code}}" code)))

(defn init
  [^GLAutoDrawable drawable]
  (let [{:keys [example-id version]} @app
        ^GL3 gl   (.. drawable getGL getGL3)
        view-rect (gl/get-viewport-rect gl)
        shader    (sh/make-shader-from-spec gl (prepare-example example-id) version)
        quad      (assoc (fx/init-fx-quad gl) :shader shader)]
    (swap! app merge
           {:quad   quad
            :shader shader})))

(defn display
  [^GLAutoDrawable drawable t]
  (let [^GL3 gl (.. drawable getGL getGL3)
        {:keys [quad width height mpos]} @app]
    (doto gl
      (gl/set-viewport 0 0 width height)
      (gl/draw-with-shader
       (update quad :uniforms merge
               {:time       t
                :mpos       (m/div mpos width height)
                :resolution [width height]})))))

(defn key-pressed
  [^KeyEvent e]
  (condp = (.getKeyCode e)
    KeyEvent/VK_ESCAPE (jogl/destroy-window (:window @app))
    nil))

(defn mouse-moved [^MouseEvent e] (swap! app assoc :mpos (v/vec2 (.getX e) (.getY e))))

(defn resize [_ x y w h] (swap! app assoc :width w :height h))

(defn dispose [_] (jogl/stop-animator (:anim @app)))

(defn -main
  [& args]
  (swap! app merge
         (jogl/gl-window
          {:profile       :gl3
           :samples       4
           :double-buffer true
           :fullscreen    false
           :events        {:init    init
                           :display display
                           :dispose dispose
                           :resize  resize
                           :keys    {:press key-pressed}
                           :mouse   {:move mouse-moved}}}))
  nil)
