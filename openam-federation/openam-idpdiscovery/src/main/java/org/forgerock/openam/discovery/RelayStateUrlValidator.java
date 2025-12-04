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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.discovery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sun.identity.saml2.idpdiscovery.IDPDiscoveryConstants;
import com.sun.identity.saml2.idpdiscovery.SystemProperties;

/**
 * Validates redirect URLs against a pre-configured list of valid, wildcard (*) allowing URL patterns.
 */
public class RelayStateUrlValidator implements ValidDomainExtractor<RedirectUrlValidator.GlobalService> {

    private static final Logger DEBUG = LoggerFactory.getLogger(RelayStateUrlValidator.class);

    private static final Set<String> VALID_PATTERNS = ConcurrentHashMap.newKeySet();

    @Override
    public Collection<String> extractValidDomains(RedirectUrlValidator.GlobalService configInfo) {
        if (VALID_PATTERNS.size() == 0) {
            synchronized (VALID_PATTERNS) {
                String domains = SystemProperties.get(IDPDiscoveryConstants.VALID_REDIRECTS);
                Set<String> patterns = Arrays.stream(domains.split(" ")).map(s -> {
                    try {
                        return ESAPI.encoder().decodeFromURL(s);
                    } catch (EncodingException e) {
                        DEBUG.error("Encoding exception decoding supplied URL patterns.", e);
                    }

                    return "";
                }).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
                VALID_PATTERNS.addAll(patterns);
            }
        }

        return VALID_PATTERNS;
    }
}
