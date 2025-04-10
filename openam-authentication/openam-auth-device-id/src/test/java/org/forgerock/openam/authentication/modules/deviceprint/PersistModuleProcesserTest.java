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
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave.NAME_PROFILE_STATE;
import static org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave.SAVE_PROFILE_STATE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

public class PersistModuleProcesserTest {

    private PersistModuleProcessor processor;

    private Map<String, Object> devicePrintProfile;
    private ProfilePersister profilePersister;

    @BeforeEach
    void setUp() {
        profilePersister = mock(ProfilePersister.class);
        devicePrintProfile = new HashMap<>();
    }

    @Test
    void shouldReturnLoginSucceedWhenDevicePrintProfileIsNull() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(null, false, profilePersister);

        Callback[] callbacks = new Callback[]{};
        int state = ISAuthConstants.LOGIN_START;

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verifyNoInteractions(profilePersister);
    }

    @Test
    void shouldReturnLoginSucceedWhenDevicePrintProfileIsEmpty() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        Callback[] callbacks = new Callback[]{};
        int state = ISAuthConstants.LOGIN_START;

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verifyNoInteractions(profilePersister);
    }

    @Test
    void shouldReturnLoginSucceedAfterSavingProfileWhenDevicePrintNotEmptyAndAutoSaveConfigured()
            throws AuthLoginException {

        //Given
        devicePrintProfile.put("KEY", "VALUE");

        processor = new PersistModuleProcessor(devicePrintProfile, true, profilePersister);

        Callback[] callbacks = new Callback[]{};
        int state = ISAuthConstants.LOGIN_START;

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verify(profilePersister).saveDevicePrint(devicePrintProfile);
    }

    @Test
    void shouldReturnSaveProfileStateWhenDevicePrintNotEmptyAndAutoSaveNotConfigured()
            throws AuthLoginException {

        //Given
        devicePrintProfile.put("KEY", "VALUE");

        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        Callback[] callbacks = new Callback[]{};
        int state = ISAuthConstants.LOGIN_START;

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(SAVE_PROFILE_STATE);
        verifyNoInteractions(profilePersister);
    }

    @Test
    void shouldReturnNameProfileStateWhenUserChooseToSaveProfile() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);
        Callback[] callbacks = new Callback[]{choiceCallback};

        given(choiceCallback.getSelectedIndexes()).willReturn(new int[]{0});

        //When
        int newState = processor.process(callbacks, SAVE_PROFILE_STATE);

        //Then
        assertThat(newState).isEqualTo(NAME_PROFILE_STATE);
        verifyNoInteractions(profilePersister);
    }

    @Test
    void shouldReturnLoginSucceedWithoutSavingProfileWhenUserChooseToNotSaveProfile() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);
        Callback[] callbacks = new Callback[]{choiceCallback};

        given(choiceCallback.getSelectedIndexes()).willReturn(new int[]{1});

        //When
        int newState = processor.process(callbacks, SAVE_PROFILE_STATE);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verifyNoInteractions(profilePersister);
    }

    @Test
    void shouldReturnLoginSucceedAfterSavingProfileWhenUserHasNamedProfile() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        NameCallback nameCallback = mock(NameCallback.class);
        Callback[] callbacks = new Callback[]{nameCallback};

        given(nameCallback.getName()).willReturn("NAME");

        //When
        int newState = processor.process(callbacks, NAME_PROFILE_STATE);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verify(profilePersister).saveDevicePrint("NAME", devicePrintProfile);
    }
}
