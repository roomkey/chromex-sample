(ns chromex-sample.dev.auto-reload
  (:require [chromex.logging :refer-macros [log]]))

(defn reloaded [& _]
  (log "WOOT!"))
