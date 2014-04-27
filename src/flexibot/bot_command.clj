(ns flexibot.bot-command
  (:require [flexibot.command :refer [def-command def-bot-command]]
            [flexibot.wiki :as wiki]
            [flexibot.psql-db :as db])
  (:use flexibot.irc-connection
        lamina.core))

(def-command
  (fn [msg] (re-find #"<3" (:trailing msg)))
  (fn [msg] (private-message (first (:params msg)) "<3")))


(defn check-auth [nick]
  (let [auth (read-channel auth-channel)]
    (private-message "NickServ" (str "status " nick))
    (let [[_ ns-nick ns-num] (read auth-channel)]
      (and (= ns-nick nick) (= ns-num "3")))))

(defn add-points [{:keys [nick params bot-command-params] :as message}]
  (let [channel (first params)
        recipient (first bot-command-params)
        points (try (Integer. (second bot-command-params))
                    (catch Exception e nil))]
    (println (str "nick: " nick ", points: " points
                  ", bot-command-params: " bot-command-params))
    (cond (not= nick "rainbowsaurus")
          (private-message channel "Only rainbowsaurus can give out rainbow points!")
          ;;(not @authenticated)
          ;; (do (receive auth-channel (partial check-auth message))
          ;;     (private-message "NickServ" "status rainbowsaurus"))
          
          (and recipient points (check-auth nick))
          (do (db/add-points! recipient points)
              (private-message channel (str "Added " points " for a total of "
                                            (db/points recipient)
                                            " rainbow points.")))
          :else
          (private-message channel "Usage: !addpts nick points"))))

;; (defn check-auth [command {:keys [trailing channel nick] :as message}]
;;   (if (= trailing "STATUS rainbowsaurus 3")
;;     (do (reset! authenticated true)
;;         (privmsg-dispatch command))
;;     (private-message channel (str "Nice try " nick "... imposter!"))))

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

(defn hug [{:keys [bot-command-params params]}]
  (let [hugged (first bot-command-params)
        channel (first params)]
    (if (= hugged "all")
      (action channel "hugs everyone!")
      (action channel (str "hugs " hugged)))))

(def-bot-command "points" points)
(def-bot-command "pts" points)
(def-bot-command "addpts" add-points)
(def-bot-command "til..." random-page)
(def-bot-command "hug" hug)

(def bot-command-list
  (atom {"points" points
         "pts" points
         "addpts" add-points
         "til..." random-page
         "hug" hug}))
