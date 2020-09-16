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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.federation.plugin.rooturl.impl;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.plugin.rooturl.RootUrlProvider;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;

/**
 * This is the default server side {@link RootUrlProvider} implementation that utilizes the Base URL service
 * to determine AM's root URL.
 */
public class FmRootUrlProvider implements RootUrlProvider {

    private final BaseURLProviderFactory baseURLProviderFactory =
            InjectorHolder.getInstance(BaseURLProviderFactory.class);

    @Override
    public String getRootURL(String realm, HttpServletRequest request) {
        BaseURLProvider baseURLProvider = baseURLProviderFactory.get(realm);
        return baseURLProvider.getRootURL(request);
    }
}
