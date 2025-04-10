FROM gcr.io/forgerock-io/ig/pit1:7.1.1

WORKDIR /home/forgerock
COPY generate-certificate.sh ca.jks ca.pem root.pem /home/forgerock/

ARG BACKEND_HOST

RUN /home/forgerock/generate-certificate.sh
RUN echo -n password > /home/forgerock/keystore.pass
ENV IG_KEYSTORE_DIRECTORY /home/forgerock


EXPOSE 8443

COPY --chown=forgerock:root . /var/ig
