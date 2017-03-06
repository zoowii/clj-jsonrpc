(defproject clj-jsonrpc "0.1.2-dev"
  :description "A JSON-RPC 2.0 library for Clojure"
  :url "https://github.com/zoowii/clj-jsonrpc"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :mirrors {"central" {:name "central"
                       :url "http://maven.aliyun.com/nexus/content/groups/public"}}
  :repositories {"aliyun" {:url "http://maven.aliyun.com/nexus/content/groups/public"}
                 "maven"   {:url "http://repo1.maven.org/maven2/"} }
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.2"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.macro "0.1.2"]
                 [ring/ring-core "1.5.1"]
                 [ring/ring-jetty-adapter "1.5.1"]
                 [ring-json-params "0.1.3"]]
  :dev-dependencies [[lein-ring "0.11.0"]]
  :ring {:handler clj-jsonrpc.server/app})

