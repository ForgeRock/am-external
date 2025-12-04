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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.amster;

import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.openam.utils.IPRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;

/**
 * Representation of a key that is authorized to access OpenAM via Amster.
 */
abstract class Key {
    private final Logger debug = LoggerFactory.getLogger(Key.class);
    private final Map<String, String> options;

    /**
     * Initialise the key.
     *
     * @param options Key options - the value should be compatible with the SSH options syntax, but also supports a
     *                {@code subject} option to restrict the subject that is valid - comma separated string, where
     *                {@code *} indicates any subject is valid.
     * @see <a href="https://www.freebsd.org/cgi/man.cgi?query=sshd&sektion=8#AUTHORIZED_KEYS_FILE_FORMAT">SSH options
     * syntax.</a>
     */
    Key(Map<String, String> options) {
        this.options = options;
    }

    /**
     * Check the subject is valid, that the origin of the request is valid, and the JWT signature is verified by the
     * encapsulated key.
     *
     * @param jwt The JWT to check.
     * @param request
     * @return {@code true} if the JWT signature is verified and the subject is ok.
     */
    boolean isValid(SignedJwt jwt, HttpServletRequest request) {
        return isValidSubject(jwt) && isValidOrigin(request) && isSignatureValid(jwt);
    }

    private boolean isValidSubject(SignedJwt jwt) {
        if (!options.containsKey("subject")) {
            return true;
        }
        String jwtSubject = jwt.getClaimsSet().getSubject();
        // split by commas that are not preceded by '\\' escaping
        for (String subject : options.get("subject").split("(?<!\\\\),")) {
            if (subject.equals(jwtSubject) || subject.equals("*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verify the JWT signature is verified by the encapsulated key.
     *
     * @param jwt The JWT to check.
     * @return {@code true} if the JWT signature is verified.
     */
    abstract boolean isSignatureValid(SignedJwt jwt);

    /**
     * Check that the key is valid for use with the provided request.
     *
     * @param request The request.
     * @return {@code true} if the request is valid.
     */
    @VisibleForTesting
    boolean isValidOrigin(HttpServletRequest request) {
        if (!options.containsKey("from")) {
            return true;
        }
        boolean denied = false;
        boolean accepted = false;
        String address = ClientUtils.getClientIPAddress(request);

        // https://www.freebsd.org/cgi/man.cgi?query=sshd&sektion=8#AUTHORIZED_KEYS_FILE_FORMAT
        for (String from : options.get("from").split(",")) {
            if (from.matches("[^0-9:./]")) {
                debug.debug("Key#isValidOrigin: From option for hostnames not supported: {}", from);
                continue;
            }
            boolean matched = false;
            boolean negated = from.startsWith("!");
            if (negated) {
                from = from.substring(1);
            }
            if (from.contains("/")) {
                if (cidrMatches(address, from)) {
                    matched = true;
                }
            } else {
                // https://www.freebsd.org/cgi/man.cgi?query=ssh_config&sektion=5#PATTERNS
                Pattern fromPattern = Pattern.compile("^"
                        + from.replace(".", "\\.").replace("?", ".").replace("*", ".*") + "$");
                if (fromPattern.matcher(address).matches()) {
                    matched = true;
                }
            }
            if (matched && negated) {
                denied = true;
            } else if (matched) {
                accepted = true;
            }
        }

        return !denied && accepted;
    }

    private boolean cidrMatches(String address, String from) {
        if (from.contains(":") && address.contains(":")) {
            debug.debug("Key#cidrMatches: checking IPv6 address {} is in range {}", address, from);
            IPv6Network ipv6Network = IPv6Network.fromString(from);
            return ipv6Network.contains(IPv6Address.fromString(address));
        } else if (!from.contains(":") && !address.contains(":")) {
            debug.debug("Key#cidrMatches: checking IPv4 address {} is in range {}", address, from);
            return new IPRange(from).inRange(address);
        }
        return false;
    }
}
