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
package org.forgerock.openam.service.datastore;

import static com.sun.identity.sm.SmsWrapperObject.isFbcWithoutEmbeddedEnabled;
import static org.forgerock.openam.services.datastore.DataStoreId.CONFIG_ID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.openam.services.datastore.DataStoreId;
import org.forgerock.openam.services.datastore.DataStoreLookup;

import com.sun.identity.sm.ChoiceValues;

/**
 * A choice value class that finds the all the globally configured data stores.
 */
public class DataStoreChoiceValues extends ChoiceValues {

    protected final DataStoreLookup dataStoreLookup;

    @Inject
    public DataStoreChoiceValues(DataStoreLookup dataStoreLookup) {
        this.dataStoreLookup = dataStoreLookup;
    }

    @Override
    public Map<String, String> getChoiceValues() {
        return getChoiceValues(dataStoreLookup.getEnabledIds());
    }

    protected Map<String, String> getChoiceValues(Set<DataStoreId> dataStoreIds) {
        Set<String> enabledDataStoreNames = dataStoreIds.stream()
                .map(DataStoreId::getOriginalId)
                .collect(Collectors.toSet());

        final Map<String, String> choiceValues = new LinkedHashMap<>();
        if (!isFbcWithoutEmbeddedEnabled()) {
            choiceValues.put(CONFIG_ID, "datastore.config.name"); // see DataStoreService.properties
        }
        return enabledDataStoreNames.stream().collect(
                Collectors.toMap(
                        dataStoreName -> dataStoreName, //key converter
                        Function.identity(),  //value converter
                        (first, second) -> first, //merge strat
                        () -> choiceValues //map supplier
                )
        );
    }
}
