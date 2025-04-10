#!/bin/bash
#
# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions Copyright [year] [name of copyright owner]".
#
# Copyright 2024-2025 Ping Identity Corporation.
#

# To run against an idcloud instance just pass the host name in as an argument to the script without any flags
# example => ./proxy_run.sh -h openam-my-sandbox.forgeblocks.com

while getopts 'h:' flag; do
  case "${flag}" in
    h) BACKEND_HOST=$OPTARG ;;
  esac
done

export BACKEND_HOST_IP="${BACKEND_HOST_IP:-`host $BACKEND_HOST 8.8.8.8 | awk '/has address/ { print $4 }'`}"
export FRONTEND_HOST=host.docker.internal

# Update hosts file to have BACKEND_HOST mapped to 127.0.0.1
if [ -n "$(grep $BACKEND_HOST /etc/hosts)" ]; then
  echo "$BACKEND_HOST found in /etc/hosts ... not adding"
else
  echo "Adding entry for ${BACKEND_HOST} to /etc/hosts"
  printf "# Added by proxy_run.sh \n127.0.0.1 ${BACKEND_HOST}\n" | sudo tee -a /etc/hosts > /dev/null
fi

if [ ! -f "ig-front/ca.pem" ]; then
    (cd ig-front && ./generate-certificate-chain.sh)
fi

rm -f /tmp/.env.tmpl

sudo docker build --build-arg BACKEND_HOST=$BACKEND_HOST -t ig-front:latest ig-front

if [ "$(uname)" == "Linux" ]; then
    PLATFORM_SPECIFIC_ARGS="--add-host=host.docker.internal:host-gateway"
else
    PLATFORM_SPECIFIC_ARGS=""
fi

sudo docker run -it -p 443:8443 --add-host $BACKEND_HOST:$BACKEND_HOST_IP -e FRONTEND_HOST=$FRONTEND_HOST -e BACKEND_HOST=$BACKEND_HOST $PLATFORM_SPECIFIC_ARGS ig-front:latest
