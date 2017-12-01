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
/*
 * simon.moffatt@forgerock.com
 *
 * Checks incoming IP address, then compares to configured list
 */

package org.forgerock.openam.auth.nodes;

import java.util.List;
import java.util.ResourceBundle;
import javax.inject.Inject;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.util.i18n.PreferredLocales;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import java.util.*;
import org.forgerock.openam.utils.IPRange;

@Node.Metadata(outcomeProvider = IPAddressDecisionNode.OutcomeProvider.class,
        configClass = IPAddressDecisionNode.Config.class)
public class IPAddressDecisionNode implements Node {


    private final static String DEBUG_FILE = "IPAddressDecisionNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    /**
     * Configuration for the node.
     */

    public interface Config {

        @Attribute(order = 100)
        Map<String, String> ipList();

        @Attribute(order=200)
        default boolean blacklist() {
            return false;
        }
    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public IPAddressDecisionNode(@Assisted Config config) throws NodeProcessException {
        this.config = config; 
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
	
       String clientIp =context.request.clientIp;
	   debug.message("[" + DEBUG_FILE + "]: " + " starting");
	   debug.message("[" + DEBUG_FILE + "]: " + " client IP found as " + clientIp);

	   //If no IP's configured stop everyone
        if(config.ipList().isEmpty()) {

            debug.message("[" + DEBUG_FILE + "]: " + " No IP addresses configured in node");
            return goTo("false").build();

        } else {

            //Pull out the IP keys
            Set<String> ipListKeys = config.ipList().keySet();

            //Iterate over every IP and associated net mask
            for (String ipAddress : ipListKeys) {

                debug.message("[" + DEBUG_FILE + "]: " + " Matching against : " + ipAddress + " with mask of " + config.ipList().get(ipAddress));
                //Leverage IPRange from utils
                IPRange ipRange = new IPRange(ipAddress+config.ipList().get(ipAddress));
                //Pass in the client IP as an arg of the inRange function of the created ipRange
                boolean matchFound = ipRange.inRange(clientIp);
                debug.message("[" + DEBUG_FILE + "]: " + " match found? : " + matchFound);

                //Check to see whether running a black or white list
                String mode = config.blacklist() ? "Blacklist" : "Whitelist";

                debug.message("[" + DEBUG_FILE + "]: " + " running in " + mode + " mode");  

                //If acting as whitelist
                if(config.blacklist()==false){

                    if(matchFound){

                        return goTo("true").build();
                    }

                //If acting as a blacklist
                } else {

                    if(matchFound){

                        return goTo("false").build();
                    }
                }


            }
        }

        return goTo("false").build();
    }

    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = IPAddressDecisionNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome("true", bundle.getString("True")),
                    new Outcome("false", bundle.getString("False")));
        }
    }
}
