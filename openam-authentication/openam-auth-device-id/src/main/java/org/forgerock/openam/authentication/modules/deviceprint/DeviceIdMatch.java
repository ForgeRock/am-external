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
 * Copyright 2014-2025 Ping Identity Corporation.
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
