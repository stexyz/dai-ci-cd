FROM jenkins/jenkins:lts
COPY ./h2oai_client-1.4.2-py3-none-any.whl /tmp
COPY plugins.txt /usr/share/jenkins/ref/extra_plugins.txt

USER root

RUN	apt update --yes
RUN	apt install --yes python3
RUN	apt install --yes python3-pip
RUN yes | pip3 install /tmp/h2oai_client-1.4.2-py3-none-any.whl
RUN	yes | pip3 install boto3

RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/extra_plugins.txt