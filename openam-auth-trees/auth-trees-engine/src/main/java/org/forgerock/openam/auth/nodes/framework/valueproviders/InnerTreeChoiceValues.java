/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.valueproviders;

import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;

/**
 * This class is used to retrieve the tree names and IDs from the auth tree service for display
 * in a drop down UI component.
 *
 * @since 13.0.0
 */
public class InnerTreeChoiceValues extends ChoiceValues {

    /**
     * Default logger for the class.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger("amAuth");

    /**
     * Defaut empty constructor.
     */
    public InnerTreeChoiceValues() {
    }

    @Override
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }

    @Override
    public Map getChoiceValues(Map envParams) {
        String realm = null;
        if (envParams != null) {
            realm = (String) envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (isBlank(realm)) {
            realm = SMSEntry.getRootSuffix();
        }

        AuthTreeService authTreeService = InjectorHolder.getInstance(AuthTreeService.class);

        Set<String> authTreeNames = null;
        try {
            authTreeNames = authTreeService.getAllAuthTrees(Realms.of(realm));
        } catch (RealmLookupException e) {
            LOGGER.error("Error getting the list of available trees for the realm {}", realm, e);
        }
        Map<String, String> choiceValues = authTreeNames.stream()
                .collect(Collectors.toMap(treeName -> treeName, Function.identity()));

        choiceValues.put(EMPTY_SCRIPT_SELECTION, "label.select.tree");
        return choiceValues;
    }
}
