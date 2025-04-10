/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FedMonSAML2SvcProvider.java,v 1.3 2009/12/07 19:11:32 bigfatrat Exp $
 *
 * Portions Copyrighted 2017-2025 Ping Identity Corporation.
 */

/*
 * Portions Copyrighted 2011-2025 Ping Identity Corporation
 */
package com.sun.identity.plugin.monitoring.impl;

import org.forgerock.guice.core.InjectorHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.monitoring.MonitoringServices;
import com.sun.identity.monitoring.Saml2MonitoringService;
import com.sun.identity.plugin.monitoring.FedMonSAML2Svc;

/**
 *  This class implements the SAML2 Monitoring
 */

public class FedMonSAML2SvcProvider implements FedMonSAML2Svc {

    private static Saml2MonitoringService sSAML2Svc;
    private static Logger debug = LoggerFactory.getLogger(FedMonSAML2SvcProvider.class);
    private final MonitoringServices monitoringServices;

    public FedMonSAML2SvcProvider() {
        this.monitoringServices = InjectorHolder.getInstance(MonitoringServices.class);
    }

    public void init() {
        sSAML2Svc = monitoringServices.getSaml2SvcMBean();
    }

    public void incFedSessionCount() {
        if (sSAML2Svc != null) {
            sSAML2Svc.incFedSessionCount();
        }
    }

    public void decFedSessionCount() {
        if (sSAML2Svc != null) {
            sSAML2Svc.decFedSessionCount();
        }
    }

    public void setFedSessionCount(long count) {
        if (sSAML2Svc != null) {
            sSAML2Svc.setFedSessionCount(count);
	} else {
	    debug.error(
		"FedMonSAML2SvcProvider:setFedSessionCount: sSAML2Svc = null");
        }
    }

    public void incIdpSessionCount() {
        if (sSAML2Svc != null) {
            sSAML2Svc.incIdpSessionCount();
        }
    }

    public void decIdpSessionCount() {
        if (sSAML2Svc != null) {
            sSAML2Svc.decIdpSessionCount();
        }
    }

    public void setIdpSessionCount(long count) {
        if (sSAML2Svc != null) {
            sSAML2Svc.setIdpSessionCount(count);
        } else {
	        debug.error("FedMonSAML2SvcProvider:setIdpSessionCount: sSAML2Svc = null");
        }
    }

    /**
     *  increment number of
     *  @param realm name of the IDP's realm
     *  @param idpName the name of the IDP
     *  @param counter the IDP counter to increment
     */
    public void incIDPCounter (String realm, String idpName, String counter) {
        if (sSAML2Svc != null) {
            sSAML2Svc.incIDPCounter(realm, idpName, counter);
        }
    }

    /**
     *  decrement number of Assertions or Artifacts in the
     *  SAML2 Caches.
     *  @param realm name of the IDP's realm
     *  @param idpName the name of the IDP
     *  @param counter the IDP counter to decrement
     */
    public void decIDPCounter (String realm, String idpName, String counter) {
        if (sSAML2Svc != null) {
            sSAML2Svc.decIDPCounter(realm, idpName, counter);
        }
    }
}
