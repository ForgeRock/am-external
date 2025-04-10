/*
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
 * $Id: AgentProvider.java,v 1.1 2009/06/19 02:43:57 bigfatrat Exp $
 *
 * Portions Copyrighted 2011-2025 Ping Identity Corporation.
 */
package com.sun.identity.plugin.monitoring.impl;

import org.forgerock.guice.core.InjectorHolder;

import com.sun.identity.monitoring.MonitoringServices;
import com.sun.identity.monitoring.MonitoringStatusService;
import com.sun.identity.plugin.monitoring.FedMonAgent;

/**
 *  This class is the AM implementation of the Monitoring interface
 */

public class AgentProvider implements FedMonAgent {

    private final MonitoringServices monitoringServices;
    private final MonitoringStatusService monitoringStatusService;

    public AgentProvider() {
        this.monitoringServices = InjectorHolder.getInstance(MonitoringServices.class);
        this.monitoringStatusService = InjectorHolder.getInstance(MonitoringStatusService.class);
    }

    public void init() {
    }

    /**
     *  Returns whether agent is "running" or not
     */
    public boolean isRunning() {
        return monitoringStatusService.isRunning();
    }

    /*
     *  Returns the pointer to the SAML2 service mbean
     */
    public Object getSaml2SvcMBean() {
        return monitoringServices.getSaml2SvcMBean();
    }

    /*
     *  Returns the pointer to the Fed COTs mbean
     */
    public Object getFedCOTsMBean() {
        return monitoringServices.getFedCOTsMBean();
    }

    /*
     *  Returns the pointer to the Federation Entities mbean
     */
    public Object getFedEntsMBean() {
        return monitoringServices.getFedEntsMBean();
    }
}

