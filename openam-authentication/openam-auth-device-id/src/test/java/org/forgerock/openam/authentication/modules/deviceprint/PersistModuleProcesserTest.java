/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave.NAME_PROFILE_STATE;
import static org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave.SAVE_PROFILE_STATE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class PersistModuleProcesserTest {

    private PersistModuleProcessor processor;

    private Map<String, Object> devicePrintProfile;
    private ProfilePersister profilePersister;

    @BeforeMethod
    public void setUp() {
        profilePersister = mock(ProfilePersister.class);
        devicePrintProfile = new HashMap<String, Object>();
    }

    @Test
    public void shouldReturnLoginSucceedWhenDevicePrintProfileIsNull() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(null, false, profilePersister);

        Callback[] callbacks = new Callback[]{};
        int state = ISAuthConstants.LOGIN_START;

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verifyZeroInteractions(profilePersister);
    }

    @Test
    public void shouldReturnLoginSucceedWhenDevicePrintProfileIsEmpty() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        Callback[] callbacks = new Callback[]{};
        int state = ISAuthConstants.LOGIN_START;

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verifyZeroInteractions(profilePersister);
    }

    @Test
    public void shouldReturnLoginSucceedAfterSavingProfileWhenDevicePrintNotEmptyAndAutoSaveConfigured()
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
    public void shouldReturnSaveProfileStateWhenDevicePrintNotEmptyAndAutoSaveNotConfigured()
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
        verifyZeroInteractions(profilePersister);
    }

    @Test
    public void shouldReturnNameProfileStateWhenUserChooseToSaveProfile() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);
        Callback[] callbacks = new Callback[]{choiceCallback};
        int state = SAVE_PROFILE_STATE;

        given(choiceCallback.getSelectedIndexes()).willReturn(new int[]{0});

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(NAME_PROFILE_STATE);
        verifyZeroInteractions(profilePersister);
    }

    @Test
    public void shouldReturnLoginSucceedWithoutSavingProfileWhenUserChooseToNotSaveProfile() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);
        Callback[] callbacks = new Callback[]{choiceCallback};
        int state = SAVE_PROFILE_STATE;

        given(choiceCallback.getSelectedIndexes()).willReturn(new int[]{1});

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verifyZeroInteractions(profilePersister);
    }

    @Test
    public void shouldReturnLoginSucceedAfterSavingProfileWhenUserHasNamedProfile() throws AuthLoginException {

        //Given
        processor = new PersistModuleProcessor(devicePrintProfile, false, profilePersister);

        NameCallback nameCallback = mock(NameCallback.class);
        Callback[] callbacks = new Callback[]{nameCallback};
        int state = NAME_PROFILE_STATE;

        given(nameCallback.getName()).willReturn("NAME");

        //When
        int newState = processor.process(callbacks, state);

        //Then
        assertThat(newState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        verify(profilePersister).saveDevicePrint("NAME", devicePrintProfile);
    }
}
