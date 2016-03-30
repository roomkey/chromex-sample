(ns chromex-sample.dev.background)

(defn reload []
  (.reload (.-location js/document)))
