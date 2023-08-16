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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.saml2.meta;

import com.sun.identity.saml2.meta.SAML2MetaException;

/**
 * A sub-exception of {@link SAML2MetaException} for {@code SAML2MetaManager}. This exception type
 * indicates that a Configuration Instance action was not able to be performed due to insufficient user authorization.
 */
public class Saml2AuthorizationMetaException extends SAML2MetaException {

    /**
     * A constructor with a throwable.
     *
     * @param t The throwable.
     */
    public Saml2AuthorizationMetaException(Throwable t) {
        super(t);
    }
}