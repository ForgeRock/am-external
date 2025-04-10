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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import org.forgerock.openam.annotations.Supported;

/**
 * A response object that contains details of the tree failure.
 */
@Supported
public interface TreeFailureResponse {
    /**
     * Adds a new entry to the failure details.
     *
     * @param key   The key to add.
     * @param value The value to add.
     */
    @Supported
    void addFailureDetail(String key, Object value);

    /**
     * Sets the failure message that will be displayed to the user to a custom value.
     *
     * @param failureMessage The failure message to set.
     */
    @Supported
    void setCustomFailureMessage(String failureMessage);

    /**
     * Returns the HTTP response code that will be sent back to the client.
     *
     * @return The HTTP response code.
     */
    @Supported
    int getStatusCode();

    /**
     * Returns the framework provided default failure message.
     *
     * @return The default message.
     */
    @Supported
    String getDefaultMessage();
}
