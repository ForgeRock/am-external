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
 * Copyright 2023 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins.scripted.wrappers;

import java.util.Map;

import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper;

/**
 * Provides helper functions for SP Adapter Script Implementations.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class SpAdapterHelperScriptWrapper {

    private final SpAdapterScriptHelper spAdapterScriptHelper;

    public SpAdapterHelperScriptWrapper(SpAdapterScriptHelper spAdapterScriptHelper) {
        this.spAdapterScriptHelper = spAdapterScriptHelper;
    }

    /**
     * Returns a map of the SP Adapter environment.
     *
     * @param realm realm of hosted entity
     * @param spEntityId name of hosted SP entity
     * @return a map of the SP Adapter Environment
     */
    public Map<String, String> getSpAdapterEnv(String realm, String spEntityId) {
        return spAdapterScriptHelper.getSpAdapterEnv(realm, spEntityId);
    }

}
