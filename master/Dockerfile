FROM openjdk:11-jdk-slim
LABEL maintainer "Carlos M. Abreu <magno.mabreu@gmail.com>"

COPY ./target/ffmda-0.1.jar /opt/lib/
ENTRYPOINT ["java"]
ENV LANG=pt_BR.utf8 
CMD ["-jar", "/opt/lib/ffmda-0.1.jar"]