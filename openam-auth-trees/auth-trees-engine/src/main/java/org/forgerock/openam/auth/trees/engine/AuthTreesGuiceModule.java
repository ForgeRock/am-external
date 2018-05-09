/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.trees.engine;

import static com.google.inject.name.Names.named;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Guice Module for configuring bindings for the OpenAM Authentication Trees.
 *
 * @since AM 5.5.0
 */
public class AuthTreesGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(named("amAuth")).toInstance(Debug.getInstance("amAuth"));
        requestStaticInjection(Action.class);
    }

    /**
     * Provide a function that can provide auth trees that provide the opportunity to achieve a particular auth level,
     * given a realm and an auth level.
     * @param authTreeService The auth tree service that provides the method that fulfils the lambda signature.
     * @return The function.
     */
    @Provides
    @Inject
    public BiFunction<Integer, Realm, Set<String>> getAuthLevelTreeFinder(AuthTreeService authTreeService) {
        return authTreeService::getAuthTrees;
    }

    /**
     * Returns the list of system session property names.
     * @return System session property names.
     */
    @Provides
    @Named("SystemSessionProperties")
    public List<String> getSystemSessionProperties() {
        return asList(
                Constants.UNIVERSAL_IDENTIFIER,
                Constants.AM_CTX_ID,
                ISAuthConstants.AUTH_INSTANT,
                ISAuthConstants.AUTH_LEVEL,
                ISAuthConstants.CHARSET,
                ISAuthConstants.CLIENT_TYPE,
                ISAuthConstants.FULL_LOGIN_URL,
                ISAuthConstants.HOST,
                ISAuthConstants.INDEX_TYPE,
                ISAuthConstants.LOCALE,
                ISAuthConstants.LOGIN_URL,
                ISAuthConstants.ORGANIZATION,
                ISAuthConstants.PRINCIPAL,
                ISAuthConstants.PRINCIPALS,
                ISAuthConstants.SERVICE,
                ISAuthConstants.SUCCESS_URL,
                ISAuthConstants.USER_ID,
                ISAuthConstants.USER_PROFILE,
                ISAuthConstants.USER_TOKEN,
                ISAuthConstants.HOST_NAME,
                ISAuthConstants.WEB_HOOKS,
                SystemProperties.get(Constants.AM_LB_COOKIE_NAME, "amlbcookie"));
    }
}
