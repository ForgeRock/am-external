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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.edge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.openam.authentication.modules.jwtpop.ConfirmationKeyType;
import org.forgerock.openam.authentication.modules.jwtpop.JwtProofOfPossession;
import org.forgerock.openam.authentication.modules.jwtpop.ResponseEncryptionStrategy;
import org.forgerock.openam.utils.AMKeyProvider;

/**
 * Authentication of edge devices via Jwt Proof of Possession.
 */
public class EdgeAuthentication extends JwtProofOfPossession {

    public EdgeAuthentication() {
        // Only allow previously registered keys to be used for authentication
        super(InjectorHolder.getInstance(AMKeyProvider.class), ConfirmationKeyType.KID);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Subject subject, Map sharedState, Map options) {
        final Map<String, Set<String>> popOptions = new HashMap<>(options);
        popOptions.put(RESPONSE_ENCRYPTION_METHOD_ATTR,
                Collections.singleton(ResponseEncryptionStrategy.PSK.name()));
        popOptions.put(RESPONSE_ENCRYPTION_CIPHER_ATTR,
                Collections.singleton(EncryptionMethod.XC20_P1305.getJweStandardName()));
        popOptions.put(TLS_SESSION_BINDING_ATTR, Collections.singleton("true"));
        popOptions.put(AUTH_LEVEL_ATTR, (Set<String>) options.get("forgerock-am-auth-edgeauthentication-auth-level"));

        super.init(subject, sharedState, popOptions);
    }
}
