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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import org.forgerock.openam.annotations.EvolvingAll;

/**
 * This class is an extension point for invalid usernames in the SAML flow.
 * This class also handles message localization in SAML exceptions.
 */
@EvolvingAll
public class SAML2InvalidUserException extends SAML2Exception {

    /**
     * Constructs a new <code>SAML2InvalidUserException</code> without
     * a nested <code>Throwable</code>.
     * @param rbName Resource Bundle Name to be used for getting
     * localized error message.
     * @param errorCode Key to resource bundle. You can use
     * <pre>
     * ResourceBundle rb = ResourceBundle.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode);
     * </pre>
     * @param args arguments to message. If it is not present pass them
     * as null
     *
     */
    public SAML2InvalidUserException(String rbName, String errorCode, Object... args) {
        super(rbName, errorCode, args);
    }

    /**
     * Constructs a new <code>SAML2InvalidUserException</code> with
     * the given message.
     *
     * @param message message for this exception. This message can be later
     * retrieved by <code>getMessage()</code> method.
     *
     */
    public SAML2InvalidUserException(String message) {
        super(message);
    }

    /**
     * Constructs an <code>SAML2InvalidUserException</code> with given
     * <code>Throwable</code>.
     *
     * @param t Exception nested in the new exception.
     *
     */
    public SAML2InvalidUserException(Throwable t) {
        super(t);
    }
}
