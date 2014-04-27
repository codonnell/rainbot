(ns flexibot.test.parse
  (:use clojure.test
        flexibot.parse))

(deftest test-parse-prefix
  (is (= {:nick "CalebDelnay" :user "calebd" :host "localhost"}
         (parse-prefix "CalebDelnay!calebd@localhost")))
  (is (nil? (parse-prefix nil))))

(deftest test-parse-message
  (is (= {:trailing "Hello everyone!"
          :params '("#mychannel")
          :command "PRIVMSG"
          :host "localhost"
          :user "calebd"
          :nick "CalebDelnay"}
         (parse-message ":CalebDelnay!calebd@localhost PRIVMSG #mychannel :Hello everyone!")))
  (is (= {:command "QUIT"
          :trailing "Bye Bye!"
          :params nil
          :nick "CalebDelnay"
          :user "calebd"
          :host "localhost"}))
  (is (= {:command "JOIN"
          :trailing nil
          :params '("#mychannel")
          :nick "CalebDelnay"
          :user "calebd"
          :host "localhost"}
         (parse-message ":CalebDelnay!calebd@localhost JOIN #mychannel")))
  (is (= {:command "PING"
          :trailing "irc.localhost.localdomain"
          :params nil})))

(deftest test-map->message
  (is (= "PONG :irc.localhost.localdomain")
      (map->message {:command "PONG" :trailing "irc.localhost.localdomain"}))
  (is (= "JOIN #mychannel")
      (map->message {:command "JOIN" :params '("#mychannel")}))
  (is (= "MODE #mychannel -l")
      (map->message {:command "MODE" :params '("#mychannel" "-l")})))

(deftest test-parse-bot-command
  (is (= {:bot-command "add-points"
          :bot-command-params '("caracal" "1")
          :trailing "!add-points caracal 1"}
         (parse-bot-command {:trailing "!add-points caracal 1"})))
  (is (= {:bot-command "points"
          :bot-command-params '("caracal")
          :trailing "!points caracal"}
         (parse-bot-command {:trailing "!points caracal"})))
  (is (nil? (parse-bot-command nil))))
