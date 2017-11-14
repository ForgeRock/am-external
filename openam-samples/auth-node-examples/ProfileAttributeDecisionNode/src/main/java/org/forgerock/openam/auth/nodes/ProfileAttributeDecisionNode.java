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
 * Copyright 2017 ForgeRock AS.
 */
// simon.moffatt@forgerock.com - retrieves profile attrbute and checks for specified value

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import java.util.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.node.api.Action.send;
import org.forgerock.openam.core.CoreWrapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.*;
import org.forgerock.openam.utils.CollectionUtils;
import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;
import javax.inject.Inject;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.util.i18n.PreferredLocales;







@Node.Metadata(outcomeProvider = ProfileAttributeDecisionNode.OutcomeProvider.class,
        configClass = ProfileAttributeDecisionNode.Config.class)
public class ProfileAttributeDecisionNode implements Node {

    private final static String DEBUG_FILE = "ProfileAttributeDecisionNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private final CoreWrapper coreWrapper;

    /**
     * Configuration for the node.
     */
    public interface Config {

        //Property to search for
        @Attribute(order = 100)
        default String profileAttribute() {
            return "Name of profile attribute";
        }

        @Attribute(order = 200)
        default String profileAttributeValue() {
            return "Value of attribute";
        }


    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public ProfileAttributeDecisionNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
   
        debug.message("[" + DEBUG_FILE + "]: " + "Starting");    

        //Pull out the user object
        AMIdentity userIdentity = coreWrapper.getIdentity(context.sharedState.get(USERNAME).asString(),context.sharedState.get(REALM).asString());

        //Pull out the specified attribute
        debug.message("[" + DEBUG_FILE + "]: Looking for profile attribute " + config.profileAttribute());

        try {

                Set<String> idAttrs = userIdentity.getAttribute(config.profileAttribute());

                if (idAttrs == null || idAttrs.isEmpty()) {

                    debug.error("[" + DEBUG_FILE + "]: " + "Unable to find attribute value for: " + config.profileAttribute());
                    

                } else {

                    String attr = idAttrs.iterator().next();
                    debug.message("[" + DEBUG_FILE + "]: " + "Found attribute value for: " + config.profileAttribute() + " as " + attr);
                    

                    //Check the attribute value found matches submitted
                    if(attr.equals(config.profileAttributeValue())) {

                        debug.message("[" + DEBUG_FILE + "]: " + "Found attribute value and matches submitted value");
                        return goTo("True").build();

                    } else {

                        debug.message("[" + DEBUG_FILE + "]: " + "Found attribute but value doesn't match submitted value");
                        return goTo("False").build();

                    }


                }
            } catch (IdRepoException e) {

                debug.error("[" + DEBUG_FILE + "]: " + " Error storing profile atttibute '{}' ", e);

            } catch (SSOException e) {

                debug.error("[" + DEBUG_FILE + "]: " + "Node exception", e);
            }

        
        //No match found outcome
        return goTo("False").build();

    }

    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = ProfileAttributeDecisionNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
            new Outcome("True", bundle.getString("True")),
            new Outcome("False", bundle.getString("False")));
        }
    }
}
