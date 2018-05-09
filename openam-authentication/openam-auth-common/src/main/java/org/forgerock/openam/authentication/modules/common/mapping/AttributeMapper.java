/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common.mapping;

import com.sun.identity.authentication.spi.AuthLoginException;

import java.util.Map;
import java.util.Set;

/**
 * Translates from a source to a map of attributes.
 * @param <T> The type of source.
 *
 * @supported.all.api
 */
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
