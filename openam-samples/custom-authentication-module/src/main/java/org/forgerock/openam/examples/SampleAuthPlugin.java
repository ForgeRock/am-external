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

package org.forgerock.openam.examples;

import org.forgerock.openam.plugins.AmPlugin;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.PluginTools;

import com.google.inject.Inject;

/**
 * Plugin which installs the SampleAuth implementation and the amAuthSampleAuth.xml service schema when AM is started
 * for the first time after adding this plugin.
 */
public class SampleAuthPlugin implements AmPlugin {

    private PluginTools pluginTools;

    @Inject
    public SampleAuthPlugin(PluginTools pluginTools) {
        this.pluginTools = pluginTools;
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0";
    }

    @Override
    public void onInstall() throws PluginException {
        pluginTools.addAuthModule(SampleAuth.class,
                getClass().getClassLoader().getResourceAsStream("amAuthSampleAuth.xml"));
    }
}
