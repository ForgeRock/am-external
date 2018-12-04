/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;

/**
 * Utilities useful for webauthn flows.
 */
public class FlowUtilities {

    private final int defaultHttpsPort = 443;

    /**
     * Compares two origins, matching on their scheme, host and port.
     *
     * @param amOriginStr First (URL) String to compare.
     * @param deviceOriginStr Second (URL) String to compare.
     * @return True if the scheme, host and port matches.
     */
    boolean originsMatch(String amOriginStr, String deviceOriginStr) {
        try {
            URL deviceOrigin = new URL(deviceOriginStr);
            URL amOrigin = new URL(amOriginStr);

            return deviceOrigin.getProtocol().equals(amOrigin.getProtocol())
                    && deviceOrigin.getHost().equals(amOrigin.getHost())
                    && portsMatch(deviceOrigin.getPort(), amOrigin.getPort());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private boolean portsMatch(int port, int port1) {
        return (port == port1) //both ports set, or both omitted
            || (port == -1 && port1 == defaultHttpsPort)  //one omitted, the other specified
            || (port == defaultHttpsPort && port1 == -1); //vice-versa
    }

    /**
     * Retrieve a public key of the appropriate type from its JWK.
     *
     * @param keyData The JWK containing the public key.
     * @return an implementation of {@link PublicKey}, or null if no type was found.
     */
    public PublicKey getPublicKeyFromJWK(JWK keyData) {

        switch (keyData.getKeyType()) {
        case EC:
            return EcJWK.parse(keyData.toJsonValue()).toECPublicKey();
        case RSA:
            return RsaJWK.parse(keyData.toJsonValue()).toRSAPublicKey();
        default:
            break;
        }

        return null;
    }

}
