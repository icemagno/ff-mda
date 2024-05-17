#! /bin/sh

docker run --rm \
-it \
-v ~/.m2:/root/.m2 \
-v ${PWD}:/home/builder \
magnoabreu/ffmda-builder:java17 /bin/bash


