(ns clj-jsonrpc.jsonrpc
  (:require [clojure.data.json :as json])
  (:import (java.util List)
           (java.io IOException)))

(def jsonrpc-version "2.0")

(def error-msgs
  ^{:doc "JSON-RPC 2.0 standard error messages."}
  {:parse-error      "Invalid JSON was received by the server."
   :invalid-request  "The JSON sent is not a valid Request object."
   :method-not-found "The method does not exist / is not available."
   :invalid-params   "Invalid method parameter(s)."
   :internal-error   "Internal JSON-RPC error."
   })

(def error-codes
  ^{:doc "JSON-RPC 2.0 standard error codes."}
  {:parse-error      -32700
   :invalid-request  -32600
   :method-not-found -32601
   :invalid-params   -32602
   :internal-error   -32603
   })

(defmulti read-rpc-message
          (fn [x ctx] (:connection-type @x)))

(defmethod read-rpc-message "function"
  [source ctx]
  ((:message-reader @source) ctx))

(defmethod read-rpc-message :default
  [source ctx]
  ((:message-reader @source) ctx))

(defmulti send-rpc-response
          (fn [x ctx msg] (:connection-type @x)))

(defmethod send-rpc-response "function"
  [source ctx msg]
  ((:message-writer @source) ctx msg))

(defmethod send-rpc-response :default
  [source ctx msg] ((:message-writer @source) ctx msg))

;;; jsonrpc v2 implementation for Clojure
(defn create-connection
  "create connection for the jsonrpc"
  ([msg-reader msg-writer]
    (create-connection msg-reader msg-writer nil nil))
  ([msg-reader msg-writer req-reactor-register starter-fn]
   (atom {
          :connection-type "function"
          :message-reader msg-reader
          :message-writer msg-writer
          :request-reactor-register req-reactor-register         ;; register request reactor fn to connection
          :starter-fn starter-fn
          })))

(defn set-rpc-reader
  [conn msg-reader]
  (swap! conn assoc :message-reader msg-reader))

(defn set-rpc-writer
  [conn msg-writer]
  (swap! conn assoc :message-writer msg-writer))

(defn send-notification
  [conn ctx notification]
  (send-rpc-response conn ctx notification))

(defn make-error-response
  ([conn error-msg error-code id]
    {"jsonrpc" jsonrpc-version "id" id "error" {"code" error-code} "message" error-msg})
  ([conn error-msg]
    (make-error-response conn error-msg (:internal-error error-codes) nil)))

(defn send-error
  ([conn ctx error-msg]
    (send-error conn ctx error-msg (:internal-error error-codes) nil))
  ([conn ctx error-msg error-code id]
   (send-rpc-response conn ctx (make-error-response conn error-msg error-code id))))

(defn make-rpc-response
  [conn result id]
  {"result" result "jsonrpc" jsonrpc-version "id" id})

(defmulti dispatch
          "Dispatches requests by method name"
          (fn [conn method params id] {:context conn :method (name method)}))

(defmethod dispatch :default
  [conn method params id]
  (make-error-response conn (str "Can't find method handler " method) (:method-not-found error-codes) id))

(defn defhandler
  [conn method-name handler]
  "bind rpc-handler to rpc request method"
  (defmethod dispatch {:context conn :method (name method-name)}
    [conn method params id]
    (future (try
              (let [res (apply handler params)]
                (make-rpc-response conn res id))
              (catch Exception e
                (make-error-response conn (.getMessage e) (:internal-error error-codes) id))))))

(defn defhandlers
  [conn handlers]
  "bind handlers"
  (doseq [p handlers]
    (defhandler conn (first p) (second p))))

(defn- process-single-rpc-request
  [conn req]
  (let [method (get req "method")
        jsonrpc-version (get req "jsonrpc")
        id (get req "id")
        params (get req "params")]
    (if (or (seq? params) (vector? params))
      (dispatch conn method params id)
      (make-error-response conn (:invalid-params error-msgs) (:invalid-params error-codes) id))))

(defn- process-batch-rpc-requests
  [conn requests]
  (if (pos? (count requests))
    (future
      (for [req requests]
        (let [res (process-single-rpc-request conn req)]
          (if (future? res) @res res))))
    (make-error-response conn (:invalid-request error-msgs) (:invalid-request error-codes) nil)))

(defn batch-rpc-requests?
  [reqs]
  (or (not (map? reqs)) (instance? List reqs)))

(defn rpc-result
  [result-future]
  (if (future? result-future) @result-future result-future))

(defn process-rpc-request-json
  [conn req]
  (if (batch-rpc-requests? req)
    (process-batch-rpc-requests conn req)
    (process-single-rpc-request conn req)))

(defn process-rpc-request
  [conn req]
  (if (string? req)
    (try
      (let [req-json (json/read-str req)]
        (process-rpc-request-json conn req-json))
      (catch Exception e
        (make-error-response conn (:parse-error error-msgs) (:parse-error error-codes) nil)))
    (process-rpc-request-json conn req)))

(defn listen-connection
  [conn]
  (when-let [req-reactor-register (:request-reactor-register @conn)]
    (let [res (req-reactor-register process-rpc-request)]
      (send-rpc-response conn (if (future? res) @res res))))
  (when-let [starter-fn (:starter-fn @conn)]
    ((:starter-fn @conn))))


