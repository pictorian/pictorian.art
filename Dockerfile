FROM clojure:openjdk-17-tools-deps AS clojure-deps
WORKDIR /app
COPY deps.edn deps.edn
COPY src-build src-build
RUN clojure -M -e :ok        # preload deps
RUN clojure -T:build noop           # preload build deps

FROM node:18-alpine AS node-deps
WORKDIR /app
COPY package.json package.json
#_COPY yarn.lock yarn.lock
COPY src src
COPY resources resources
COPY src-build src-build
RUN yarn install
RUN src-build/build-css.sh

FROM clojure:openjdk-17-tools-deps AS build
RUN  apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=node-deps /app/node_modules /app/node_modules
COPY --from=node-deps /app/resources /app/resources
COPY --from=clojure-deps /root/.m2 /root/.m2
COPY shadow-cljs.edn shadow-cljs.edn
COPY deps.edn deps.edn
COPY bb.edn bb.edn
COPY src src
COPY env env
#_COPY resources resources
COPY src-build src-build
COPY local-jars local-jars
RUN curl -sLO https://raw.githubusercontent.com/babashka/babashka/v1.3.180/install
RUN chmod +x install
RUN ./install
ARG REBUILD=unknown
RUN bb uberjar

FROM amazoncorretto:17 AS app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
# not required - included for Directory Explorer demo
#_COPY --from=node-deps /app/node_modules node_modules
EXPOSE 3000
CMD java -jar app.jar
