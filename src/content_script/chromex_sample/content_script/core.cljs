(ns chromex-sample.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [dommy.core :as dommy :refer-macros [sel sel1]]))


; -- a message loop ---------------------------------------------------------------------------------------------------------

(defn process-message! [message]
  (log "process message" message)
  (let [type (get message "type")
        data (get message "data")]
    (case type
      "log" (log "CONTENT SCRIPT: " data)
      nil)))


(defn run-message-loop! [message-channel]
  (log "CONTENT SCRIPT: starting message loop...")
  (go-loop []
    (when-let [message (<! message-channel)]
      (process-message! (js->clj message))
      (recur))
    (log "CONTENT SCRIPT: leaving message loop")))

; -- a simple page analysis  ------------------------------------------------------------------------------------------------

(defn do-page-analysis! [background-port]
  (when-let [scraped (first (sel [".rk-cover-image .container h2"]))]
    (post-message! background-port (clj->js {:type "scrape"
                                             :data (dommy/text scraped)}))))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (post-message! background-port (clj->js {:type "log"
                                             :data "hello from CONTENT SCRIPT! 3"}))
    (run-message-loop! background-port)

    ;; Artificial timeout so that element exists
    ;; Would of course have a better way of waiting
    ;; for an element to exist prior to scraping
    (.setTimeout js/window #(do-page-analysis! background-port) 3000)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "CONTENT SCRIPT: init")
  (connect-to-background-page!))
