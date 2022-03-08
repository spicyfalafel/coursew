(ns coursew.auth
  (:require [buddy.sign.jwt :as jwt]))

(defonce secret "86bae26023208e57a5880d5ad644143c567fc57baaf5a942")


(defn generate-signature [username password]
  (jwt/sign {:username username :password password} secret))

(defn unsign-token [token]
  (jwt/unsign token secret))

(unsign-token (generate-signature "1" "1"))
