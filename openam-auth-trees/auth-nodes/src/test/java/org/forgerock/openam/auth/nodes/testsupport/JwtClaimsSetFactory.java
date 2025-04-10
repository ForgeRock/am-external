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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.testsupport;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.utils.Time;

import java.net.URI;
import java.util.Date;

/**
 * Test factory for the creation of {@link JwtClaimsSet}s with given values.
 */
public class JwtClaimsSetFactory {
    public static final String ISSUER_CONFIG = "https://accounts.google.com";
    public static final String AUDIENCE_CONFIG = "audience.google";
    public static final String AUTHORIZED_PARTIES_CONFIG = "audience.google";

    // Minimal JwtClaims setup for testing
    public static JwtClaimsSetBuilder aJwtClaimsSet() {
        JwtClaimsSetBuilder.jwtClaimsSet = new JwtClaimsSet();
        return new JwtClaimsSetBuilder()
                .withIssuerInStringFormat(ISSUER_CONFIG)
                .withAudienceInStringFormat(AUDIENCE_CONFIG)
                .withAuthorisedParties(AUDIENCE_CONFIG)
                .withIssuedAtTimeInLongFormat(Time.getClock().instant().getEpochSecond() - 60)
                .withExpiryTimeInLongFormat(Time.getClock().instant().getEpochSecond() + 1000);
    }

    public static class JwtClaimsSetBuilder {
        static JwtClaimsSet jwtClaimsSet;

        public JwtClaimsSetBuilder withAuthorisedParties(String authorisedParties) {
            jwtClaimsSet.setClaim("azp", authorisedParties);
            return this;
        }

        public JwtClaimsSetBuilder withAudienceInStringFormat(String audience) {
            jwtClaimsSet.addAudience(audience);
            return this;
        }

        public JwtClaimsSetBuilder withAudienceInUriFormat(URI audience) {
            jwtClaimsSet.addAudience(audience);
            return this;
        }

        public JwtClaimsSetBuilder withIssuedAtTimeInLongFormat(long time) {
            jwtClaimsSet.setClaim("iat", time);
            return this;
        }

        public JwtClaimsSetBuilder withIssuedAtTimeInDateFormat(Date time) {
            jwtClaimsSet.setIssuedAtTime(time);
            return this;
        }

        public JwtClaimsSetBuilder withExpiryTimeInLongFormat(long time) {
            jwtClaimsSet.setClaim("exp", time);
            return this;
        }

        public JwtClaimsSetBuilder withExpiryTimeInDateFormat(Date time) {
            jwtClaimsSet.setExpirationTime(time);
            return this;
        }

        public JwtClaimsSetBuilder withIssuerInStringFormat(String issuer) {
            jwtClaimsSet.setIssuer(issuer);
            return this;
        }

        public JwtClaimsSetBuilder withIssuerInUriFormat(URI issuer) {
            jwtClaimsSet.setIssuer(issuer);
            return this;
        }

        public JwtClaimsSetBuilder withJwtId(String id) {
            jwtClaimsSet.setJwtId(id);
            return this;
        }

        public JwtClaimsSetBuilder withSubjectInStringFormat(String subject) {
            jwtClaimsSet.setSubject(subject);
            return this;
        }

        public JwtClaimsSetBuilder withSubjectInUriFormat(URI subject) {
            jwtClaimsSet.setSubject(subject);
            return this;
        }

        public JwtClaimsSetBuilder withNotBeforeTime(long time) {
            jwtClaimsSet.setClaim("nbf", time);
            return this;
        }

        public JwtClaimsSetBuilder withNotBeforeTimeInDateFormat(Date time) {
            jwtClaimsSet.setNotBeforeTime(time);
            return this;
        }

        public <T> JwtClaimsSetBuilder withCustomAttribute(String key, T value) {
            jwtClaimsSet.setClaim(key, value);
            return this;
        }

        public JwtClaimsSet build() {
            return jwtClaimsSet;
        }
    }
}
