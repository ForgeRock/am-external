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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.federation.testutils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;

/**
 * A simplified DataStore provider implementation for an in-memory only store.
 */
public class TestCaseDataStoreProvider implements DataStoreProvider {

    /** By userID, attribute, values */
    private static Map<String, Map<String, Set<String>>> userAttributes;
    /** By userID, attribute, binary values */
    private static Map<String, Map<String, byte[][]>> userAttributesBinary;

    /**
     * Set/Reset the attributes for this DataStore provider.
     * @param newUserAttributes A map of attributes keyed by UserID
     * @param newUserAttributesBinary A map of binary attributes keyed by UserID
     */
    public static void resetAttributes(Map<String, Map<String, Set<String>>> newUserAttributes,
                                Map<String, Map<String, byte[][]>> newUserAttributesBinary) {
        userAttributes = newUserAttributes;
        userAttributesBinary = newUserAttributesBinary;
    }

    @Override
    public void init(String componentName) throws DataStoreProviderException {
    }

    @Override
    public Set<String> getAttribute(String userID, String attrName) throws DataStoreProviderException {
        Set<String> result = null;
        if (userAttributes != null) {
            Map<String, Set<String>> attributes = userAttributes.get(userID);
            if (attributes != null) {
                result = attributes.get(attrName);
            }
        }

        return result;
    }

    @Override
    public Map<String, Set<String>> getAttributes(String userID, Set<String> attrNames) throws DataStoreProviderException {
        Map<String, Set<String>> result = null;
        if (userAttributes != null) {
            Map<String, Set<String>> theUserAttributes = userAttributes.get(userID);
            if (theUserAttributes != null) {
                result = new HashMap<>();
                for (String attributeName : attrNames) {
                    Set<String> attributes = theUserAttributes.get(attributeName);
                    if (attributes != null) {
                        result.put(attributeName, new HashSet<>(attributes));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public byte[][] getBinaryAttribute(String userID, String attrName) throws DataStoreProviderException {
        byte[][] result = null;
        if (userAttributesBinary != null) {
            Map<String, byte[][]> attributes = userAttributesBinary.get(userID);
            if (attributes != null) {
                result = attributes.get(attrName);
            }
        }

        return result;
    }

    @Override
    public Map<String, byte[][]> getBinaryAttributes(String userID, Set<String> attrNames) throws DataStoreProviderException {
        Map<String, byte[][]> result = null;
        if (userAttributesBinary != null) {
            Map<String, byte[][]> theUserAttributes = userAttributesBinary.get(userID);
            if (theUserAttributes != null) {
                result = new HashMap<>();
                for (String attributeName : attrNames) {
                    byte[][] attributes = theUserAttributes.get(attributeName);
                    if (attributes != null) {
                        result.put(attributeName, attributes);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void setAttributes(String userID, Map<String, Set<String>> attrMap) throws DataStoreProviderException {
        userAttributes.put(userID, attrMap);
    }

    @Override
    public String getUserID(String orgDN, Map<String, Set<String>> avPairs) throws DataStoreProviderException {
        return null;
    }

    @Override
    public boolean isUserExists(String userID) throws DataStoreProviderException {
        return userAttributes.containsKey(userID);
    }
}
