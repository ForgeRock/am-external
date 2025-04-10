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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.common.mapping;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.authentication.spi.AuthLoginException;

/**
 * Translates from a source to a map of attributes.
 * @param <T> The type of source.
 *
 */
@SupportedAll
public interface AttributeMapper<T> {

    /**
     * Initialise the instance for i18n.
     * @param bundleName The name of the bundle for exceptions thrown by the getAttributes method.
     */
    void init(String bundleName);

    /**
     * Maps from values found in the source to a map of keys in the result, according to a provided map of keys in the
     * source to keys in the result.
     * @param attributeMapConfiguration The map of keys in the source to keys in the result.
     * @param source The source of values.
     * @return A map of attribute keys to values found.
     * @throws AuthLoginException If there was an error while retrieving the user attributes.
     */
    Map<String, Set<String>> getAttributes(Map<String, String> attributeMapConfiguration, T source)
            throws AuthLoginException;

}
