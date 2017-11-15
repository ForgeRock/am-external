/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.DEVICE_ID;

import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.authentication.modules.scripted.Scripted;

/**
 * Scripted Device Id (Match) Authentication module.
 *
 * @since 12.0.0
 */
public class DeviceIdMatch extends Scripted {

    @Override
    protected AuthenticationAuditEntry getAuditEntryDetail() {
        AuthenticationAuditEntry entryDetail = super.getAuditEntryDetail();

        Object deviceId = sharedState.get(CLIENT_SCRIPT_OUTPUT_DATA_VARIABLE_NAME);
        if (deviceId != null && deviceId instanceof String) {
            entryDetail.addInfo(DEVICE_ID, (String) deviceId);
        }

        return entryDetail;
    }
}
