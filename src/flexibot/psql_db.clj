(ns flexibot.psql-db
  (:use [korma core db]
        [carica.core]))

(defdb db (postgres (config :db)))

(defentity users
  (pk :username)
  (table :users)
  (database db)
  (entity-fields :nick :points))

(defn has-user [nick]
  "Checks to see if a user with the given nick has an entry in the database."
  (not (empty? (select users (where {:nick [= nick]})))))

(defn add-user! [nick]
  "Adds a user to the database with the given nick and 0 points when a
  user with that nick does not already exist."
  (transaction
   (when-not (has-user nick)
     (insert users (values {:nick nick :points 0})))))

(defn points [nick]
  "Gets the number of points for a given user."
  (get (first (select users (where {:nick [= nick]})))
       :points 0))

(defn add-points! [nick n]
  "Adds n points to the given nick. If that nick doesn't exist in the
  database, an entry will be created for it."
  (transaction
   (add-user! nick) ; Adds the user to the database if they're not
                    ; already in it
   (let [p (points nick)]
     (update users (set-fields {:points (+ p n)}) (where {:nick [= nick]})))))
