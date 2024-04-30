```
docker run --name ffmda-agent --network=ffmda --hostname=ffmda-agent \
--restart=always \
-d -p 36780:8080 \
-e NODE_NAME=FF-N1 \
-e ORG_NAME="The New Org" \
-e HOST_NAME=firefly.s1 \
-e HOST_ADDRESS=192.168.0.205 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /srv:/srv \
magnoabreu/ffmda-agent:0.1
```
