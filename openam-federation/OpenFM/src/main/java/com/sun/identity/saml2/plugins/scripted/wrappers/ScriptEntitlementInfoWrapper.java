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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted.wrappers;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementInfo;
import org.forgerock.openam.annotations.SupportedAll;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class wraps around an {@link EntitlementInfo} object for consumption in scripts. This prevents the script from
 * calling into methods that are not exposed by the EntitlementInfo interface but may be available in the concrete
 * class.
 */
@SupportedAll(scriptingApi = true)
public class ScriptEntitlementInfoWrapper {
    private EntitlementInfo wrappedEntitlementInfo;

    /**
     * Create a new ScriptEntitlementInfoWrapper that wraps the provided {@link EntitlementInfo}.
     * @param entitlementInfo the entitlement info object to be wrapped
     */
    public ScriptEntitlementInfoWrapper(EntitlementInfo entitlementInfo) {
        this.wrappedEntitlementInfo = entitlementInfo;
    }

    /**
     * Returns the name of the entitlement.
     *
     * @return the name of the entitlement
     */
    public String getName() {
        return wrappedEntitlementInfo.getName();
    }

    /**
     * Returns resource names.
     *
     * @return resource names.
     */
    public List<String> getResourceNames() {
        Set<String> resourceNames = wrappedEntitlementInfo.getResourceNames();
        return (resourceNames == null) ? null : new ArrayList<>(wrappedEntitlementInfo.getResourceNames());
    }

    /**
     * Returns resource name.
     *
     * @return resource names.
     */
    public String getResourceName() {
        return wrappedEntitlementInfo.getResourceName();
    }

    /**
     * Returns application name.
     *
     * @return application name.
     */
    public String getApplicationName() {
        return wrappedEntitlementInfo.getApplicationName();
    }

    /**
     * Returns action value.
     *
     * @param name Name of the action.
     * @return action values.
     */
    public Boolean getActionValue(String name) {
        return wrappedEntitlementInfo.getActionValue(name);
    }

    /**
     * Returns action values.
     *
     * @return action values.
     */
    public Map<String, Boolean> getActionValues() {
        return wrappedEntitlementInfo.getActionValues();
    }

    /**
     * Returns action values.
     *
     * @param name Name of the action.
     * @return action values.
     */
    public List<Object> getActionValues(String name) {
        Set<Object> actionValues = wrappedEntitlementInfo.getActionValues(name);
        return actionValues == null ? null : new ArrayList<>(wrappedEntitlementInfo.getActionValues(name));
    }

    /**
     * Returns advices.
     *
     * @return Advices.
     */
    public Map<String, List<String>> getAdvices() {
        Map<String, Set<String>> advices = wrappedEntitlementInfo.getAdvices();
        return advices == null ? null : wrappedEntitlementInfo.getAdvices()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> new ArrayList<>(entry.getValue())));
    }

    /**
     * Check whether this entitlement has advice.
     *
     * @return Whether this entitlement has any advice.
     */
    public boolean hasAdvice() {
        return wrappedEntitlementInfo.hasAdvice();
    }

    /**
     * Returns attributes.
     *
     * @return Attributes.
     */
    public Map<String, List<String>> getAttributes() {
        Map<String, Set<String>> attributes = wrappedEntitlementInfo.getAttributes();
        return attributes == null ? null : wrappedEntitlementInfo.getAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> new ArrayList<>(entry.getValue())));
    }

    /**
     * Returns the TTL.
     *
     * @return The TTL in ms
     */
    public long getTTL() {
        return wrappedEntitlementInfo.getTTL();
    }

    /**
     * Returns JSONObject mapping of  the object.
     *
     * @return JSONObject mapping of  the object
     * @throws JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        return wrappedEntitlementInfo.toJSONObject();
    }

    /**
     * Returns application for this entitlement.
     *
     * @param adminSubject Admin Subject.
     * @param realm        Realm Name
     * @return application for this entitlement.
     */
    public Application getApplication(Subject adminSubject, String realm) throws EntitlementException {
        return wrappedEntitlementInfo.getApplication(adminSubject, realm);
    }

    /**
     * Returns non normalised resource names. If this has not been set the resource names (which is most
     * probably normalised) will be returned.
     *
     * @return Non normalised resource names or if that has not been set, the normalised resource names.
     */
    public List<String> getRequestedResourceNames() {
        Set<String> requestedResourceNames = wrappedEntitlementInfo.getRequestedResourceNames();
        return requestedResourceNames == null ? null :
                new ArrayList<>(wrappedEntitlementInfo.getRequestedResourceNames());
    }

    /**
     * Returns non normalised resource name. If this has not been set the resource name (which is most
     * probably normalised) will be returned.
     *
     * @return Non normalised resource name or if that has not been set, the normalised resource name.
     */
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
        ScriptEntitlementInfoWrapper that = (ScriptEntitlementInfoWrapper) o;
        return wrappedEntitlementInfo.equals(that.wrappedEntitlementInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return wrappedEntitlementInfo.hashCode();
    }
}
