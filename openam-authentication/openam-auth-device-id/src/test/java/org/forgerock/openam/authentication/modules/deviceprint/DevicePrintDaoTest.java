/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class DevicePrintDaoTest {

    private DevicePrintDao devicePrintDao;

    @BeforeMethod
    public void setUp() {
        devicePrintDao = new DevicePrintDao();
    }

    @Test
    public void shouldGetProfiles() throws IOException, IdRepoException, SSOException {

        //Given
        AMIdentityWrapper amIdentity = mock(AMIdentityWrapper.class);

        Set ldapProfiles = Collections.singleton("{}");

        given(amIdentity.getAttribute("devicePrintProfiles")).willReturn(ldapProfiles);

        //When
        List<Map<String, Object>> profiles = devicePrintDao.getProfiles(amIdentity);

        //Then
        assertThat(profiles).hasSize(1);
    }

    @Test
    public void shouldSaveProfiles() throws IOException, IdRepoException, SSOException {

        //Given
        AMIdentityWrapper amIdentity = mock(AMIdentityWrapper.class);

        List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
        Map<String, Object> profileOne = new HashMap<String, Object>();
        profileOne.put("uuid", "UUID1");
        profileOne.put("lastSelectedDate", new Date(newDate().getTime() - 172800));
        Map<String, Object> profileTwo = new HashMap<String, Object>();
        profileTwo.put("uuid", "UUID2");
        profileTwo.put("lastSelectedDate", new Date(newDate().getTime() - 86400));
        profiles.add(profileOne);
        profiles.add(profileTwo);

        //When
        devicePrintDao.saveProfiles(amIdentity, profiles);

        //Then
        ArgumentCaptor<Map> ldapProfilesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(amIdentity).setAttributes(ldapProfilesCaptor.capture());
        verify(amIdentity).store();
        Map ldapProfiles = ldapProfilesCaptor.getValue();
        assertThat(((Set) ldapProfiles.get("devicePrintProfiles"))).hasSize(2);
    }
}
