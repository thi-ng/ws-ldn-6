(ns ws-ldn-6.ex01
  (:import
   [com.jogamp.opengl
    GL GL2 GL3 GL4 GLProfile GLCapabilities GLAutoDrawable GLEventListener]
   [com.jogamp.opengl.util Animator GLBuffers]
   [com.jogamp.opengl.util.glsl ShaderCode ShaderProgram]
   [com.jogamp.newt NewtFactory]
   [com.jogamp.newt.opengl GLWindow]
   [com.jogamp.newt.event MouseEvent MouseListener KeyEvent KeyListener]
   [java.nio Buffer FloatBuffer])
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v]
   [thi.ng.geom.matrix :as mat]))

(def ^:private gl-profiles
  {:gl3 GLProfile/GL3
   :gl4 GLProfile/GL4})

(defn gl-event-proxy
  [{:keys [init dispose resize display]}]
  (let [t0 (System/nanoTime)]
    (proxy [GLEventListener] []
      (init [^GLAutoDrawable drawable]
        (when init (init drawable)))
      (dispose [^GLAutoDrawable drawable]
        (when dispose (dispose drawable)))
      (reshape [^GLAutoDrawable drawable x y width height]
        (when resize (resize drawable x y width height)))
      (display [^GLAutoDrawable drawable]
        (display drawable (* (- (System/nanoTime) t0) 1e-9))))))

(defn key-proxy
  [{:keys [press release]}]
  (proxy [KeyListener] []
    (keyPressed [^KeyEvent e]
      (when press (press e)))
    (keyReleased [^KeyEvent e]
      (when release (release e)))))

(defn gl-window
  [opts]
  (let [display (NewtFactory/createDisplay nil)
        screen  (NewtFactory/createScreen display (get opts :screen 0))
        profile (GLProfile/get (gl-profiles (get opts :profile :gl4)))
        caps    (doto (GLCapabilities. profile)
                  (.setSampleBuffers (if (> (get opts :samples 0) 0) true false))
                  (.setNumSamples (get opts :samples 1))
                  (.setDoubleBuffered (get opts :double-buffer false)))
        win     (GLWindow/create screen caps)]
    (doto win
      (.setSize (get opts :width 1280) (get opts :height 720))
      (.setVisible true)
      (.addGLEventListener (gl-event-proxy (get opts :events))))
    (when-let [k (get-in opts [:events :keys])]
      (.addKeyListener win (key-proxy k)))
    {:display display
     :screen  screen
     :window  win
     :profile profile
     :caps    caps
     :anim    (doto (Animator. win) (.start))}))

(defn float-buffer
  [n-or-coll]
  (if (number? n-or-coll)
    (GLBuffers/newDirectFloatBuffer n-or-coll)
    (-> n-or-coll float-array GLBuffers/newDirectFloatBuffer)))

(defn short-buffer
  [n-or-coll]
  (if (number? n-or-coll)
    (GLBuffers/newDirectShortBuffer n-or-coll)
    (-> n-or-coll short-array GLBuffers/newDirectShortBuffer)))

(defn int-buffer
  [n-or-coll]
  (if (number? n-or-coll)
    (GLBuffers/newDirectIntBuffer n-or-coll)
    (-> n-or-coll int-array GLBuffers/newDirectIntBuffer)))

(defn make-array-buffer
  [^GL3 gl data]
  (let [buf (float-buffer data)
        id  (int-buffer 1)]
    (doto gl
      (.glGenBuffers 1 id)
      (.glBindBuffer GL/GL_ARRAY_BUFFER (.get id 0))
      (.glBufferData GL/GL_ARRAY_BUFFER (* 4 (count data)) buf GL/GL_STATIC_DRAW)
      (.glBindBuffer GL/GL_ARRAY_BUFFER 0))
    id))

(defn make-element-buffer
  [^GL3 gl data]
  (let [buf (short-buffer data)
        id  (int-buffer 1)]
    (doto gl
      (.glGenBuffers 1 id)
      (.glBindBuffer GL/GL_ELEMENT_ARRAY_BUFFER (.get id 0))
      (.glBufferData GL/GL_ELEMENT_ARRAY_BUFFER (* 2 (count data)) buf GL/GL_STATIC_DRAW)
      (.glBindBuffer GL/GL_ELEMENT_ARRAY_BUFFER 0))
    id))

(defn make-vertex-array
  [^GL3 gl spec]
  (let [id (int-buffer 1)]
    (.glGenVertexArrays gl 1 id)
    (.glBindVertexArray gl (.get id 0))
    (doseq [[_ {:keys [id target offset size stride]
                :or   {stride (* 4 size) offset 0}}] (:attribs spec)]
      (doto gl
        (.glBindBuffer GL/GL_ARRAY_BUFFER (.get id 0))
        (.glEnableVertexAttribArray (int target))
        (.glVertexAttribPointer (int target) (int size) GL/GL_FLOAT false (int stride) (int offset))))
    (when-let [index (:index spec)]
      (.glBindBuffer gl GL/GL_ELEMENT_ARRAY_BUFFER (.get index 0)))
    (.glBindVertexArray gl 0)
    (assoc spec :vao id)))

(defn make-shader
  [^GL3 gl spec]
  (let [vs'  (make-array CharSequence 1 1)
        fs'  (make-array CharSequence 1 1)
        _    (aset (aget vs' 0) 0 (get-in spec [:shader :vs]))
        _    (aset (aget fs' 0) 0 (get-in spec [:shader :fs]))
        vs   (ShaderCode. GL3/GL_VERTEX_SHADER 1 vs')
        fs   (ShaderCode. GL3/GL_FRAGMENT_SHADER 1 fs')
        prog (doto (ShaderProgram.)
               (.add vs)
               (.add fs)
               (.init gl))
        id   (.program prog)]
    (doseq [[attr aspec] (:attribs spec)]
      (.glBindAttribLocation gl id (:target aspec) (name attr)))
    (.link prog gl System/out)
    (reduce
     (fn [acc u]
       (assoc-in acc [:shader :uniforms u :loc] (.glGetUniformLocation gl id (name u))))
     (assoc-in spec [:shader :program] id)
     (keys (get-in spec [:shader :uniforms])))))

;;;;;; userland

(def app (atom nil))

(def shader
  {:vs "
#version 330
in vec3 position;
uniform mat4 model;
uniform mat4 view;
uniform mat4 proj;
void main() { gl_Position = proj * view * model * vec4(position, 1.0); }"

   :fs "
#version 330
out vec4 fragColor;
void main() { fragColor = vec4(1.0, 0.0, 0.0, 1.0); }"})

(defn init
  [^GLAutoDrawable drawable]
  (let [^GL3 gl (-> drawable (.getGL) (.getGL3))
        spec    (->> {:attribs {:position {:id (make-array-buffer gl [-1 -1 0 1 -1 0 1 1 0 -1 1 0])
                                           :size   3
                                           :target 0}}
                      :index   (make-element-buffer gl [0 1 2 0 2 3])
                      :shader  {:vs       (:vs shader)
                                :fs       (:fs shader)
                                :uniforms {:model {:type :mat4}
                                           :view  {:type :mat4}
                                           :proj  {:type :mat4}}}}
                     (make-vertex-array gl)
                     (make-shader gl))]
    (prn spec)
    (swap! app assoc :spec spec)))

(defn display
  [^GLAutoDrawable drawable t]
  (let [^GL3 gl (-> drawable (.getGL) (.getGL3))
        {:keys [shader vao] :as spec} (:spec @app)
        unis    (:uniforms shader)
        t       (* 0.25 t)
        model   (-> mat/M44 (g/rotate-x t) (g/rotate-y (* t 1.5)))
        view    (mat/look-at (v/vec3 0 0 3) (v/vec3) (v/vec3 0 1 0))
        proj    (mat/perspective 45 (/ (:width @app) (:height @app)) 0.1 10)]
    (doto gl
      (.glClearColor 0.3 0.3 0.3 1.0)
      (.glClearDepth 1.0)
      (.glClear (bit-or GL/GL_COLOR_BUFFER_BIT GL/GL_DEPTH_BUFFER_BIT))
      (.glUseProgram (:program shader))
      (.glBindVertexArray (.get vao 0))
      (.glUniformMatrix4fv (get-in unis [:model :loc]) 1 false (float-array model) 0)
      (.glUniformMatrix4fv (get-in unis [:view :loc]) 1 false (float-array view) 0)
      (.glUniformMatrix4fv (get-in unis [:proj :loc]) 1 false (float-array proj) 0)
      ;;(.glDrawArrays GL/GL_TRIANGLES 0 3)
      (.glDrawElements GL/GL_TRIANGLES 6 GL/GL_UNSIGNED_SHORT 0)
      )))

(defn -main
  [& args]
  (reset!
   app
   (gl-window
    {:profile         :gl3
     :samples         4
     :double-buffer   true
     :fullscreen      false
     :pointer-visible true
     :events          {:init    init
                       :display display
                       :dispose (fn [_] (prn :dispose) (.stop ^Animator (:anim @app)))
                       :resize  (fn [_ x y w h] (swap! app assoc :width w :height h))
                       :keys    {:press (fn [^KeyEvent e]
                                          (if (= KeyEvent/VK_ESCAPE (.getKeyCode e))
                                            (.destroy ^GLWindow (:window @app))))}
                       :mouse   {:move  (fn [^MouseEvent e]
                                          (prn (.getX e) (.getY e)))
                                 :wheel (fn [^MouseEvent e deltas]
                                          (prn deltas))}}})))
