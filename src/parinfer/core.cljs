(ns ^:figwheel-always parinfer.core
  (:require
    [om.core :as om :include-macros true]
    [sablono.core :refer-macros [html]]
    [clojure.string :as string]
    [parinfer.vcr-data :as vcr-data]
    [parinfer.vcr :refer [vcr
                          play-recording!
                          stop-playing!
                          render-controls!]]
    [parinfer.editor :refer [create-editor!
                             create-regular-editor!
                             start-editor-sync!
                             get-prev-state
                             ]]
    [parinfer.state :refer [state]]
    [parinfer.format.infer :as infer]
    [parinfer.format.prep :as prep]
    [parinfer.toc :as toc]
    [ajax.core :refer [GET]]))

(enable-console-print!)

(defn create-index-editors! []
  (create-editor! "code-indent" :indent)
  (create-editor! "code-indent-far" :indent-far)
  (create-editor! "code-indent-multi" :indent-multi)

  (create-editor! "code-line" :line)
  (create-editor! "code-comment" :comment)
  (create-editor! "code-wrap" :wrap)
  (create-editor! "code-splice" :splice)
  (create-editor! "code-barf" :barf)
  (create-editor! "code-slurp" :slurp)
  (create-editor! "code-string" :string)
  (create-editor! "code-enter" :enter)

  (let [opts {:readOnly true}]
    (create-editor! "code-warn-good" :warn-good opts)
    (create-editor! "code-warn-bad" :warn-bad opts))

  (create-editor! "code-displaced" :displaced)
  (create-editor! "code-not-displaced" :not-displaced)

  (let [opts {:parinfer-mode :prep}]
    (create-editor! "code-paren-tune" :paren-tune opts)
    (create-editor! "code-paren-frac" :paren-frac opts)
    (create-editor! "code-paren-comment" :paren-comment opts)
    (create-editor! "code-paren-wrap" :paren-wrap opts)) 

  (start-editor-sync!)

  (create-regular-editor! "code-lisp-style")
  (create-regular-editor! "code-c-style")
  (create-regular-editor! "code-skim")
  (create-regular-editor! "code-inspect" {:matchBrackets true})

  (let [cm-input (create-regular-editor! "code-how-input" {:mode "clojure-parinfer"})
        cm-output (create-regular-editor! "code-how-output" {:readOnly true
                                                             :mode "clojure-parinfer"})
        sync! #(.setValue cm-output (infer/format-text (.getValue cm-input)))]
    (when cm-input
      (.on cm-input "change" sync!)
      (sync!)))

  (let [cm-input (create-regular-editor! "code-edit-input")
        cm-output (create-regular-editor! "code-edit-output" {:readOnly true
                                                              :mode "clojure-parinfer"})
        sync! #(.setValue cm-output (or (prep/format-text (.getValue cm-input))
                                        "; ERROR: input must be balanced!"))]
    (when cm-input
      (.on cm-input "change" sync!)
      (sync!))))

(defn animate-when-visible!
  [key-]
  (doto (get-in @state [key- :watcher])
    (.enterViewport #(play-recording! key-))
    (.exitViewport #(stop-playing! key-))))

(def index-anims
  {:indent vcr-data/indent
   :indent-far vcr-data/indent-far
   :indent-multi vcr-data/indent-multi
   :line vcr-data/line
   :wrap vcr-data/wrap
   :splice vcr-data/splice
   :barf vcr-data/barf
   :slurp vcr-data/slurp-
   :displaced vcr-data/displaced
   :not-displaced vcr-data/not-displaced
   :comment vcr-data/comment-
   :string vcr-data/string
   :warn-bad vcr-data/warn-bad
   :warn-good vcr-data/warn-good
   :enter vcr-data/enter
   :paren-tune vcr-data/paren-tune
   :paren-frac vcr-data/paren-frac
   :paren-comment vcr-data/paren-comment
   :paren-wrap vcr-data/paren-wrap})

(defn load-index-anims! []
  (swap! vcr
    (fn [data]
      (reduce
        (fn [result [key- state]]
          (update result key- merge state))
        data
        index-anims)))
  
  (doseq [[key- _] index-anims]
    (animate-when-visible! key-)))

(defn render-index! []
  (toc/init!)
  (create-index-editors!)
  (load-index-anims!)
  (render-controls!))

(defn render-dev! []
  (create-editor! "code-indent-mode" :indent-mode)
  (create-editor! "code-paren-mode" :paren-mode {:parinfer-mode :prep})
  (start-editor-sync!))

(defn state-viewer
  [{:keys [postline-states]} owner]
  (reify
    om/IRender
    (render [_]
      (html
        [:table.state-table
        (for [[i {:keys [insert stack]}] (map-indexed vector postline-states)]
          [:tr
           [:td (inc i)]
           [:td (pr-str insert)]
           [:td (pr-str stack)]])]))))

(defn render-debug-state! []
  (when-let [cm (create-editor! "code-editor" :editor {:viewportMargin Infinity :lineNumbers true})]
    (start-editor-sync!)
    (om/root
      state-viewer
      (get-prev-state cm)
      {:target (js/document.getElementById "debug-state")})))

(defn init! []
  (cond
    js/window.parinfer_devpage (render-dev!)
    js/window.parinfer_debug_state (render-debug-state!)
    :else (render-index!)))

(init!)
