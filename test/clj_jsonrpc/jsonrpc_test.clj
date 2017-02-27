(ns clj-jsonrpc.jsonrpc-test
  (:require [clojure.test :refer :all]
            [clj-jsonrpc.jsonrpc :as jsonrpc]
            [clojure.data.json :as json])
  (:use org.httpkit.server)
  (:import (java.io IOException)))

(def rpc-handlers {
               :sum (fn [& params] (apply + params))
               })

(def rpc-conn (jsonrpc/create-connection
                (fn [ctx]
                  "jsonrpc req reader"
                  (let [req (:req ctx)
                        req-body (:body req)
                        req-body-str (slurp req-body)]
                    req-body-str))
                (fn [ctx msg]
                  "jsonrpc res writer"
                  (send! (:channel ctx) (json/write-str msg)))))

(jsonrpc/bind-rpc-handlers rpc-conn rpc-handlers)

(defn async-handler [req]
  (with-channel req channel
                (if (= :get (:request-method req))
                  (send! channel "hello, this is from server")
                  (let [ctx {:channel channel :req req}]
                    (try (let [rpc-result (jsonrpc/process-rpc-request
                                            rpc-conn
                                            (jsonrpc/read-rpc-message rpc-conn ctx))]
                           (jsonrpc/send-rpc-response rpc-conn ctx
                                                      (jsonrpc/rpc-result rpc-result))
                           )
                         (catch Exception e
                           (jsonrpc/send-error rpc-conn ctx (.getMessage e))))))))


(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn run-demo-server [& args]
  (println "hello")
  (reset! server (run-server #'async-handler {:port 8080})))

(deftest jsonrpc-http-demo-test1
  (do
    (run-demo-server [])))