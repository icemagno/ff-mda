# FF-MDA
Multiparty Deployer Agent for FireFly

I'm not finished yet. Be patient!

![local-node](https://github.com/icemagno/ff-mda/assets/4127512/b543234e-7fe4-4bd3-ab3a-8c4f10b1aa29)

![dx](https://github.com/icemagno/ff-mda/assets/4127512/8f318e6c-795e-44c6-bafb-4a8a0b3b978e)

![postgresql](https://github.com/icemagno/ff-mda/assets/4127512/5516c72a-4975-422e-b115-7eea94932d08)

![graph](https://github.com/icemagno/ff-mda/assets/4127512/15d94996-5b9a-4e15-b49b-14bdc4aca924)

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
