FROM clojure:lein

RUN apt-get update && apt-get install -y curl
#EXPOSE 8080
RUN mkdir -p /usr/src/coursew
WORKDIR /usr/src/coursew
COPY project.clj /usr/src/coursew/
RUN lein deps
COPY . /usr/src/coursew
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar
CMD ["java", "-jar", "app-standalone.jar"]


# COPY . /user/src/coursew
# WORKDIR /user/src/coursew
# CMD ["lein", "run"]
