(ns flexibot.command
  (:require [flexibot.parse :refer [parse-bot-command]]))

(def commands (atom '()))

(defn def-command
  "Adds a command to the bot. The first argument is a predicate which
  takes a message map and determines whether the command will be run
  or not. The second argument is the command, which takes a message
  map."
  [p cmd]
  (swap! commands (partial cons [p cmd])))

(defn bot-command? [name message]
  (and
   (= \! (first (:trailing message)))
   (= name (:bot-command message))))

(defn def-bot-command
  "Adds a command which will execute with a message beginning with
  !commandname. The first argument is the name of the command (what
  comes after the !), and the second argument is the command, which
  takes a message map including the :bot-command
  and :bot-command-params keys."
  [name cmd]
  (swap! commands (partial cons [(comp (partial bot-command? name) parse-bot-command)
                                 (comp cmd parse-bot-command)])))

(defn react-to-message [message]
  (doseq [[p cmd] @commands]
    (when (p message) (cmd message))))
