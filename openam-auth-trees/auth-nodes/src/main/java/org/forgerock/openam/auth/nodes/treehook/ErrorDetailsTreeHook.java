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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.TreeFailureResponse;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.SetErrorDetailsNode;

import com.google.inject.assistedinject.Assisted;

/**
 * A tree hook that sets error details on the response.
 */
@TreeHook.Metadata(configClass = SetErrorDetailsNode.Config.class)
public class ErrorDetailsTreeHook extends DetailsTreeHook {

    private final SetErrorDetailsNode.Config config;
    private final TreeFailureResponse response;
    private final JsonValue data;

    @Inject
    ErrorDetailsTreeHook(@Assisted SetErrorDetailsNode.Config config,
            @Assisted TreeFailureResponse response, @Assisted JsonValue data) {
        this.config = config;
        this.response = response;
        this.data = data;
    }

    @Override
    public void accept() throws TreeHookException {
        // do nothing
    }

    @Override
    public void acceptException() {
        config.errorDetails()
                .forEach((key, value) -> response.addFailureDetail(key, convertStringToJson(value)));
        if (data != null && data.isDefined("message")) {
            response.setCustomFailureMessage(data.get("message").asString());
        }
    }
}
