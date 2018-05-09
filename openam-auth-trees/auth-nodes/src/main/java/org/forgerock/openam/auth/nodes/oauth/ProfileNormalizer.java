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

package org.forgerock.openam.auth.nodes.oauth;


import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;

import com.google.inject.Singleton;
import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * User profile normaliser. It uses configured Account Mapper and Attribute Mappers to normalise the user attributes.
 */
@Singleton
public class ProfileNormalizer {

    private final Map<String, AccountProvider> accountProvider = new HashMap<>();
    private final Map<String, AttributeMapper> accountMapper = new HashMap<>();

    /**
     * Returns the AccountProvider instance.
     * @param config the config
     * @return The AccountProvider instance.
     * @throws AuthLoginException when error occurs while instantiating the provider class.
     */
    public AccountProvider getAccountProvider(AbstractSocialAuthLoginNode.Config config) throws AuthLoginException {
        String providerClass = config.cfgAccountProviderClass();
        synchronized (accountProvider) {
            if (!accountProvider.containsKey(providerClass)) {
                accountProvider.put(providerClass, OAuthUtil.instantiateAccountProvider(providerClass));
            }
        }
        return accountProvider.get(providerClass);
    }

    /**
     * Returns the AttributeMapper instance.
     * @param config the config
     * @return the AttributeMapper instance
     * @throws AuthLoginException when error occurs while instantiating the mapper class.
     */
    public AttributeMapper getAccountMapper(AbstractSocialAuthLoginNode.Config config) throws AuthLoginException {
        String accountMapperClass = config.cfgAccountMapperClass();
        synchronized (accountMapper) {
            if (!accountMapper.containsKey(accountMapperClass)) {
                accountMapper.put(accountMapperClass, OAuthUtil.instantiateAccountMapper(accountMapperClass));
            }
        }
        return accountMapper.get(accountMapperClass);
    }


    /**
     * Gets normalized attributes for the Account lookup. It internally uses Account Mapper configurations.
     *
     * @param userInfo The user attributes from the social provider.
     * @param jwtClaimsSet The id token claim from the social provider.
     * @param config the config
     * @return The normalized attributes.
     * @throws AuthLoginException when attributes can't be mapped.
     */
    public Map<String, Set<String>> getNormalisedAccountAttributes(UserInfo userInfo, JwtClaimsSet jwtClaimsSet,
            AbstractSocialAuthLoginNode.Config config)
            throws AuthLoginException {
        try {
            return unmodifiableMap(OAuthUtil.getAttributes(userInfo.getRawProfile().toString(),
                    config.cfgAccountMapperConfiguration(), getAccountMapper(config), jwtClaimsSet));
        } catch (OAuthException e) {
            throw new AuthLoginException("Unable to normalise user account attributes", e);
        }
    }

    /**
     * Gets normalized attributes for the user. It internally uses Attribute Mapper configurations.
     *
     * @param userInfo The user attributes from the social provider.
     * @param jwtClaimsSet The id token claim from the social provider.
     * @param config the config
     * @return The normalized attributes.
     * @throws AuthLoginException when attributes can't be mapped.
     */
    public Map<String, Set<String>> getNormalisedAttributes(UserInfo userInfo, JwtClaimsSet jwtClaimsSet,
            AbstractSocialAuthLoginNode.Config config)
            throws AuthLoginException {
        try {
            return unmodifiableMap(OAuthUtil.getAttributesMap(config.cfgAttributeMappingConfiguration(),
                    config.cfgAttributeMappingClasses(),
                    userInfo.getRawProfile().toString(), jwtClaimsSet));
        } catch (OAuthException e) {
            throw new AuthLoginException("Unable to normalise user attributes", e);
        }
    }
}
