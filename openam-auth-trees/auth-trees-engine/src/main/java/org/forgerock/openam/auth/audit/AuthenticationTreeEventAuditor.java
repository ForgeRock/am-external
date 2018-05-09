/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.audit;

import static com.sun.identity.shared.Constants.TREES_AUDIT_LOGS_ENABLED;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status.FAILED;
import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status.SUCCESSFUL;
import static org.forgerock.openam.audit.AuditConstants.AUTHENTICATION_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.AUTH_LEVEL;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.IP_ADDRESS;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.TREE_NAME;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_TREE_LOGIN_COMPLETED;

import javax.inject.Inject;

import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.TreeResult;

import com.iplanet.am.util.SystemPropertiesWrapper;

/**
 * Generates the audit logs for the authentication tree outcome.
 */
public class AuthenticationTreeEventAuditor  {

    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final SystemPropertiesWrapper systemPropertiesWrapper;

    /**
     * Constructs the tree event auditor.
     *
     * @param auditEventPublisher the audit event publisher.
     * @param auditEventFactory the audit event factory.
     * @param systemPropertiesWrapper the system properties wrapper to watch if the system properties change.
     */
    @Inject
    public AuthenticationTreeEventAuditor(AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory, SystemPropertiesWrapper systemPropertiesWrapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.systemPropertiesWrapper = systemPropertiesWrapper;
    }

    /**
     * Adds a log entry in the audit logs under the authentication topic and sets the tree outcome to Failure.
     * @param result the result object returned after processing a tree.
     * @param authTree the auth tree we are logging about.
     * @param clientIp the IP Address of the client who made the request.
     */
    public void auditLoginFailure(TreeResult result, AuthTree authTree, String clientIp) {
        logAuditEvent(result, authTree, clientIp, FAILED);
    }

    /**
     * Adds a log entry in the audit logs under the authentication topic and sets the tree outcome to Success.
     * @param result the result object returned after processing a tree.
     * @param authTree the auth tree we are logging about.
     * @param clientIp the IP Address of the client who made the request.
     */
    public void auditLoginSuccess(TreeResult result, AuthTree authTree, String clientIp) {
        logAuditEvent(result, authTree, clientIp, SUCCESSFUL);
    }

    private boolean isTreeAuditLogsEnabled() {
        return systemPropertiesWrapper.getAsBoolean(TREES_AUDIT_LOGS_ENABLED, true);
    }

    private void logAuditEvent(TreeResult result, AuthTree authTree, String clientIp, Status status) {
        if (!isTreeAuditLogsEnabled()) {
            return;
        }
        String username = result.treeState.sharedState.get(SharedStateConstants.USERNAME).asString();
        String realm = result.treeState.sharedState.get(SharedStateConstants.REALM).asString();

        if (!auditEventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_TREE_LOGIN_COMPLETED)) {
            return;
        }
        AMAuthenticationAuditEventBuilder builder = auditEventFactory.authenticationEvent(realm)
                .component(AUTHENTICATION)
                .eventName(AM_TREE_LOGIN_COMPLETED)
                .result(status)
                .entry(getAuditEntryDetail(result, authTree, clientIp))
                .principal(username);

        auditEventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
    }

    private AuthenticationAuditEntry getAuditEntryDetail(TreeResult result, AuthTree authTree, String clientIp) {
        AuthenticationAuditEntry auditEntry = new AuthenticationAuditEntry();

        auditEntry.addInfo(TREE_NAME, authTree.getName());

        Integer authLevel = result.treeState.sharedState.get(SharedStateConstants.AUTH_LEVEL).asInteger();
        auditEntry.addInfo(AUTH_LEVEL, authLevel.toString());

        auditEntry.addInfo(IP_ADDRESS, clientIp);

        return auditEntry;
    }

}
