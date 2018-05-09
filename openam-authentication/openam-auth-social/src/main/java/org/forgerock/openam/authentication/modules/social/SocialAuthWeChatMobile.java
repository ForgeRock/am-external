/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import org.forgerock.openam.authentication.modules.common.AbstractLoginModuleBinder;

/**
 * Social Auth module for WeChat Mobile.
 */
public class SocialAuthWeChatMobile
        extends AbstractLoginModuleBinder<SocialAuthLoginModuleWeChatMobile> {

    /**
     * Constructs an instance of SocialAuthWeChatMobile.
     */
    public SocialAuthWeChatMobile() {
        super(new SocialAuthLoginModuleWeChatMobile(
                map -> new SmsSocialAuthWeChatMobileConfiguration(map)));
    }
}
