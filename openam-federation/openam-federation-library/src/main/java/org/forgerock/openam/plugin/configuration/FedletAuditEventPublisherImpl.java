/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.plugin.configuration;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventPublisher;

/**
 * No-Op AuditEventPublisher implementation for the OpenAM Fedlet.
 */
public class FedletAuditEventPublisherImpl implements AuditEventPublisher {

    @Override
    public void tryPublish(String topic, AuditEvent auditEvent) {
        // this section intentionally left blank
    }

    @Override
    public boolean isAuditing(String realm, String topic, AuditConstants.EventName eventName) {
        return false;
    }
}
