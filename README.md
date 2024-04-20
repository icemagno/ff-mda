# ff-mda
Multiparty Deployer Agent for FireFly

```
docker run --name ffmda-master --network=ffmda --hostname=ffmda-master \
--restart=always \
-d -p 36780:8080 \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /srv:/srv \
magnoabreu/ffmda-master:0.1
```
optional:

```-v /srv/ffmda/manifest.json:/srv/manifest.json```
