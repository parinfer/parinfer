(ns ^:figwheel-always parinfer.core
  (:require-macros
    [hiccups.core :as hiccups :refer [html]])
  (:require
    [hiccups.runtime]
    [clojure.string :as string]
    [parinfer.vcr-data :as vcr]
    [parinfer.vcr :refer [vcr
                          play-recording!
                          render-controls!]]
    [parinfer.editor :refer [create-editor!
                             create-regular-editor!
                             start-editor-sync!]]
    [parinfer.formatter :refer [format-text]]
    [ajax.core :refer [GET]]))

(enable-console-print!)

;; Create a Table of Contents for a Markdown file.
;; from: https://github.com/chjj/marked/issues/545#issuecomment-74505539

(def toc (atom []))

(defn toc-heading
  [text level raw]
  (this-as this
    (let [anchor (str (aget this "options" "headerPrefix")
                      (-> raw string/lower-case (string/replace #"[^\w]+" "-")))]
      (swap! toc conj {:text text :level level :anchor anchor})
      (str "<h" level " id='" anchor "'>" text "</h" level">\n"))))

(defn toc-renderer []
  (let [renderer (js/marked.Renderer.)]
    (aset renderer "heading" toc-heading)
    renderer))

(.setOptions js/marked #js {:renderer (toc-renderer)})

(defn make-toc-html []
  (html
    [:div
     [:h2 "Table of Contents"]
     (for [{:keys [anchor level text]} @toc]
       [:div {:class (str "toc-link toc-level-" level)}
        [:a {:href (str "#" anchor)} text]])]))

(defn render!
  [md-text]

  ;; initialize page
  (let [element (js/document.getElementById "app")
        html-text (js/marked md-text)]
    (set! (.-innerHTML element) html-text))

  ;; create table of contents
  (let [element (js/document.getElementById "toc")
        toc-html (make-toc-html)]
    (set! (.-innerHTML element) toc-html))

  ;; create editors
  (create-editor! "code-intro" :intro {:styleActiveLine true})

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

  (let [opts {:readOnly true}
        cm-good (create-editor! "code-warn-good" :warn-good opts)
        cm-bad (create-editor! "code-warn-bad" :warn-bad opts)]
    (.refresh cm-good)
    (.refresh cm-bad))

  (create-editor! "code-displaced" :displaced)
  (create-editor! "code-not-displaced" :not-displaced)

  (start-editor-sync!)

  (create-regular-editor! "code-lisp-style")
  (create-regular-editor! "code-c-style")
  (create-regular-editor! "code-skim")
  (create-regular-editor! "code-inspect" {:matchBrackets true})

  (let [cm-input (create-regular-editor! "code-how-input")
        cm-output (create-regular-editor! "code-how-output" {:readOnly true
                                                             :mode "clojure-parinfer"})
        sync! #(.setValue cm-output (format-text (.getValue cm-input)))]
    (.on cm-input "change" sync!)
    (sync!)
    (.refresh cm-input)
    (.refresh cm-output)
    )

  ;; create editor animations
  (swap! vcr update-in [:intro] merge vcr/intro)
  (swap! vcr update-in [:indent] merge vcr/indent)
  (swap! vcr update-in [:indent-far] merge vcr/indent-far)
  (swap! vcr update-in [:indent-multi] merge vcr/indent-multi)
  (swap! vcr update-in [:line] merge vcr/line)
  (swap! vcr update-in [:wrap] merge vcr/wrap)
  (swap! vcr update-in [:splice] merge vcr/splice)
  (swap! vcr update-in [:barf] merge vcr/barf)
  (swap! vcr update-in [:slurp] merge vcr/slurp-)
  (swap! vcr update-in [:displaced] merge vcr/displaced)
  (swap! vcr update-in [:not-displaced] merge vcr/not-displaced)
  (swap! vcr update-in [:comment] merge vcr/comment-)
  
  (swap! vcr update-in [:string] merge vcr/string)
  (swap! vcr update-in [:warn-bad] merge vcr/warn-bad)
  (swap! vcr update-in [:warn-good] merge vcr/warn-good)

  (play-recording! :intro)
  (play-recording! :indent)
  (play-recording! :indent-far)
  (play-recording! :indent-multi)
  (play-recording! :line)
  (play-recording! :wrap)
  (play-recording! :splice)
  (play-recording! :barf)
  (play-recording! :slurp)
  (play-recording! :comment)
  (play-recording! :string)
  (play-recording! :warn-good)
  (play-recording! :warn-bad)
  (play-recording! :displaced)
  (play-recording! :not-displaced)

  (render-controls!))

(defn init! []
  (GET "content.md" {:handler render!}))

(init!)