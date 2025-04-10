#!/bin/sh
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
echo  "===================================================================="
echo  "Now generating /home/forgerock/IG-keystore ..."
echo  "===================================================================="

# generate private keys (for server)

keytool -genkeypair -alias https-connector-key -dname "CN=$BACKEND_HOST,O=Example Corp,C=FR"  -validity 10000 -keyalg RSA -keysize 2048 -keystore /home/forgerock/IG-keystore -keypass password -storepass password

# generate a certificate for server signed by ca (root -> ca -> server)

keytool -keystore /home/forgerock/IG-keystore -storepass password -certreq -alias https-connector-key \
| keytool -keystore ca.jks -storepass password -gencert -alias ca -ext ku:c=dig,keyEnc -ext "san=dns:$BACKEND_HOST" -ext eku=sa,ca -rfc > server.pem

# import server cert chain into /home/forgerock/IG-keystore

keytool -keystore /home/forgerock/IG-keystore -storepass password -importcert -trustcacerts -noprompt -alias root -file root.pem
keytool -keystore /home/forgerock/IG-keystore -storepass password -importcert -alias ca -file ca.pem
keytool -keystore /home/forgerock/IG-keystore -storepass password -importcert -alias https-connector-key -file server.pem
