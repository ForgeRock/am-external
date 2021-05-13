/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: CacheCleanUpRunnable.java,v 1.2 2008/11/10 22:57:03 veiming Exp $
 *
 * Portions Copyrighted 2019 ForgeRock AS.
 */


package com.sun.identity.saml2.profile;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.saml2.assertion.Assertion;

/**
 * This is a helper class to clean up cache.
 */

public class CacheCleanUpRunnable extends GeneralTaskRunnable {
    private static final Logger logger = LoggerFactory.getLogger(CacheCleanUpRunnable.class);
    private long runPeriod;
    
    
    /**
     * Constructor.
     *
     * @param runPeriod The period for the clean up to run.
     */
    public CacheCleanUpRunnable(long runPeriod) {
        this.runPeriod = runPeriod;
        if (logger.isDebugEnabled()) {
            logger.debug(
                "CacheCleanUpRunnable.CacheCleanUpRunnable: runPeriod = " +
                runPeriod);
        }
    }
    
    public boolean addElement(Object obj) {
        return false;
    }
    
    public boolean removeElement(Object obj) {
        return false;
    }
    
    public boolean isEmpty() {
        return false;
    }
    
    public long getRunPeriod() {
        return runPeriod;
    }
    
    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("CacheCleanUpRunnable.run:");
        }

        synchronized(IDPCache.assertionCache) {
            for(Iterator iter = IDPCache.assertionCache.keySet().iterator();
                iter.hasNext(); ) {

                String userName = (String)iter.next();
                List assertions = (List)IDPCache.assertionCache.get(userName);
                if ((assertions == null) || (assertions.isEmpty())) {
                    continue;
                }
                for(Iterator iterA = assertions.iterator(); iterA.hasNext();) {
                    Assertion assertion = (Assertion)iterA.next();
                    if (!assertion.isTimeValid()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                "CacheCleanUpRunnable.run: remove assertion." +
                                "ID = " + assertion.getID() + ", userName = " +
                                userName);
			}
                        iterA.remove();
                        IDPCache.assertionByIDCache.remove(assertion.getID());
                    }
                }
            }
        }

    }
    
}
