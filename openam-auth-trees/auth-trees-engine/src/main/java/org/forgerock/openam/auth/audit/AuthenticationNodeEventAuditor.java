/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.audit;

import static com.sun.identity.shared.Constants.NODE_AUDIT_LOGS_ENABLED;
import static org.forgerock.openam.audit.AuditConstants.AUTHENTICATION_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.Component.AUTHENTICATION;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.AUTH_LEVEL;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.NODE_DISPLAY_NAME;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.NODE_EXTRA_LOG;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.NODE_ID;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.NODE_OUTCOME;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.NODE_TYPE;
import static org.forgerock.openam.audit.AuditConstants.EntriesInfoFieldKey.TREE_NAME;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_NODE_LOGIN_COMPLETED;

import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.trees.engine.AuthTree;

import com.iplanet.am.util.SystemPropertiesWrapper;

/**
 * Generates the audit logs for the nodes in authentication trees.
 */
public class AuthenticationNodeEventAuditor {

    private static final String NODE_AUDIT_LOGS_ACTIVE_PROPERTY = "org.forgerock.openam.auth.audit.nodeAuditLogsActive";
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final SystemPropertiesWrapper systemPropertiesWrapper;

    /**
     * Constructs the node event auditor.
     *
     * @param auditEventPublisher the audit event publisher.
     * @param auditEventFactory the audit event factory.
     * @param systemPropertiesWrapper the system properties wrapper to watch if the system properties change.
     */
    @Inject
    public AuthenticationNodeEventAuditor(AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory, SystemPropertiesWrapper systemPropertiesWrapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.systemPropertiesWrapper = systemPropertiesWrapper;
    }

    /**
     * Adds a log entry in the audit logs under the authentication topic with the information about the node
     * being processed.
     *
     * @param tree the tree object we are logging about.
     * @param action the action object with the details of the node that is being processed.
     * @param nodeType the type of the node that is being processed.
     * @param nodeId the UUID of the node that is being processed.
     * @param sharedState the current shared state of the tree.
     * @param extraNodeLogEntry any extra information specific to the node we are processing. It can be empty if no
     *                          node specific information needs to be logged.
     */
    public void logAuditEvent(AuthTree tree, Action action, String nodeType, UUID nodeId, JsonValue sharedState,
            JsonValue extraNodeLogEntry) {
        if (!isNodeAuditLogsEnabled()) {
            return;
        }
        // Skip if we haven't finished processing the node
        if (action.sendingCallbacks()) {
            return;
        }

        String username = "";
        String realm = "";
        JsonValue treeSharedState = getSharedState(sharedState);

        if (treeSharedState != null) {
            username = treeSharedState.get(SharedStateConstants.USERNAME).asString();
            realm = treeSharedState.get(SharedStateConstants.REALM).asString();
        }
        if (!auditEventPublisher.isAuditing(realm, AUTHENTICATION_TOPIC, AM_NODE_LOGIN_COMPLETED)) {
            return;
        }
        AuthenticationAuditEntry nodeLogEntry = getAuditEntryDetail(tree, nodeType, nodeId, treeSharedState,
                action, extraNodeLogEntry);

        AMAuthenticationAuditEventBuilder builder = auditEventFactory.authenticationEvent(realm)
                .component(AUTHENTICATION)
                .eventName(AM_NODE_LOGIN_COMPLETED)
                .entry(nodeLogEntry)
                .principal(username);

        auditEventPublisher.tryPublish(AUTHENTICATION_TOPIC, builder.toEvent());
    }

    /**
     * This method is needed to support inner trees. If the node producing the audit logs is the InnerTreeEvaluatorNode,
     * it will have the shared state of the parent tree nested inside the shared state of the node (the one we see here)
     * - that means that we need to get the inner shared state to access what we need (username and password).
     * Once we go to the next node, inside the inner tree, the outer shared state and the inner shared state will be
     * flatted out and combined into a single view (a combined shared state with no inner shared states), and that is
     * what the nodes inside the tree will see - that means that having inner shared states will ONLY happen while we
     * are in the InnerTreeEvaluatorNode, but we need to account for this special case, or we will see nasty null
     * pointer exceptions we don't want to see.
     */
    private JsonValue getSharedState(JsonValue outerSharedState) {
        JsonValue innerSharedState = outerSharedState;

        while (innerSharedState != null && innerSharedState.asMap().containsKey("sharedState")) {
            innerSharedState = innerSharedState.get("sharedState");
        }
        return innerSharedState;
    }

    private boolean isNodeAuditLogsEnabled() {
        return systemPropertiesWrapper.getAsBoolean(NODE_AUDIT_LOGS_ENABLED, true);
    }

    private AuthenticationAuditEntry getAuditEntryDetail(AuthTree tree, String nodeType, UUID nodeId,
            JsonValue sharedState, Action action, JsonValue extraNodeLogEntry) {
        String treeName = tree.getName();
        String displayName = tree.getDisplayNameForId(nodeId);
        AuthenticationAuditEntry auditEntry = new AuthenticationAuditEntry();

        auditEntry.addInfo(TREE_NAME, treeName);

        auditEntry.addInfo(NODE_TYPE, nodeType);

        auditEntry.addInfo(NODE_ID, nodeId.toString());

        auditEntry.addInfo(NODE_DISPLAY_NAME, displayName);

        Integer authLevel = sharedState.get(SharedStateConstants.AUTH_LEVEL).asInteger();
        auditEntry.addInfo(AUTH_LEVEL, authLevel.toString());

        if (action.outcome != null) {
            auditEntry.addInfo(NODE_OUTCOME, action.outcome);
        }

        if (!extraNodeLogEntry.asMap().isEmpty()) {
            auditEntry.addInfoAsJson(NODE_EXTRA_LOG, extraNodeLogEntry);
        }

        return auditEntry;
    }
}
