(defproject clj-jsonrpc "0.1.1-dev"
  :description "A JSON-RPC 2.0 library for Clojure"
  :url "https://github.com/zoowii/clj-jsonrpc"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :mirrors {"central" {:name "central"
                       :url "http://maven.aliyun.com/nexus/content/groups/public"}}
  :repositories {"aliyun" {:url "http://maven.aliyun.com/nexus/content/groups/public"}
                 "maven"   {:url "http://repo1.maven.org/maven2/"} }
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]])
