(ns chromex-sample.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.runtime :as runtime]
            [chromex-sample.background.storage :refer [test-storage!]]
            [ajax.core :refer [GET]]))

(def clients (atom []))


; -- api request ------------------------------------------------------------------------------------------------------------

(def request-url "http://www.roomkey.com/j/autofill")

(defn handle-api-success [{:keys [_ locations airports]} client]
  (log "locations" locations))

(defn handle-api-error [client {:keys [status status-text]}]
  (log "Uh oh: " status " " status-text))

(defn api-request [client]
  (log "make api request for client" client)
  (GET request-url
       {:params {:query "ch"}
        :handler #(handle-api-success % client)
        :error-handler #(handle-api-error % client)
        :response-format :json
        :keywords? true}))


; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  ; (log "BACKGROUND: client connected" (get-sender client))
  (swap! clients conj client))

(defn remove-client! [client]
  ; (log "BACKGROUND: client disconnected" (get-sender client))
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))


; -- client event loop ------------------------------------------------------------------------------------------------------

(defn run-client-message-loop! [client]
  (go-loop []
    (when-let [message (js->clj (<! client))]
      (let [type (get message "type")
            data (get message "data")]
        (case type
          "scrape" (do
                    (log "Scrape success:" data)
                    (api-request client))
          "log" (log data)
          nil))
        (recur))
      (remove-client! client)))


; -- event handlers ---------------------------------------------------------------------------------------------------------

(defn handle-client-connection! [client]
  (add-client! client)
  (post-message! client (clj->js {:type "log"
                                  :data "hello from BACKGROUND PAGE!"}))
  (run-client-message-loop! client))

(defn tell-clients-about-new-tab! []
  (doseq [client @clients]
    (post-message! client (clj->js {:type "log"
                                    :data "a new tab was created"}))))

; -- main event loop --------------------------------------------------------------------------------------------------------

(defn process-chrome-event [event-num event]
  ; (log (gstring/format "BACKGROUND: got chrome event (%05d)" event-num) event)
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      ::tabs/on-created (tell-clients-about-new-tab!)
      nil)))

(defn run-chrome-event-loop! [chrome-event-channel]
  ; (log "BACKGROUND: starting main event loop...")
  (go-loop [event-num 1]
    (when-let [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))))
    ; (log "BACKGROUND: leaving main event loop")))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (log "BACKGROUND: init")
  (test-storage!)
  (boot-chrome-event-loop!))
