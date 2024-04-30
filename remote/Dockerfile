FROM openjdk:17.0.1-jdk-slim
LABEL maintainer "Carlos M. Abreu <magno.mabreu@gmail.com>"

RUN apt update && apt install -y iputils-ping

COPY ./target/ffmda-remote-0.1.jar /opt/lib/
ENTRYPOINT ["java"]
ENV LANG=pt_BR.utf8 
CMD ["-jar", "/opt/lib/ffmda-remote-0.1.jar"]