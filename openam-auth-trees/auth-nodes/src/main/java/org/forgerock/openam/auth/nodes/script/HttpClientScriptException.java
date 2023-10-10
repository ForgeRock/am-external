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
package org.forgerock.openam.auth.nodes.script;

import org.forgerock.openam.annotations.Supported;

/**
 * An Exception thrown by the {@link HttpClientScriptWrapper} which can be used for logging purposes in scripts.
 */
@Supported
public class HttpClientScriptException extends Exception {

    /**
     * The exception message.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public final String message;

    HttpClientScriptException(Exception e) {
        this.message = e.getLocalizedMessage();
    }

}
