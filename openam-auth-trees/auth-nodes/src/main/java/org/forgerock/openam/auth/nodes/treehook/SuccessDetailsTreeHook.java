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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static org.forgerock.json.JsonValue.json;

import java.io.IOException;

import javax.inject.Inject;

import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.SetSuccessDetailsNode;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

/**
 * A tree hook that sets success details on the response.
 */
@TreeHook.Metadata(configClass = SetSuccessDetailsNode.Config.class)
public class SuccessDetailsTreeHook extends DetailsTreeHook {

    private final SetSuccessDetailsNode.Config config;
    private final SSOToken ssoToken;
    private final Response response;

    @Inject
    SuccessDetailsTreeHook(@Assisted SetSuccessDetailsNode.Config config, @Assisted SSOToken ssoToken,
            @Assisted Response response) {
        this.config = config;
        this.ssoToken = ssoToken;
        this.response = response;
    }

    @Override
    public void accept() throws TreeHookException {
        JsonValue responseBody;
        try {
            responseBody = json(response.getEntity().getJson());
        } catch (IOException e) {
            throw new TreeHookException("Failed to parse response body", e);
        }
        for (String key : config.sessionProperties().keySet()) {
            try {
                var sessionProperty = ssoToken.getProperty(config.sessionProperties().get(key));
                if (sessionProperty != null) {
                    responseBody.put(key, convertStringToJson(sessionProperty));
                }
            } catch (SSOException e) {
                throw new TreeHookException("Failed to get property from session " + key, e);
            }
        }
        config.successDetails().forEach((key, value) -> responseBody.put(key, convertStringToJson(value)));
        response.getEntity().setJson(responseBody);
    }
}
