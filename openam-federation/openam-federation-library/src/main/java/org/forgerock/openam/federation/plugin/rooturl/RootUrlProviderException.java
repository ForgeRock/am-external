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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.federation.plugin.rooturl;

import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * To be used when an exception has occurred in a root url provider.
 */
public class RootUrlProviderException extends L10NMessageImpl {

    /**
     * Constructs a <code>RootUrlProviderException</code> with a detailed message.
     *
     * @param message Detailed message for this exception.
     */
    public RootUrlProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a <code>RootUrlProviderException</code> with an embedded exception.
     *
     * @param rootCause An embedded exception
     */
    public RootUrlProviderException(Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Constructs a <code>RootUrlProviderException</code> with an exception.
     *
     * @param ex an exception
     */
    public RootUrlProviderException(Exception ex) {
        super(ex);
    }

    /**
     * Constructs a new <code>RootUrlProviderException</code> without a nested
     * <code>Throwable</code>.
     *
     * @param rbName Resource Bundle Name to be used for getting localized error message.
     * @param errorCode Key to resource bundle. You can use
     * <pre>
     * ResourceBundle rb = ResourceBundle.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode);
     * </pre>
     * @param args arguments to message. If it is not present pass them as null
     */
    public RootUrlProviderException(String rbName, String errorCode, Object[] args) {
        super(rbName, errorCode, args);
    }
}
