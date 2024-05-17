FROM openjdk:17.0.1-jdk-slim
LABEL maintainer "Carlos M. Abreu <magno.mabreu@gmail.com>"

RUN apt update && apt install -y maven && apt upgrade -y

RUN rm -rf /var/cache/apt/archives /var/lib/apt/lists/*

RUN mkdir /home/builder

WORKDIR /home/builder