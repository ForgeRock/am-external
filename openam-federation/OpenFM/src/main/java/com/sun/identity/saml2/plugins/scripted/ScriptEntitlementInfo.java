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
 * Copyright 2021-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.openam.annotations.EvolvingAll;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementInfo;

/**
 * This class wraps around an {@link EntitlementInfo} object for consumption in scripts. This prevents the script from
 * calling into methods that are not exposed by the EntitlementInfo interface but may be available in the concrete
 * class.
 */
@EvolvingAll
public class ScriptEntitlementInfo implements EntitlementInfo {
    private EntitlementInfo wrappedEntitlementInfo;

    /**
     * Create a new ScriptEntitlementInfo that wraps the provided {@link EntitlementInfo}.
     * @param entitlementInfo the entitlement info object to be wrapped
     */
    public ScriptEntitlementInfo(EntitlementInfo entitlementInfo) {
        this.wrappedEntitlementInfo = entitlementInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return wrappedEntitlementInfo.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getResourceNames() {
        return wrappedEntitlementInfo.getResourceNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceName() {
        return wrappedEntitlementInfo.getResourceName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApplicationName() {
        return wrappedEntitlementInfo.getApplicationName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getActionValue(String name) {
        return wrappedEntitlementInfo.getActionValue(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Boolean> getActionValues() {
        return wrappedEntitlementInfo.getActionValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Object> getActionValues(String name) {
        return wrappedEntitlementInfo.getActionValues(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Set<String>> getAdvices() {
        return wrappedEntitlementInfo.getAdvices();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAdvice() {
        return wrappedEntitlementInfo.hasAdvice();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Set<String>> getAttributes() {
        return wrappedEntitlementInfo.getAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTTL() {
        return wrappedEntitlementInfo.getTTL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJSONObject() throws JSONException {
        return wrappedEntitlementInfo.toJSONObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Application getApplication(Subject adminSubject, String realm) throws EntitlementException {
        return wrappedEntitlementInfo.getApplication(adminSubject, realm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getRequestedResourceNames() {
        return wrappedEntitlementInfo.getRequestedResourceNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestedResourceName() {
        return wrappedEntitlementInfo.getRequestedResourceName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScriptEntitlementInfo that = (ScriptEntitlementInfo) o;
        return wrappedEntitlementInfo.equals(that.wrappedEntitlementInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(wrappedEntitlementInfo);
    }
}
