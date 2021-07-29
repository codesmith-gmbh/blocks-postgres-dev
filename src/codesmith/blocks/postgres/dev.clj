(ns codesmith.blocks.postgres.dev
  (:require [codesmith.blocks :as cb]
            [codesmith.blocks.postgres :as cbp]
            [integrant.core :as ig]
            [next.jdbc.connection :as conn])
  (:import [com.zaxxer.hikari HikariDataSource]
           [io.zonky.test.db.postgres.embedded EmbeddedPostgres]))

(defmethod cb/typed-block-transform
  [::cbp/postgres :embedded-dev]
  [block-key system+profile ig-config final-subsitution]
  [(cb/assoc-if-absent ig-config
                       ::embedded-dev
                       (-> system+profile block-key))
   final-subsitution])

(defmethod ig/init-key ::embedded-dev
  [_ {:keys [port]}]
  (let [builder                             (EmbeddedPostgres/builder)
        builder                             (if port (.setPort builder port) builder)
        ^EmbeddedPostgres embedded-postgres (.start builder)
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

(derive ::embedded-dev ::cbp/postgres)
