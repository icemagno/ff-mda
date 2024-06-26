#! /bin/sh


docker run --rm -it \
-v ~/.m2:/root/.m2 \
-v ${PWD}:/home/builder \
magnoabreu/ffmda-builder:java17 mvn clean package

docker network create ffmda

mkdir /srv/ffmda

docker rmi magnoabreu/ffmda-agent:0.1
docker build --tag=magnoabreu/ffmda-agent:0.1 --rm=true .

docker stop ffmda-agent && docker rm ffmda-agent

docker run --name ffmda-agent --network=ffmda --hostname=ffmda-agent \
--restart=always \
-d -p 36780:8080 \
-e NODE_NAME=FF-N1 \
-e ORG_NAME="The New Org" \
-e HOST_NAME=firefly.s1 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /srv:/srv \
magnoabreu/ffmda-agent:0.1

# docker push magnoabreu/ffmda-agent:0.1

