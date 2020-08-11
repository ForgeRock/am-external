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
 * Copyright 2017-2019 ForgeRock AS.
 */

package org.forgerock.openam.services.push.dispatch.handlers;

import org.forgerock.json.JsonValue;
import org.forgerock.am.cts.api.tokens.Token;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.MessageId;
import org.forgerock.openam.services.push.MessageState;

/**
 *
 * Interface for interacting with the CTS-versions of message tokens.
 */
public interface ClusterMessageHandler {

    /**
     * Update the cluster-wide token.
     *
     * @param token The token to update.
     * @param content The contents to write into the token.
     * @throws CoreTokenException In case of exceptions writing to the CTS.
     */
    void update(Token token, JsonValue content) throws CoreTokenException;

    /**
     * Check the {@link MessageState} of a cluster-wide token.
     *
     * @param messageId The ID of the message that needs to be checked.
     * @return This message's MessageState.
     * @throws CoreTokenException In case of exceptions writing to the CTS.
     */
    MessageState check(MessageId messageId) throws CoreTokenException;

    /**
     * Get the {@link JsonValue} contents of a cluster-wide token.
     *
     * @param messageId The ID of the message whose contents to retrieve.
     * @return the JsonValue contents of the token.
     * @throws CoreTokenException In case of exceptions writing to the CTS.
     */
    JsonValue getContents(MessageId messageId) throws CoreTokenException;

    /**
     * Delete this message from the cluster-wide store.
     *
     * @param messageId The message whose token to remove.
     * @throws CoreTokenException In case of exceptions writing to the CTS.
     */
    void delete(MessageId messageId) throws CoreTokenException;

}
