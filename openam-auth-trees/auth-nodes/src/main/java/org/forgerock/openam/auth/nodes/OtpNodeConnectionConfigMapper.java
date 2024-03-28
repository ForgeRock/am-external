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
 * Copyright 2023-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singleton;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * Base configuration for the authentication tree nodes using SMTP.
 */
public class OtpNodeConnectionConfigMapper {

    private static final Logger logger = LoggerFactory.getLogger(OtpNodeConnectionConfigMapper.class);
    private static final String SMTPHOSTNAME = "sunAMAuthHOTPSMTPHostName";
    private static final String SMTPHOSTPORT = "sunAMAuthHOTPSMTPHostPort";
    private static final String SMTPUSERNAME = "sunAMAuthHOTPSMTPUserName";
    private static final String SMTPUSERPASSWORD = "sunAMAuthHOTPSMTPUserPassword";
    private static final String SMTPSSLENABLED = "sunAMAuthHOTPSMTPSSLEnabled";

    private final SecretReferenceCache secretReferenceCache;

    /**
     * Constructor for the OTP node connection config mapper.
     *
     * @param secretReferenceCache the secret reference cache
     */
    @Inject
    public OtpNodeConnectionConfigMapper(SecretReferenceCache secretReferenceCache) {
        this.secretReferenceCache = secretReferenceCache;
    }

    /**
     * Returns the SMTPBaseConfig values as a config map.
     * @param config the OTP node configuration
     * @param realm the realm
     *
     * @return the values in this config.
     */
    public Map<String, Set<String>> asConfigMap(OtpNodeBaseConfig config, Realm realm) {
        String password = config.passwordPurpose()
                .map(passwordPurpose -> secretReferenceCache.realm(realm).active(passwordPurpose))
                .map(SecretReference::getAsync)
                .map(promise -> promise.then(s -> s.revealAsUtf8(String::new)))
                .map(promise -> promise.thenCatch(ex -> {
                    logger.debug("Failed to get secret from store ", ex);
                    return null;
                }))
                .map(Promise::getOrThrowIfInterrupted)
                .or(() -> config.password().map(String::new))
                .orElse(null);
        return new ImmutableMap.Builder<String, Set<String>>()
                .put(SMTPHOSTNAME, singleton(config.hostName()))
                .put(SMTPHOSTPORT, singleton(String.valueOf(config.hostPort())))
                .put(SMTPUSERNAME, singleton(config.username()))
                .put(SMTPSSLENABLED, singleton(config.sslOption().option))
                .put(SMTPUSERPASSWORD, singleton(password))
                .build();
    }

}
