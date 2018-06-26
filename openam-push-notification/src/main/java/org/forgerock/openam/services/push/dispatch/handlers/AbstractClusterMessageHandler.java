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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.services.push.dispatch.handlers;

import static org.forgerock.openam.utils.Time.getCalendarInstance;

import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;
import org.forgerock.openam.services.push.PushNotificationConstants;

/**
 * An abstract cluster message handler for common functionality across {@link ClusterMessageHandler}s.
 */
public abstract class AbstractClusterMessageHandler implements ClusterMessageHandler {

    /**
     * CTS for storing and retrieving clustered messages.
     */
    protected final CTSPersistentStore ctsPersistentStore;

    /**
     * Construct a new AbstractClusterMessageHandler.
     *
     * @param ctsPersistentStore Reference to the CTS.
     */
    public AbstractClusterMessageHandler(CTSPersistentStore ctsPersistentStore) {
        this.ctsPersistentStore = ctsPersistentStore;
    }

    @Override
    public void delete(MessageId messageId) throws CoreTokenException {
        ctsPersistentStore.deleteAsync(messageId.toString());
    }

    @Override
    public MessageState check(MessageId messageId) throws CoreTokenException {
        Token coreToken = ctsPersistentStore.read(messageId.toString());

        if (coreToken == null || coreToken.getExpiryTimestamp().before(getCalendarInstance())) {
            return null;
        }

        MessageState result = MessageState.UNKNOWN;
        Integer deny = coreToken.getAttribute(PushNotificationConstants.CTS_ACCEPT_TOKEN_FIELD);

        if (deny == null) {
            return result;
        } else {
            if (deny == PushNotificationConstants.DENY_VALUE) {
                result = MessageState.DENIED;
            } else {
                result = MessageState.SUCCESS;
            }
        }

        return result;
    }
}
