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
 * Copyright 2010-2022 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.openam.saml2.plugins.IDPAdapter;

/**
 * This interface <code> SAML2IdentityProviderAdapter</code> is used to perform
 * specific tasks in the IdP
 *
 *  @deprecated since AM 7.3.0 Implement use-case specific {@link IDPAdapter} implementations instead.
 *
 */
@Deprecated(forRemoval = true, since = "7.3.0")
@SupportedAll
public interface SAML2IdentityProviderAdapter extends IDPAdapter {
}
