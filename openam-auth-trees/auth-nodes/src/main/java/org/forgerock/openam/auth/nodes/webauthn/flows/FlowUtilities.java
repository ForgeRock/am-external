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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities useful for webauthn flows.
 */
public class FlowUtilities {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationFlow.class);

    private final IdentityUtils identityUtils;

    /**
     * Dependency Injection constructor.
     *
     * @param identityUtils the utility class for searching for oauth agents with.
     */
    @Inject
    public FlowUtilities(IdentityUtils identityUtils) {
        this.identityUtils = identityUtils;
    }

    /**
     * Compares two origins, matching on their scheme, host and port.
     *
     * @param realm the realm evaluation is taking place in.
     * @param amOriginStrs Set of (URL) Strings AM allows.
     * @param deviceOriginStr Second (URL) String to compare.
     * @return True if the scheme, host and port matches.
     */
    boolean isOriginValid(Realm realm, Set<String> amOriginStrs, String deviceOriginStr) {
        try {
            URL deviceOrigin = new URL(deviceOriginStr);

            for (String amOriginStr : amOriginStrs) {
                URL amOrigin = new URL(amOriginStr);

                boolean result = deviceOrigin.getProtocol().equals(amOrigin.getProtocol())
                        && deviceOrigin.getHost().equals(amOrigin.getHost())
                        && portsMatch(deviceOrigin, amOrigin);

                if (result) {
                    return true;
                }
            }

            return isOriginFromOAuthClient(realm, deviceOrigin);
        } catch (MalformedURLException e) {
            logger.error("Invalid error for origin verification, origin {}, validated against {}", deviceOriginStr,
                    amOriginStrs, e);
        }
        return false;
    }

    private boolean isOriginFromOAuthClient(Realm realm, URL origin) {
        return identityUtils.getOAuthClientForOrigin(realm, origin).isPresent();
    }

    private boolean portsMatch(URL url1, URL url2) {
        int port1 = url1.getPort();
        int port2 = url2.getPort();
        if (port1 == port2) {
            return true;
        }
        int defaultPort = url1.getDefaultPort();
        if (defaultPort == -1) {
            logger.error("No default port known for protocol {}", url1);
            return false;
        }
        return (port1 == -1 && port2 == defaultPort)  //one omitted, the other specified
            || (port1 == defaultPort && port2 == -1); //vice-versa
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
