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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.federation.util;

import org.apache.xml.security.Init;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML Security utility class.
 */
public final class XmlSecurity {

    // XML Santuario configuration
    private static final String IGNORE_LINE_BREAKS = "org.apache.xml.security.ignoreLineBreaks";
    private static final String IGNORE_LINE_BREAKS_DEFVAL = "true";
    private static final String XML_SECURITY_RESOURCE_CONFIG = "org.apache.xml.security.resource.config";
    private static final String XML_SECURITY_CONFIG_XML = "/xml-security-config.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlSecurity.class);
    private static volatile boolean initialized = false;

    private XmlSecurity() {
    }

    /**
     * Initialize the XML Santuario library.
     *
     * This method initialize the XML security library with default for
     * org.apache.xml.security.ignoreLineBreaks and org.apache.xml.security.resource.config
     * if they are not already set/overridden in the JVM system property by the user.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        synchronized (XmlSecurity.class) {
            if (initialized) {
                return;
            }
            String lineBreakValue;
            String prop = System.getProperty(IGNORE_LINE_BREAKS);
            if (prop == null) {
                System.setProperty(IGNORE_LINE_BREAKS, IGNORE_LINE_BREAKS_DEFVAL);
                lineBreakValue = IGNORE_LINE_BREAKS_DEFVAL;
            } else {
                lineBreakValue = prop;
            }
            LOGGER.info("Property {} set to '{}'", IGNORE_LINE_BREAKS, lineBreakValue);
            String xmlSecCfg;
            prop = System.getProperty(XML_SECURITY_RESOURCE_CONFIG);
            if (prop == null) {
                System.setProperty(XML_SECURITY_RESOURCE_CONFIG, XML_SECURITY_CONFIG_XML);
                xmlSecCfg = XML_SECURITY_CONFIG_XML;
            }
            xmlSecCfg = prop;
            LOGGER.info("Property {} set to '{}'", XML_SECURITY_RESOURCE_CONFIG, xmlSecCfg);
            Init.init();
            initialized = Init.isInitialized();
        }
    }
}
