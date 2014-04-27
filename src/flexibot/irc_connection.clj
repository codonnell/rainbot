(ns flexibot.irc-connection
  (:require [gloss.core :as gloss]
            [aleph.tcp :refer [tcp-client]]
            [carica.core :refer [config]]
            [flexibot.command :refer [react-to-message]]
            [flexibot.parse :refer [parse-message map->message parse-bot-command]])
  (:use lamina.core))

(def bot-info {:host "irc.utonet.org"
               :nick (config :nick)
               :user "rainbot"
               :port 6667
               :frame (gloss/string :utf-8 :delimiters ["\r\n"])})

(def output-channel (wait-for-result (tcp-client bot-info)))
(def input-channel (map* parse-message output-channel))
(def auth-channel (channel))


(defn join-channel [channel]
  (enqueue output-channel (map->message {:command "JOIN"
                                         :params (list channel)})))

(defn private-message [channel message]
  (enqueue output-channel (map->message {:command "PRIVMSG"
                                         :params (list channel)
                                         :trailing message})))
(defn action [channel message]
  (private-message channel (str "\001ACTION " message "\001")))


(defn quit [message]
  (enqueue output-channel (map->message {:command "QUIT"
                                         :trailing message})))

(defmulti cmd-dispatch :command)

(defmethod cmd-dispatch "PING" [message]
  (enqueue output-channel (map->message (assoc message :command "PONG"))))

(defmethod cmd-dispatch "ERROR" [message]
  (close input-channel)
  (close output-channel)
  (System/exit 0))

(defmethod cmd-dispatch "PRIVMSG" [message]
  (react-to-message message))

(defmethod cmd-dispatch "001" [message]
  (join-channel (config :channel)))

(defmethod cmd-dispatch "NOTICE" [message]
  (when (and (= (:nick message) "NickServ")
             (re-find #"STATUS .*" (:trailing message)))
    (enqueue auth-channel message)))

(defmethod cmd-dispatch :default [message]
  nil)

(defn connect []
  (enqueue output-channel
               (str "NICK " (:nick bot-info))
               (str "USER " (:user bot-info) " 0 * : " (:user bot-info)))
  (receive-all input-channel cmd-dispatch)
  (receive-all input-channel println))
