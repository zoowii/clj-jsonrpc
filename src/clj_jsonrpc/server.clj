(ns clj-jsonrpc.server
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clj-jsonrpc.jsonrpc :as jsonrpc])
  (:use compojure.core)
  (:use ring.util.response)
  (:use ring.middleware.json-params))

(defn json-response [data & [status]]
  "wrapper data into a json response"
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/json-str data)})

(def rpc-conn (jsonrpc/create-connection
                (fn [ctx]
                  "jsonrpc req reader"
                  (let [req (:req ctx)
                        req-body (:body req)
                        req-body-str (slurp req-body)]
                    req-body-str))
                nil))

(defn dispatch-rpc-request [req]
  "Dispatches json rpc requests to the rpc dispatcher"
  (json-response (jsonrpc/rpc-result (jsonrpc/process-rpc-request rpc-conn req))))

(defroutes rpc-routes
           (GET "/" req (str req))
           (POST "/" req
                 (dispatch-rpc-request (slurp (:body req)))
                 ))
(def app
  (-> rpc-routes
      handler/site))

;;; TODO:  websocket wrapper, etc.