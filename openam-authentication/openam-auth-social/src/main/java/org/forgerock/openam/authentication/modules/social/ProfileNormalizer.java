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

package org.forgerock.openam.authentication.modules.social;

import static java.util.Collections.unmodifiableMap;

import java.util.Map;
import java.util.Set;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.utils.MappingUtils;

import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * User profile normaliser. It uses configured Account Mapper and Attribute Mappers to normalise the user attributes.
 *
 * @see AbstractSmsSocialAuthConfiguration
 */
class ProfileNormalizer {

    private final AbstractSmsSocialAuthConfiguration config;

    private AccountProvider accountProvider;
    private AttributeMapper accountMapper;
    private Map<String, String> accountMapperConfig;
    private Set<String> attributeMappers;
    private Map<String, String> attributeMapperConfig;

    ProfileNormalizer(AbstractSmsSocialAuthConfiguration config) {
        this.config = config;
    }

    /**
     * @return The AccountProvider instance.
     * @throws AuthLoginException when error occurs while instantiating the provider class.
     */
    AccountProvider getAccountProvider() throws AuthLoginException {
        if (accountProvider == null) {
            accountProvider = OAuthUtil.instantiateAccountProvider(config.getCfgAccountProviderClass());
        }
        return accountProvider;
    }

    private AttributeMapper getAccountMapper() throws AuthLoginException {
        if (accountMapper == null) {
            accountMapper = OAuthUtil.instantiateAccountMapper(config.getCfgAccountMapperClass());
        }
        return accountMapper;
    }

    private Set<String> getAttributeMappers() throws AuthLoginException {
        if (attributeMappers == null) {
            attributeMappers = config.getCfgAttributeMappingClasses();
        }
        return attributeMappers;
    }

    private Map<String, String> getAccountMapperConfig() throws AuthLoginException {
        if (accountMapperConfig == null) {
            accountMapperConfig = MappingUtils.parseMappings(config.getCfgAccountMapperConfiguration());
        }
        return accountMapperConfig;
    }

    private Map<String, String> getAttributeMapperConfig() throws AuthLoginException {
        if (attributeMapperConfig == null) {
            attributeMapperConfig = MappingUtils.parseMappings(config.getCfgAttributeMappingConfiguration());
        }
        return attributeMapperConfig;
    }

    /**
     * Gets normalized attributes for the Account lookup. It internally uses Account Mapper configurations.
     *
     * @param userInfo The user attributes from the social provider.
     * @param jwtClaimsSet The id token claim from the social provider.
     * @return The normalized attributes.
     * @throws AuthLoginException when attributes can't be mapped.
     */
    public Map<String, Set<String>> getNormalisedAccountAttributes(UserInfo userInfo, JwtClaimsSet jwtClaimsSet)
            throws AuthLoginException {
        try {
            return unmodifiableMap(OAuthUtil.getAttributes(userInfo.getRawProfile().toString(),
                    getAccountMapperConfig(), getAccountMapper(), jwtClaimsSet));
        } catch (OAuthException e) {
            throw new AuthLoginException("Unable to normalise user account attributes", e);
        }
    }

    /**
     * Gets normalized attributes for the user. It internally uses Attribute Mapper configurations.
     *
     * @param userInfo The user attributes from the social provider.
     * @param jwtClaimsSet The id token claim from the social provider.
     * @return The normalized attributes.
     * @throws AuthLoginException when attributes can't be mapped.
     */
    public Map<String, Set<String>> getNormalisedAttributes(UserInfo userInfo, JwtClaimsSet jwtClaimsSet)
            throws AuthLoginException {
        try {
            return unmodifiableMap(OAuthUtil.getAttributesMap(getAttributeMapperConfig(), getAttributeMappers(),
                    userInfo.getRawProfile().toString(), jwtClaimsSet));
        } catch (OAuthException e) {
            throw new AuthLoginException("Unable to normalise user attributes", e);
        }
    }

}
