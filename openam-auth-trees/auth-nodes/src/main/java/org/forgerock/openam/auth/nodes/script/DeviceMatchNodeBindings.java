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
package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.CALLBACKS_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.SHARED_STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.STATE_IDENTIFIER;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.TRANSIENT_STATE_IDENTIFIER;

import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.scripting.domain.Binding;
import org.forgerock.openam.scripting.domain.ScriptBindings;

/**
 * Script bindings for the DeviceMatchNode script.
 */
public final class DeviceMatchNodeBindings extends ScriptBindings {

    private static final String DEVICE_PROFILES_DAO_IDENTIFIER = "deviceProfilesDao";
    private final NodeState nodeState;
    private final List<? extends Callback> callbacks;
    private final DeviceProfilesDao deviceProfilesDao;
    private final Object sharedState;
    private final Object transientState;

    private DeviceMatchNodeBindings(Builder builder) {
        super(builder);
        this.nodeState = builder.nodeState;
        this.callbacks = builder.callbacks;
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

    /**
     * The signature of these bindings. Used to provide information about available bindings via REST without the
     * stateful underlying objects.
     *
     * @return The signature of this ScriptBindings implementation.
     */
    public static ScriptBindings signature() {
        return new Builder().signature();
    }

    @Override
    public String getDisplayName() {
        return "Device Match Node Bindings";
    }

    @Override
    protected List<Binding> additionalV1Bindings() {
        return List.of(
                Binding.of(SHARED_STATE_IDENTIFIER, sharedState, Map.class),
                Binding.of(TRANSIENT_STATE_IDENTIFIER, transientState, Map.class),
                Binding.of(STATE_IDENTIFIER, nodeState, NodeState.class),
                Binding.of(CALLBACKS_IDENTIFIER, callbacks, List.class),
                Binding.of(DEVICE_PROFILES_DAO_IDENTIFIER, deviceProfilesDao, DeviceProfilesDao.class)
        );
    }

    /**
     * Step 1 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep1 {
        /**
         * Sets the {@link NodeState}.
         *
         * @param nodeState the node state
         * @return the next step of the {@link Builder}
         */
        DeviceMatchNodeBindingsStep2 withNodeState(NodeState nodeState);
    }

    /**
     * Step 2 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep2 {
        /**
         * Sets the callbacks.
         *
         * @param callbacks the callbacks
         * @return the next step of the {@link Builder}
         */
        DeviceMatchNodeBindingsStep3 withCallbacks(List<? extends Callback> callbacks);
    }

    /**
     * Step 3 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep3 {
        /**
         * Sets the {@link DeviceProfilesDao}.
         *
         * @param deviceProfilesDao the {@link DeviceProfilesDao}
         * @return the next step of the {@link Builder}
         */
        DeviceMatchNodeBindingsStep4 withDeviceProfilesDao(DeviceProfilesDao deviceProfilesDao);
    }

    /**
     * Step 4 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep4 {
        /**
         * Sets shared state.
         *
         * @param sharedState the shared state
         * @return the next step of the {@link Builder}
         */
        DeviceMatchNodeBindingsStep5 withSharedState(Object sharedState);
    }

    /**
     * Step 5 of the builder.
     */
    public interface DeviceMatchNodeBindingsStep5 {
        /**
         * Sets transient state.
         *
         * @param transientState the transient state
         * @return the next step of the {@link Builder}
         */
        ScriptBindingsStep1 withTransientState(Object transientState);
    }

    /**
     * Builder object to construct a {@link DeviceMatchNodeBindings}.
     */
    private static final class Builder extends ScriptBindings.Builder<Builder>
            implements DeviceMatchNodeBindingsStep1, DeviceMatchNodeBindingsStep2, DeviceMatchNodeBindingsStep3,
            DeviceMatchNodeBindingsStep4, DeviceMatchNodeBindingsStep5 {

        private NodeState nodeState;
        private List<? extends Callback> callbacks;
        private DeviceProfilesDao deviceProfilesDao;
        private Object sharedState;
        private Object transientState;

        /**
         * Set the nodeState for the builder.
         *
         * @param nodeState The nodeState.
         * @return The next step of the Builder.
         */
        @Override
        public DeviceMatchNodeBindingsStep2 withNodeState(NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * Set the callbacks for the builder.
         *
         * @param callbacks The List of callbacks.
         * @return The next step of the Builder.
         */
        @Override
        public DeviceMatchNodeBindingsStep3 withCallbacks(List<? extends Callback> callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        /**
         * Set the {@link DeviceProfilesDao}.
         *
         * @param deviceProfilesDao The {@link DeviceProfilesDao}.
         * @return The next step of the Builder.
         */
        @Override
        public DeviceMatchNodeBindingsStep4 withDeviceProfilesDao(DeviceProfilesDao deviceProfilesDao) {
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
        public DeviceMatchNodeBindingsStep5 withSharedState(Object sharedState) {
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
        public ScriptBindingsStep1 withTransientState(Object transientState) {
            this.transientState = transientState;
            return this;
        }

        /**
         * Builds the {@link DeviceMatchNodeBindings}.
         *
         * @return the {@link DeviceMatchNodeBindings}.
         */
        @Override
        public DeviceMatchNodeBindings build() {
            return new DeviceMatchNodeBindings(this);
        }
    }
}
