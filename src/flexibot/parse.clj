(ns flexibot.parse
  "Parses IRC messages. Takes a lot of inspiration from irclj.parse."
  (require [clojure.string :refer [split join]]))

(defn split-on-prefix [line]
  "Returns a tuple (prefix rest-of-message) for the passed in IRC
  message."
  (let [words (split line #" ")]
    (if (= \: (first line))
      (list (apply str (rest (first words))) (join " " (rest words)))
      (list nil line))))

(defn parse-prefix [prefix]
  "Returns a map with keys :host, :user, and :nick corresponding to
  the prefix of the passed in message."
  (when prefix
    (zipmap [:nick :user :host] (split prefix #"@|!"))))

(defn split-on-command [line]
  "Returns a tuple (command-and-params rest-of-message) for the passed
  in IRC message."
  (split line #":" 2))

(defn parse-command [command]
  "Returns a map with keys :command and :params from a command string."
  (let [words (split command #"\s")]
    {:command (first words) :params (rest words)}))

(defn parse-message [line]
  "Parses an IRC message and Returns a map with the potential to have
  keys :host, :user, :nick, :command, :params, :trailing, and
  :raw. Some of these may be missing or nil, depending on the message."
  (let [[prefix rem] (split-on-prefix line)
        [command trailing] (split-on-command rem)]
    (merge (parse-prefix prefix)
           (parse-command command)
           {:trailing trailing})))

(defn map->message [{:keys [command params trailing]}]
  "Takes a map with an IRC command (eg. JOIN or PRIVMSG), its
  parameters, like the channel where a PRIVMSG was sent, and any
  trailing information, like the text of a PRIVMSG. Returns a properly
  formatted string to be sent to an IRC server."
  (str command
       (if params " " "") (join " " params)
       (if trailing " :" "") trailing))

(defn parse-bot-command [message]
  "Takes a message map with a bot command in :trailing (like !points
  ircnick) and returns the same map, but with the new key
  :bot-command, which is the command without the ! in front, and
  :bot-command-params, which contains a list of its parameters."
  (when-let [words (try (split (:trailing message) #" ")
                        (catch Exception e nil))]
    (assoc message
      :bot-command (apply str (rest (first words)))
      :bot-command-params (rest words))))
