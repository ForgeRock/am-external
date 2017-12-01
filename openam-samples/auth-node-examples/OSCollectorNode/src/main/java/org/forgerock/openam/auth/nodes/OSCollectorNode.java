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
 * Main node file - pulls in OS details from the presented user-agent
 *
 */

//package org.forgerock.openam.auth.nodes;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Node.Metadata(outcomeProvider = OSCollectorNode.OutcomeProvider.class,
        configClass = OSCollectorNode.Config.class)
public class OSCollectorNode implements Node {


    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Configuration for the node.
     */

    public interface Config {

    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public OSCollectorNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
	
	//Pull user-agent out of headers
	List<String> userAgent = context.request.headers.get("User-Agent");
	logger.info("OSCollectorNode user-agent found: " + userAgent);

	//If no user-agent present at all        
	if (userAgent.size() != 1) {
	
		logger.info("OSCollectorNode no user-agent found");
                return goTo("Other").build();
        }

	//Take first entry of [] to make string and see what it contains
	//Probably need to migrate this to a switch if it gets too messy
	if(userAgent.get(0).contains("Linux")){
       		
		logger.info("OSCollectorNode Linux machine detected");
		return goTo("Linux").build();

	} else if (userAgent.get(0).contains("Windows")) {

		logger.info("OSCollectorNode Windows machine detected");
		return goTo("Windows").build();

	} else if (userAgent.get(0).contains("iPhone")) {

		logger.info("OSCollectorNode iPhone detected");
		return goTo("iPhone").build();

	} else if (userAgent.get(0).contains("Android")) {

		logger.info("OSCollectorNode Android detected");
		return goTo("Android").build();

	} else if (userAgent.get(0).contains("IEMobile")) {

		logger.info("OSCollectorNode Windows phone detected");
		return goTo("WinPhone").build();

	} else if (userAgent.get(0).contains("Macintosh")) {

		logger.info("OSCollectorNode Macintosh machine detected");
		return goTo("Macintosh").build();

	} 
	
	//All other OS's spin to other
	return goTo("Other").build();

    }

    private Action.ActionBuilder goTo(String outcome) {
        return Action.goTo(outcome);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = OSCollectorNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
            new Outcome("Linux", bundle.getString("Linux")),
		    new Outcome("Windows", bundle.getString("Windows")),
		    new Outcome("iPhone", bundle.getString("iPhone")),
		    new Outcome("Android", bundle.getString("Android")),
		    new Outcome("WinPhone", bundle.getString("WinPhone")),	
            new Outcome("Macintosh", bundle.getString("Macintosh")),
            new Outcome("Other", bundle.getString("Other")));
        }
    }
}
