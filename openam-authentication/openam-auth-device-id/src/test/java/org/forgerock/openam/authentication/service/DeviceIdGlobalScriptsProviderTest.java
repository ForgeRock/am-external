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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.authentication.service.DeviceIdGlobalScript.DEVICE_ID_MATCH_CLIENT_SIDE;
import static org.forgerock.openam.authentication.service.DeviceIdGlobalScript.DEVICE_ID_MATCH_SERVER_SIDE;

import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.Script;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DeviceIdGlobalScriptsProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DeviceIdGlobalScriptsProviderTest {

    @InjectMocks
    private DeviceIdGlobalScriptsProvider provider;

    @Test
    public void shouldReturnDeviceIdGlobalScripts() throws ScriptException {
        // When
        List<Script> actual = provider.get();

        // Then
        assertThat(actual.size()).isEqualTo(2);
        assertScriptConfiguration(actual.get(0), DEVICE_ID_MATCH_SERVER_SIDE.getId(),
                DEVICE_ID_MATCH_SERVER_SIDE.getContext());
        assertScriptConfiguration(actual.get(1), DEVICE_ID_MATCH_CLIENT_SIDE.getId(),
                DEVICE_ID_MATCH_CLIENT_SIDE.getContext());
    }

    private void assertScriptConfiguration(Script script, String id, ScriptContext context) {
        assertThat(script.getId()).isEqualTo(id);
        assertThat(script.getContext()).isEqualTo(context);
        assertThat(script.getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);
    }

}