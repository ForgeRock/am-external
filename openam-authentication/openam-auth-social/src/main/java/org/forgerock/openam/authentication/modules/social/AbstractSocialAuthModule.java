/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import java.util.Map;
import java.util.function.Function;

import org.forgerock.openam.authentication.modules.common.AbstractLoginModuleBinder;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

/**
 * Base class for all Social Auth modules.
 */
abstract class AbstractSocialAuthModule extends AbstractLoginModuleBinder<SocialAuthLoginModule> {

    public AbstractSocialAuthModule(Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction) {
        super(new SocialAuthLoginModule(configurationFunction));
    }
}
