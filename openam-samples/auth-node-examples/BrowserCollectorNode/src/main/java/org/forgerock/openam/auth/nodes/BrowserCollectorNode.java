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

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;

import com.google.inject.assistedinject.Assisted;

//smoff
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** simon.moffatt@forgerock.com 10/08/17 - A node that checks to see what browser a user is logging in from based on user-agent */
@Node.Metadata(outcomeProvider = BrowserCollectorNode.OutcomeProvider.class,
        configClass = BrowserCollectorNode.Config.class)
public class BrowserCollectorNode implements Node {

    //smoff

    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The name of the HTTP header we want to look for.
         * @return the name.
         */
        //@Attribute(order = 100)
        //default String cookieName() {
        //    return "My-Cookie";
        // }

    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public BrowserCollectorNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
	
	//Pull user-agent out of headers
	List<String> userAgent = context.request.headers.get("User-Agent");
	logger.info("BrowserCollectorNode user-agent found: " + userAgent);

	//If no user-agent present at all        
	if (userAgent.size() != 1) {
	
		logger.info("BrowserCollectorNode no user-agent found");
                return goTo("Other").build();
        }

	//Take first entry of [] to make string and see what it contains
	//Probably need to migrate this to a switch if it gets too messy
	if(userAgent.get(0).contains("Chrome")){
       		
		logger.info("BrowserCollectorNode Chrome browser detected");
		return goTo("Chrome").build();

	} else if (userAgent.get(0).contains("Opera")) {

		logger.info("BrowserCollectorNode Opera browser detected");
		return goTo("Opera").build();

	} else if (userAgent.get(0).contains("Firefox")) {

		logger.info("BrowserCollectorNode Firefox browser detected");
		return goTo("Firefox").build();

	} else if (userAgent.get(0).contains("Windows")) {

		logger.info("BrowserCollectorNode Internet Explorer detected");
		return goTo("IE").build();

	} else if (userAgent.get(0).contains("Safari")) {

		logger.info("BrowserCollectorNode Safari browser detected");
		return goTo("Safari").build();

	}
	
	//All other OS's spin to other
	return goTo("Other").build();

    }

    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = BrowserCollectorNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome("Chrome", bundle.getString("Chrome")),
		    new Outcome("Opera", bundle.getString("Opera")),
		    new Outcome("Firefox", bundle.getString("Firefox")),
		    new Outcome("IE", bundle.getString("IE")),
		    new Outcome("Other", bundle.getString("Other")),
                    new Outcome("Safari", bundle.getString("Safari")));
        }
    }
}
