#! /bin/sh


mvn clean package

docker network create ffmda

docker rmi magnoabreu/ffmda-master:0.1
docker build --tag=magnoabreu/ffmda-master:0.1 --rm=true .

docker stop ffmda-master && docker rm ffmda-master

docker run --name ffmda-master --network=ffmda --hostname=ffmda-master \
--restart=always \
-d -p 36780:8080 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /srv:/srv \
magnoabreu/ffmda-master:0.1

docker push magnoabreu/ffmda-master:0.1

