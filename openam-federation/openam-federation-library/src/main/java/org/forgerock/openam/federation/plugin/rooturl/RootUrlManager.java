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

package org.forgerock.openam.federation.plugin.rooturl;

import java.util.ResourceBundle;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.locale.Locale;

/**
 * Singleton class used to manage Root URL providers.
 *
 * @supported.api
 */
public enum RootUrlManager {

    /**
     * The root url manager instance.
     */
    INSTANCE;

    private Logger debug = LoggerFactory.getLogger(RootUrlManager.class);

    /**
     * Attribute name for default provider.
     */
    private static final String DEFAULT_PROVIDER_ATTR = "com.sun.identity.plugin.root.url.class.default";

    private ResourceBundle bundle = Locale.getInstallResourceBundle("libRootUrlProvider");

    private RootUrlProvider provider;

    RootUrlManager() {
        try {
            provider = loadDefaultProvider();
        } catch (RootUrlProviderException e) {
            debug.error("RootUrlManager: exception obtaining default provider:", e);
        }
    }

    /**
     * Gets the default root url provider.
     *
     * @return the default provider.
     * @throws RootUrlProviderException when the default provider is null.
     */
    public RootUrlProvider getDefaultProvider() throws RootUrlProviderException {
        if (provider == null) {
            throw new RootUrlProviderException(bundle.getString("nullRootUrlProvider"));
        }
        return provider;
    }

    private RootUrlProvider loadDefaultProvider() throws RootUrlProviderException {
        String className = SystemConfigurationUtil.getProperty(DEFAULT_PROVIDER_ATTR);
        if (StringUtils.isEmpty(className)) {
            throw new RootUrlProviderException(bundle.getString("defaultProviderNotDefined"));
        }
        try {
            return InjectorHolder.getInstance(Class.forName(className).asSubclass(RootUrlProvider.class));
        } catch (Exception e) {
            throw new RootUrlProviderException(e);
        }
    }
}
