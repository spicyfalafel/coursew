FROM timbru31/java-node
#EXPOSE 8080
RUN apt-get update && apt-get install -y tree
RUN mkdir -p /usr/mibui
WORKDIR /usr/mibui
COPY package.json .
COPY package-lock.json .
COPY resources ./
COPY shadow-cljs.edn .
#COPY karma.conf.js .
COPY src ./src

RUN tree -L 3

RUN npm ci && npm cache clean --force && npm install && npx shadow-cljs release app
RUN npx shadow-cljs start
#CMD tail -f /dev/null
#    СMD ["npx", "shadow-cljs", "start"]
