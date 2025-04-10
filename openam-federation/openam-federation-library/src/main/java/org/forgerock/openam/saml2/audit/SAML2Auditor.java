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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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

import jakarta.servlet.http.HttpServletRequest;

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
    private JsonValue auditDetails = json(object());

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
                    .requestDetail(this.auditDetails)
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
                    .requestDetail(this.auditDetails)
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

    @Override
    public void setSpEntity(String spEntity) {
        this.auditDetails.put("spEntity", spEntity);
    }

    @Override
    public void setIdpEntity(String idpEntity) {
        this.auditDetails.put("idpEntity", idpEntity);
    }

    @Override
    public void setConfiguredService(String service) {
        this.auditDetails.put("configuredService", service);
    }

}
