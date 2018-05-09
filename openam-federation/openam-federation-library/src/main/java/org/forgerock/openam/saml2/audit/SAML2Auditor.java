/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.saml2.audit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.FAILED;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getTrackingIdFromSSOToken;
import static org.forgerock.openam.audit.context.AuditRequestContext.USER_ID;
import static org.forgerock.openam.audit.context.AuditRequestContext.getAuditRequestContext;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;

import com.iplanet.sso.SSOToken;

/**
 * Responsible for publishing audit access events for individual SAML2 requests.  A SAML2Auditor is not thread safe
 * and a new SAML2Auditor should be used for each request.
 */
public class SAML2Auditor implements SAML2EventLogger {

    private static final String PROXY_MESSAGE = "Forwarding request to a proxy";
    private static final String LOCAL_USER_LOGIN_MESSAGE = "Forwarding request to local user login";

    private String realm;
    private String method;

    private boolean accessAttemptAudited = false;
    private long startTime;

    private final HttpServletRequest request;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private String message;

    /**
     * Constructor for SAML2Auditor
     *
     * @param auditEventPublisher The AuditEventPublisher
     * @param auditEventFactory The AuditEventFactory
     * @param request The HttpServletReqeust associated with the SAML2 request
     */
    public SAML2Auditor(final AuditEventPublisher auditEventPublisher,
                        final AuditEventFactory auditEventFactory, final HttpServletRequest request) {
        this.request = request;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.startTime = currentTimeMillis();
    }

    @Override
    public void auditAccessAttempt() {
        if (auditEventPublisher.isAuditing(
                realm, AuditConstants.ACCESS_TOPIC, AuditConstants.EventName.AM_ACCESS_ATTEMPT)) {

            AuditEvent auditEvent = getDefaultSAML2AccessAuditEventBuilder()
                    .timestamp(startTime)
                    .eventName(AuditConstants.EventName.AM_ACCESS_ATTEMPT)
                    .toEvent();
            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);
        }
        accessAttemptAudited = true;
    }

    @Override
    public void auditAccessSuccess() {
        if (!accessAttemptAudited) {
            auditAccessAttempt();
        }
        if (auditEventPublisher.isAuditing(
                realm, AuditConstants.ACCESS_TOPIC, AuditConstants.EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = currentTimeMillis();
            final long elapsedTime = endTime - startTime;

            AuditEvent auditEvent = getDefaultSAML2AccessAuditEventBuilder()
                    .timestamp(endTime)
                    .eventName(AuditConstants.EventName.AM_ACCESS_OUTCOME)
                    .response(SUCCESSFUL, message, elapsedTime, MILLISECONDS)
                    .toEvent();
            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);
        }
    }

    @Override
    public void auditAccessFailure(String errorCode, String message) {
        if (!accessAttemptAudited) {
            auditAccessAttempt();
        }
        if (auditEventPublisher.isAuditing(
                realm, AuditConstants.ACCESS_TOPIC, AuditConstants.EventName.AM_ACCESS_OUTCOME)) {

            final long endTime = currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            final JsonValue detail = json(object(field(AuditConstants.ACCESS_RESPONSE_DETAIL_REASON, message)));
            AuditEvent auditEvent = getDefaultSAML2AccessAuditEventBuilder()
                    .timestamp(endTime)
                    .eventName(AuditConstants.EventName.AM_ACCESS_OUTCOME)
                    .responseWithDetail(FAILED, errorCode, elapsedTime, MILLISECONDS, detail)
                    .toEvent();

            auditEventPublisher.tryPublish(AuditConstants.ACCESS_TOPIC, auditEvent);
        }
    }

    private AMAccessAuditEventBuilder getDefaultSAML2AccessAuditEventBuilder() {
        return auditEventFactory.accessEvent(realm)
                .forHttpServletRequest(request)
                .component(AuditConstants.Component.SAML2)
                .request(AuditConstants.Component.SAML2.toString(), method);
    }

    @Override
    public void setSessionTrackingId(String trackingId) {
        getAuditRequestContext().addTrackingId(trackingId);
    }

    @Override
    public void setUserId(String userId) {
        getAuditRequestContext().putProperty(USER_ID, userId);
    }

    @Override
    public void setRealm(String realm) {
        this.realm = isEmpty(realm) ? null : realm;
    }

    @Override
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public void auditForwardToProxy() {
        this.message = PROXY_MESSAGE;
        auditAccessSuccess();
    }

    @Override
    public void auditForwardToLocalUserLogin() {
        this.message = LOCAL_USER_LOGIN_MESSAGE;
        auditAccessSuccess();
    }

    @Override
    public void setRequestId(String authnRequestId) {
        getAuditRequestContext().addTrackingId(authnRequestId);
    }

    @Override
    public void setSSOTokenId(Object session) {
        if (session instanceof SSOToken) {
            getAuditRequestContext().addTrackingId(getTrackingIdFromSSOToken((SSOToken) session));
        }
    }

    @Override
    public void setAuthTokenId(Object session) {
        if (session instanceof SSOToken) {
            getAuditRequestContext().addTrackingId(getTrackingIdFromSSOToken((SSOToken) session));
        }
    }
}
