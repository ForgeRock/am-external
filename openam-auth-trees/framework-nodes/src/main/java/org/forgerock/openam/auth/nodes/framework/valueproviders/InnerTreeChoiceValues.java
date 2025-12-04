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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.valueproviders;

import static com.sun.identity.shared.locale.Locale.getDefaultLocale;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.AMResourceBundleCache;
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
    protected static final Logger LOGGER = LoggerFactory.getLogger(InnerTreeChoiceValues.class);
    private final TreeProvider treeProvider;
    private final RealmLookup realmLookup;

    /**
     * Defaut empty constructor.
     * @param treeProvider The tree provider.
     * @param realmLookup The realm lookup.
     */
    @Inject
    public InnerTreeChoiceValues(TreeProvider treeProvider, RealmLookup realmLookup) {
        this.treeProvider = treeProvider;
        this.realmLookup = realmLookup;
    }

    @Override
    public Map<String, String> getChoiceValues() {
        return getChoiceValues(Map.of());
    }

    @Override
    public Map<String, String> getChoiceValues(Map<String, Object> envParams) {
        String realm = null;
        if (envParams != null) {
            realm = (String) envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (isBlank(realm)) {
            realm = SMSEntry.getRootSuffix();
        }
        Locale locale = envParams != null && envParams.containsKey(Constants.LOCALE)
                ? (Locale) envParams.get(Constants.LOCALE)
                : getDefaultLocale();

        Map<String, String> choices = new LinkedHashMap<>(singletonMap(EMPTY_SCRIPT_SELECTION, "label.select.tree"));

        try {
            TreeMap<String, String> trees = treeProvider.getTrees(realmLookup.lookup(realm)).stream()
                    .collect(toMap(Tree::getName, t -> treeDisplayName(t, locale), (a, b) -> a, TreeMap::new));
            choices.putAll(trees);
        } catch (RealmLookupException e) {
            LOGGER.error("Error getting the list of available trees for the realm {}", realm, e);
        }

        return choices;
    }

    private String treeDisplayName(Tree tree, Locale locale) {
        boolean enabled = tree.isEnabled();
        String disabled = AMResourceBundleCache.getInstance().getResBundle("amConsole", locale)
                .getString("label.disabled.tree");
        return !enabled
                ? MessageFormat.format(disabled, tree.getName())
                : tree.getName();
    }
}
