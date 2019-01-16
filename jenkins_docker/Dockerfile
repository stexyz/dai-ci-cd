FROM jenkins/jenkins:lts
COPY ./h2oai_client-1.4.2-py3-none-any.whl /tmp

USER root

RUN	apt update --yes
RUN	apt install --yes python3
RUN	apt install --yes python3-pip
RUN yes | pip3 install /tmp/h2oai_client-1.4.2-py3-none-any.whl
RUN	yes | pip3 install boto3