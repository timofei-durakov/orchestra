FROM       ubuntu:15.10
MAINTAINER T. Durakov <tdurakov@mirantis.com>
RUN apt-get install apt-transport-https
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
RUN apt-get update && apt-get install -y sbt \
git
RUN mkdir /orchestra
RUN git clone https://github.com/timofei-durakov/orchestra.git /orchestra
WORKDIR /orchestra
VOLUME /scenarios
RUN sbt "run test.yaml"

