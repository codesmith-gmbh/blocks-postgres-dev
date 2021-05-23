(ns codesmith.blocks.postgres.dev
  (:require [codesmith.blocks :as cb]
            [codesmith.blocks.postgres :as cbp]
            [integrant.core :as ig]
            [next.jdbc.connection :as conn])
  (:import [com.zaxxer.hikari HikariDataSource]
           [io.zonky.test.db.postgres.embedded EmbeddedPostgres]))

(defmethod cb/typed-block-transform
  [::cb/postgres :embedded-dev]
  [block-key system+profile ig-config]
  (cb/assoc-if-absent ig-config
                      ::embedded-dev
                      {}))

(defmethod ig/init-key ::embedded-dev
  [_ _]
  (let [^EmbeddedPostgres embedded-postgres (EmbeddedPostgres/start)
        ^HikariDataSource ds                (conn/->pool HikariDataSource {:jdbcUrl  (.getJdbcUrl embedded-postgres "postgres" "postgres")
                                                                           :password "postgres"})]
    (cbp/migrate-db! ds)
    {:datasource        ds
     :embedded-postgres embedded-postgres}))

(defmethod ig/resolve-key ::embedded-dev
  [_ value]
  (:datasource value))

(defmethod ig/halt-key! ::embedded-dev
  [_ {:keys [datasource embedded-postgres]}]
  (.close datasource)
  (.close embedded-postgres))

(derive ::embedded-dev ::cb/postgres)
