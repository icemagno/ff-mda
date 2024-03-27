FROM openjdk:17.0.1-jdk-slim
LABEL maintainer "Carlos M. Abreu <magno.mabreu@gmail.com>"

RUN mkdir /scripts

COPY create-ca.sh /scripts

RUN chmod 777 /scripts/*.sh

COPY ./target/ffmda-0.1.jar /opt/lib/
ENTRYPOINT ["java"]
ENV LANG=pt_BR.utf8 
CMD ["-jar", "/opt/lib/ffmda-0.1.jar"]