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

package org.forgerock.openam.services.push.dispatch.handlers;

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.services.push.PushNotificationConstants;
import org.forgerock.openam.services.push.dispatch.MessagePromise;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * Registration cluster message handler.
 */
public class RegClusterMessageHandler extends AbstractClusterMessageHandler {

    private final JSONSerialisation jsonSerialisation;

    /**
     * Generate a new Registration cluster message handler.
     *
     * @param ctsPersistentStore For accessing tokens in the CTS.
     * @param jsonSerialisation For objectifying tokens in the CTS.
     */
    @Inject
    public RegClusterMessageHandler(CTSPersistentStore ctsPersistentStore, JSONSerialisation jsonSerialisation) {
        super(ctsPersistentStore);
        this.jsonSerialisation = jsonSerialisation;
    }

    @Override
    public void update(Token token, JsonValue content) throws CoreTokenException {
        token.setBlob(jsonSerialisation.serialise(content.getObject()).getBytes());
        token.setAttribute(PushNotificationConstants.CTS_ACCEPT_TOKEN_FIELD, ACCEPT_VALUE);
        ctsPersistentStore.update(token);
    }

    @Override
    public JsonValue getContents(MessagePromise messagePromise) throws CoreTokenException {
        Token coreToken = ctsPersistentStore.read(messagePromise.getMessageId());

        if (coreToken == null) {
            return null;
        }

        return JsonValueBuilder.toJsonValue(new String(coreToken.getBlob(), StandardCharsets.UTF_8));
    }

}
