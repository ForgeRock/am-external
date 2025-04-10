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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SHARED_STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.TRANSIENT_STATE_IDENTIFIER;

import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;

/**
 * Script bindings for the DeviceMatchNode script.
 */
public final class DeviceMatchNodeBindings extends BaseScriptedDecisionNodeBindings
        implements LegacyScriptBindings {

    private static final String DEVICE_PROFILES_DAO_IDENTIFIER = "deviceProfilesDao";
    private final DeviceProfilesDao deviceProfilesDao;
    private final Object sharedState;
    private final Object transientState;

    private DeviceMatchNodeBindings(Builder builder) {
        super(builder);
        this.deviceProfilesDao = builder.deviceProfilesDao;
        this.sharedState = builder.sharedState;
        this.transientState = builder.transientState;
    }

    /**
     * Static method to get the builder object.
     *
     * @return The first step of the Builder.
     */
    public static DeviceMatchNodeBindingsStep1 builder() {
        return new Builder();
    }

    @Override
    public BindingsMap legacyBindings() {
        BindingsMap bindings = new BindingsMap();
        bindings.put(SHARED_STATE_IDENTIFIER, sharedState);
        bindings.put(TRANSIENT_STATE_IDENTIFIER, transientState);
        bindings.put(STATE_IDENTIFIER, nodeState);
        bindings.put(CALLBACKS_IDENTIFIER, callbacks);
        bindings.put(DEVICE_PROFILES_DAO_IDENTIFIER, deviceProfilesDao);
        return bindings;
    }

    @Override
    public BindingsMap nextGenBindings() {
        var bindings =  new BindingsMap(commonNextGenBindings());
        bindings.put(DEVICE_PROFILES_DAO_IDENTIFIER, new DeviceProfilesDaoScriptWrapper(deviceProfilesDao));
        return bindings;
    }

    /**
     * Step 1 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep1 {
        /**
         * Sets the {@link DeviceProfilesDao}.
         *
         * @param deviceProfilesDao the {@link DeviceProfilesDao}
         * @return the next step of the {@link Builder}
         */
        DeviceMatchNodeBindingsStep2 withDeviceProfilesDao(DeviceProfilesDao deviceProfilesDao);
    }

    /**
     * Step 2 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep2 {
        /**
         * Sets shared state.
         *
         * @param sharedState the shared state
         * @return the next step of the {@link Builder}
         */
        DeviceMatchNodeBindingsStep3 withSharedState(Object sharedState);
    }

    /**
     * Step 3 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep3 {
        /**
         * Sets transient state.
         *
         * @param transientState the transient state
         * @return the next step of the {@link Builder}
         */
        BaseScriptedDecisionNodeBindingsStep1<DeviceMatchNodeBindings> withTransientState(Object transientState);
    }

    /**
     * Builder object to construct a {@link DeviceMatchNodeBindings}.
     * Before modifying this builder, or creating a new one, please read
     * service-component-api/scripting-api/src/main/java/org/forgerock/openam/scripting/domain/README.md
     */
    private static final class Builder extends BaseScriptedDecisionNodeBindings.Builder<DeviceMatchNodeBindings>
            implements DeviceMatchNodeBindingsStep1, DeviceMatchNodeBindingsStep2, DeviceMatchNodeBindingsStep3 {

        private DeviceProfilesDao deviceProfilesDao;
        private Object sharedState;
        private Object transientState;

        /**
         * Set the {@link DeviceProfilesDao}.
         *
         * @param deviceProfilesDao The {@link DeviceProfilesDao}.
         * @return The next step of the Builder.
         */
        @Override
        public DeviceMatchNodeBindingsStep2 withDeviceProfilesDao(DeviceProfilesDao deviceProfilesDao) {
            this.deviceProfilesDao = deviceProfilesDao;
            return this;
        }

        /**
         * Set the sharedState.
         *
         * @param sharedState the sharedState.
         * @return The next step of the Builder.
         */
        @Override
        public DeviceMatchNodeBindingsStep3 withSharedState(Object sharedState) {
            this.sharedState = sharedState;
            return this;
        }

        /**
         * Set the transientState.
         *
         * @param transientState the sharedState.
         * @return The next step of the Builder.
         */
        @Override
        public BaseScriptedDecisionNodeBindingsStep1<DeviceMatchNodeBindings> withTransientState(
                Object transientState) {
            this.transientState = transientState;
            return this;
        }

        /**
         * Builds the {@link DeviceMatchNodeBindings}.
         *
         * @return the {@link DeviceMatchNodeBindings}.
         */
        public DeviceMatchNodeBindings build() {
            return new DeviceMatchNodeBindings(this);
        }
    }
}
