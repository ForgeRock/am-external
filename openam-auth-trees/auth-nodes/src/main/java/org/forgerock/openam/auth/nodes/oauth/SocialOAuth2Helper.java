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
package org.forgerock.openam.auth.nodes.oauth;

import static com.iplanet.am.util.SecureRandomManager.getSecureRandom;
import static com.sun.identity.authentication.spi.AMLoginModule.getAMIdentityRepository;
import static org.forgerock.openam.utils.Time.getTimeService;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.http.Handler;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.util.time.TimeService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.encode.Base64;

/**
 * This class provides helper functions for the social auth nodes.
 */
public class SocialOAuth2Helper {
    /**
     * The scope delimiter for oauth2. This value exists because some provider may use different one
     * ( facebook used to use a comma for instance)
     */
    public static final String DEFAULT_OAUTH2_SCOPE_DELIMITER = " ";

    /**
     * Key to store and access the userinfo data in the shared state.
     */
    public static final String USER_INFO_SHARED_STATE_KEY = "userInfo";

    /**
     * Key to store and access the names data in the shared state.
     */
    public static final String USER_NAMES_SHARED_STATE_KEY = "userNames";

    /**
     * Key to store and access the user attributes data in the shared state.
     */
    public static final String ATTRIBUTES_SHARED_STATE_KEY = "attributes";

    private Handler handler;

    /**
     * Constructor.
     *
     * @param handler the handler for the OAuthClient
     */
    @Inject
    public SocialOAuth2Helper(@Named("SocialAuthClientHandler") Handler handler) {
        this.handler = handler;
    }

    /**
     *  Create a new OAuthClient instance based on the OAuthClientConfiguration.
     *
     *  @param config The OAuthClientConfiguration instance.
     *  @return The new OAuthClient instance.
     */
    public OAuthClient newOAuthClient(OAuthClientConfiguration config) {
        try {
            final Class<? extends OAuthClient> oauthClient =
                    (Class<? extends OAuthClient>) Class.forName(config.getClientClass().getName(), true,
                            getClass().getClassLoader());
            return oauthClient.getConstructor(Handler.class, config.getClass(), TimeService.class, SecureRandom.class)
                    .newInstance(handler, config, getTimeService(), getSecureRandom());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create an instance of oAuthClient", e);
        }
    }

    /**
     * Gets and existing user from the data store, based on the given criteria.
     *
     * @param realm The realm in which the user belongs.
     * @param accountProvider The provider class using the which the search will be performed.
     * @param userNames The name of the user.
     * @return The user name if exist in the data store.
     */
    public Optional<String> userExistsInTheDataStore(String realm, AccountProvider accountProvider,
            Map<String, Set<String>> userNames) {
        if (!userNames.isEmpty()) {
            final String user = getUser(realm, accountProvider, userNames);
            if (user != null) {
                return Optional.of(user);
            }
        }
        return Optional.absent();
    }

    private String getUser(String realm, AccountProvider accountProvider,
            Map<String, Set<String>> userNames) {

        String user = null;
        if ((userNames != null) && !userNames.isEmpty()) {
            AMIdentity userIdentity = accountProvider.searchUser(
                    AuthD.getAuth().getAMIdentityRepository(realm), userNames);
            if (userIdentity != null) {
                user = userIdentity.getName();
            }
        }

        return user;
    }

    /**
     * Generates a string of 20 random bytes.
     * @return the Base64 encoded random string.
     * @throws NodeProcessException Thrown if an error occurs while generating a random string
     */
    public String getRandomData() throws NodeProcessException {
        byte[] pass = new byte[20];
        try {
            getSecureRandom().nextBytes(pass);
        } catch (Exception e) {
            throw new NodeProcessException("Error while generating random data", e);
        }
        return Base64.encode(pass);
    }

    /**
     * Provisions a user with the specified attributes.
     *
     * @param realm The realm.
     * @param accountProvider The account provider for creating the user.
     * @param attributes The user attributes.
     * @return The name of the created user.
     * @throws AuthLoginException If an error occurs creating the user.
     */
    public String provisionUser(String realm, AccountProvider accountProvider, Map<String, Set<String>> attributes)
            throws AuthLoginException {
        AMIdentity userIdentity = accountProvider.provisionUser(getAMIdentityRepository(realm), attributes);
        return userIdentity.getName();
    }

    /**
     * Converts a Map of List, of the kind returned by {@link org.forgerock.json.JsonValue} into a Map of Set.
     * @param mapOfList The source Map of List
     * @return The converted Map
     */
    public static Map<String, Set<String>> convertMapOfListToMapOfSet(Map<String, List<String>> mapOfList) {
        return mapOfList.entrySet().stream()
                .collect(Collectors.toMap(p -> p.getKey(), p -> new HashSet<>(p.getValue())));
    }
}
