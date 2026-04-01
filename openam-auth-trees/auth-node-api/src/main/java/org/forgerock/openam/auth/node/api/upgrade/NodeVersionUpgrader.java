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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api.upgrade;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.Evolving;
import org.forgerock.openam.auth.node.api.upgrade.exception.NodeUpgradeException;

/**
 * Interface for upgrading a node to this version.
 */
@FunctionalInterface
public interface NodeVersionUpgrader {
    /**
     * Upgrade the node to this version.
     * @param config the configuration to upgrade.
     *
     * @return the config after the upgrade.
     * @throws NodeUpgradeException if the upgrade fails for any reason.
     */
    JsonValue upgrade(JsonValue config) throws NodeUpgradeException;

    /**
     * Metadata annotation for the upgrader.
     * This annotation should be used on the class that implements the upgrader.
     * It specifies the previous version upgrader to use if any.
     */
    @Evolving
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface UpgraderMetadata {

        /**
         * The node version that this upgrader is for.
         *
         * @return the version.
         */
        int version();

        /**
         * The previous upgrader class, should be {@link NoUpgrades} if this is the first upgrader
         * for this node.
         *
         * @return the previous upgrader class.
         */
        Class<? extends NodeVersionUpgrader> previousUpgrader();
    }
}
