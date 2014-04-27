(ns flexibot.wiki
  (require [clj-json.core :as json]
           [http.async.client :as http]))

(defn query-api [url query]
  (with-open [client (http/create-client)]
    (let [resp (http/GET client url :query query)]
      (-> resp
          http/await
          http/string
          json/parse-string))))

(def url-query
  {:action "query"
   :prop "info"
   :inprop "url"
   :format "json"})

(def random-query
  {:action "query"
   :list "random"
   :rnlimit "1"
   :rnnamespace "0"
   :format "json"})

(def api-url "http://en.wikipedia.org/w/api.php")

(defn random-id []
  (let [resp (query-api api-url random-query)]
    (get-in resp ["query" "random" 0 "id"])))

(defn full-url [id]
  (let [resp (query-api api-url (assoc url-query :pageids id))]
    (get-in resp ["query" "pages" (str id) "fullurl"])))

(def random-page (comp full-url random-id))
