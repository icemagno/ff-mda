#! /bin/sh


mvn clean package

docker network create ffmda

mkdir /srv/ffmda
cp manifest.json /srv/ffmda

docker rmi magnoabreu/ffmda-master:0.1
docker build --tag=magnoabreu/ffmda-master:0.1 --rm=true .

docker stop ffmda-master && docker rm ffmda-master

# For IPFS
sysctl -w net.core.rmem_max=2500000
sysctl -w net.core.wmem_max=2500000


docker run --name ffmda-master --network=ffmda --hostname=ffmda-master \
--restart=always \
-d -p 36780:8080 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /srv:/srv \
magnoabreu/ffmda-master:0.1

# docker push magnoabreu/ffmda-master:0.1

