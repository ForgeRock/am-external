/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
