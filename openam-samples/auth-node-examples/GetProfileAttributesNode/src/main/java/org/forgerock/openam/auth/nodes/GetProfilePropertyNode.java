/*
 * jon.knight@forgerock.com
 *
 * Gets user profile attributes 
 *
 */

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

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.*;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.CollectionUtils;

import javax.inject.Inject;
import java.util.*;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.AUTH_LEVEL;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

/**
 * A node which contributes a configurable set of properties to be added to the user's session, if/when it is created.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = GetProfilePropertyNode.Config.class)
public class GetProfilePropertyNode extends SingleOutcomeNode {

    private final static String DEBUG_FILE = "GetProfilePropertyNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    private final CoreWrapper coreWrapper;


    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * A map of property name to value.
         * @return a map of properties.
         */
        @Attribute(order = 100)
        Map<String, String> properties();
    }

    private final Config config;

    /**
     * Constructs a new GetSessionPropertiesNode instance.
     * @param config Node configuration.
     */
    @Inject
    public GetProfilePropertyNode(@Assisted Config config, CoreWrapper coreWrapper) {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) {

        debug.message("[" + DEBUG_FILE + "]: " + "Starting");

        AMIdentity userIdentity = coreWrapper.getIdentity(context.sharedState.get(USERNAME).asString(),context.sharedState.get(REALM).asString());

        JsonValue newSharedState = context.sharedState.copy();

        Set<String> configKeys = config.properties().keySet();
        
        for (String key: configKeys) {

            debug.message("[" + DEBUG_FILE + "]: Looking for profile attribute " + key);

            try {

                Set<String> idAttrs = userIdentity.getAttribute(key);

               

                if (idAttrs == null || idAttrs.isEmpty()) {

                    debug.error("[" + DEBUG_FILE + "]: " + "Unable to find attribute value for: " + key);
                    

                } else {

                    debug.error("[" + DEBUG_FILE + "]: " + "Found attribute value for: " + key);
                    String attr = idAttrs.iterator().next();
                    newSharedState.put(config.properties().get(key), attr);
                    debug.error("[" + DEBUG_FILE + "]: " + "sharedState : " + newSharedState);

                }
            } catch (IdRepoException e) {

                debug.error("[" + DEBUG_FILE + "]: " + " Error storing profile atttibute '{}' ", e);

            } catch (SSOException e) {

                debug.error("[" + DEBUG_FILE + "]: " + "Node exception", e);
            }
        }

        return goToNext().replaceSharedState(newSharedState).build();
    }

}
