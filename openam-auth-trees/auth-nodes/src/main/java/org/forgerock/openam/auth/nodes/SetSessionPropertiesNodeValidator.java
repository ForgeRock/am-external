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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapSetThrows;
import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.configuration.MapValueParser;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;

import com.google.common.collect.Sets;
import com.sun.identity.shared.datastruct.ValueNotFoundException;

/**
 * A validator for the SetSessionPropertiesNode.
 */
public class SetSessionPropertiesNodeValidator implements ServiceConfigValidator {

    private static final String MAX_SESSION_TIME = "maxSessionTime";
    private static final String MAX_IDLE_TIME = "maxIdleTime";

    private final MapValueParser mapValueParser;
    private final Set<String> systemSessionProperties;

    /**
     * Constructs a new SetSessionPropertiesNodeValidator.
     *
     * @param mapValueParser the map value parser.
     * @param systemSessionProperties the system session properties.
     */
    @Inject
    public SetSessionPropertiesNodeValidator(MapValueParser mapValueParser,
                                             @Named("SystemSessionProperties") List<String> systemSessionProperties) {
        this.mapValueParser = mapValueParser;
        this.systemSessionProperties = Set.copyOf(systemSessionProperties);
    }

    @Override
    public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
            throws ServiceConfigException, ServiceErrorException {
        Map<String, String> properties = getProperties(attributes);
        Optional<String> maxSessionTime = Optional.ofNullable(getMapAttr(attributes, MAX_SESSION_TIME));
        Optional<String> maxIdleTime = Optional.ofNullable(getMapAttr(attributes, MAX_IDLE_TIME));

        if (properties.isEmpty() && maxSessionTime.isEmpty() && maxIdleTime.isEmpty()) {
            throw new ServiceConfigException(
                    String.format("At least one of the following properties must be set: properties, %s, %s",
                            MAX_SESSION_TIME, MAX_IDLE_TIME));
        }
        if (!Sets.intersection(systemSessionProperties, properties.keySet()).isEmpty()) {
            throw new ServiceConfigException("Provided properties cannot include system session properties: "
                    + systemSessionProperties);
        }
        if (properties.values().stream().anyMatch(String::isEmpty)) {
            throw new ServiceConfigException("Session property values cannot be empty");
        }
    }

    private Map<String, String> getProperties(Map<String, Set<String>> attributes) {
        try {
            return mapValueParser.parse(getMapSetThrows(attributes, "properties"));
        } catch (ValueNotFoundException e) {
            return emptyMap();
        }
    }
}
