/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.DEVICE_ID;

import java.util.Map;
import java.util.Optional;

import javax.security.auth.Subject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.authentication.modules.scripted.Scripted;
import org.forgerock.openam.core.rest.devices.deviceprint.DeviceIdDao;

/**
 * Scripted Device Id (Match) Authentication module.
 *
 * @since 12.0.0
 */
public class DeviceIdMatch extends Scripted {

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        super.init(subject, sharedState, options);
        sharedState.put("_DeviceIdDao", InjectorHolder.getInstance(DeviceIdDao.class));
    }

    @Override
    protected AuthenticationAuditEntry getAuditEntryDetail() {
        AuthenticationAuditEntry entryDetail = super.getAuditEntryDetail();
        parseClientSideJson().ifPresent(json -> entryDetail.addInfoAsJson(DEVICE_ID, json));
        return entryDetail;
    }

    private Optional<String> parseClientSideJson() {
        Object json = sharedState.get(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME);
        if (json instanceof String) {
            return Optional.of(json.toString());
        } else {
            return Optional.empty();
        }
    }
}