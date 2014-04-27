(ns flexibot.core
  (:gen-class)
  (:require [flexibot.psql-db :as db]
            [flexibot.parse :refer [parse-message map->message parse-bot-command]]
            [clojure.string :as s]
            [gloss.core :as gloss]
            [flexibot.wiki :as wiki])
  (:use lamina.core
        aleph.tcp
        carica.core))

(def rainbow-pts-responses
  (vector "Rainbowsaurus has more rainbow points than you can imagine."
          "Rainbowsaurus has so many rainbow points mathematicians named a new number rainbowpointillion in her honor."
          "Do not compare your rainbow points with rainbowsaurus's. You will only despair at your relative insignificance."
          "Error: Number too large."))

(def bot-info {:host "irc.utonet.org"
               :nick (config :nick)
               :user "rainbot"
               :port 6667
               :frame (gloss/string :utf-8 :delimiters ["\r\n"])})

(def output-channel (wait-for-result (tcp-client bot-info)))
(def input-channel (map* parse-message output-channel))
(def auth-channel (filter* #(and (= "NOTICE" (:command %))
                                 (= "NickServ" (:nick %))
                                 (re-find #"STATUS rainbowsaurus ." (:trailing %)))
                           input-channel))
(def connection-channel (filter* #(= "001" (:command %))
                                 input-channel))
;; (def heart-channel (filter* #(and (re-find #"<3" (:trailing %))
;;                                   (= "PRIVMSG" (:command %)))
;;                             input-channel))

(def authenticated (atom false))

(defn join-channel [channel]
  (enqueue output-channel (map->message {:command "JOIN"
                                         :params (list channel)})))

(defn private-message [channel message]
  (enqueue output-channel (map->message {:command "PRIVMSG"
                                         :params (list channel)
                                         :trailing message})))

(defn quit [message]
  (enqueue output-channel (map->message {:command "QUIT"
                                         :trailing message})))

;; (defn heart-reply [{:keys [channel]}]
;;   (private-message channel "<3"))

(declare privmsg-dispatch)

(defn check-auth []
  (let [auth (read-channel auth-channel)]
    (private-message "NickServ" "status rainbowsaurus")
    ))

(defn add-points [{:keys [nick params bot-command-params] :as message}]
  (let [channel (first params)
        recipient (first bot-command-params)
        points (try (Integer. (second bot-command-params))
                    (catch Exception e nil))]
    (println (str "nick: " nick ", points: " points ", auth: " @authenticated
                  ", bot-command-params: " bot-command-params))
    (cond (not= nick "rainbowsaurus")
          (private-message channel "Only rainbowsaurus can give out rainbow points!")
          (not @authenticated)
          (do (receive auth-channel (partial check-auth message))
            (private-message "NickServ" "status rainbowsaurus"))
          (and recipient points @authenticated)
          (do (db/add-points! recipient points)
              (private-message channel (str "Added " points " for a total of "
                                            (db/points recipient)
                                            " rainbow points.")))
          :else
          (private-message channel "Usage: !addpts nick points"))))

(defn check-auth [command {:keys [trailing channel nick] :as message}]
  (if (= trailing "STATUS rainbowsaurus 3")
    (do (reset! authenticated true)
        (privmsg-dispatch command))
    (private-message channel (str "Nice try " nick "... imposter!"))))

(defn points [{:keys [params bot-command-params]}]
  (let [channel (first params)
        nick (first bot-command-params)]
    (cond (and nick (= nick "rainbowsaurus"))
          (private-message channel "Rainbowsaurus has much pointsies!")
          nick
          (private-message channel (str nick " has " (db/points nick)
                                        " rainbow points."))
          :else
          (private-message channel "Usage: !points nick"))))

(defn random-page [{:keys [params]}]
  (let [channel (first params)]
    (private-message channel (wiki/random-page))))

(def bot-command-list
  (atom {"points" points
         "pts" points
         "addpts" add-points
         "til" random-page}))

(defn privmsg-dispatch [message]
  ((get @bot-command-list (:bot-command message)) message))

(defmulti cmd-dispatch :command)

(defmethod cmd-dispatch "PING" [message]
  (enqueue output-channel (map->message (assoc message :command "PONG"))))

(defmethod cmd-dispatch "ERROR" [message]
  (close input-channel)
  (close output-channel)
  (System/exit 0))

(defmethod cmd-dispatch "QUIT" [message]
  (when (re-find #"rain.*" (:nick message))
    (reset! authenticated false)))

(defn bot-command? [message]
  (= \! (first (:trailing message))))

(defmethod cmd-dispatch "PRIVMSG" [message]
  (cond (bot-command? message)
        (privmsg-dispatch (parse-bot-command message))))

(defmethod cmd-dispatch :default [message]
  nil)

(defn on-connect [message]
  (join-channel "#rainbot"))

(defn -main [& args]
  (do (enqueue output-channel
               (str "NICK " (:nick bot-info))
               (str "USER " (:user bot-info) " 0 * : " (:user bot-info)))
      (receive-all input-channel cmd-dispatch)
      (receive-all input-channel println)
      (receive-all connection-channel on-connect)))
