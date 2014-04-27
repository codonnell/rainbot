(ns flexibot.test.db
  (:use flexibot.db
        clojure.test)
  (:require [datomic.api :as d :refer [db q]]))

(def ^:dynamic *conn* nil)

(defn db-setup-teardown-fixture [f]
  (let [uri (str "datomic:mem://" (d/squuid))]
    (binding [*conn* (init! uri)]
      (f)
      (d/release *conn*))
    (d/delete-database uri)))

(use-fixtures :each db-setup-teardown-fixture)

(deftest test-get-user
  (d/transact
   *conn*
   [{:db/id (d/tempid :db.part/user)
     :user/name "test-user"
     :user/points "0"}])
  (is (= (get (get-user *conn* "test-user") :user/name) "test-user"))
  (is (= (get (get-user *conn* "test-user") :user/points) "0")))

(deftest test-add-user!
  (add-user! *conn* "test-user")
  (is (not (empty? (q '[:find ?e :where [?e :user/name "test-user"]] (db *conn*))))))

(deftest test-get-or-add-user!
  (get-or-add-user! *conn* "test-user")
  (is (not (empty? (q '[:find ?e :where [?e :user/name "test-user"]] (db *conn*)))))
  (is (= (d/entity (db *conn*) (ffirst (q '[:find ?e :where [?e :user/name "test-user"]] (db *conn*))))
         (get-or-add-user! *conn* "test-user"))))

(deftest test-points
  (d/transact
   *conn*
   [{:db/id (d/tempid :db.part/user)
     :user/name "test-user"
     :user/points "0"}])
  (is (= "0" (points *conn* "test-user")))
  (is (nil? (points *conn* "dummy-user"))))

(deftest test-add-points!
  (d/transact
   *conn*
   [{:db/id (d/tempid :db.part/user)
     :user/name "test-user"
     :user/points "0"}])
  (add-points! *conn* "test-user" 1)
  (is (= "1" (points *conn* "test-user")))
  (add-points! *conn* "test-user" -1)
  (is (= "0" (points *conn* "test-user")))
  (add-points! *conn* "test-user" 0)
  (is (= "0" (points *conn* "test-user"))))
