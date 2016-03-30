(ns chromex-sample.dev.auto-reload
  (:require [chromex.logging :refer-macros [log]]
            [cljs.core.async :refer [<! chan]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.ext.tabs :as tabs-api]))


; (defn get-extension-tab-loop [tab-channel]
;   (go-loop []
;     (when-let [extension-tabs (<! tab-channel)]
;       (log cljs->extension-tabs)
;       (recur))))


(defn reloaded []
  (log "Background script was recompiled")
  (.reload (.-location js/document))
  ; (let [extension-tabs-channel (make-chrome-event-channel (chan))]
  ;   (tabs-api/query extension-tabs-channel {:url "chrome://extensions"})
  ;   (get-extension-tab-loop extension-tabs-channel))
    )
