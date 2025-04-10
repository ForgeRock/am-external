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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.saml2.plugins;

import java.util.Map;

import org.forgerock.openam.annotations.EvolvingAll;

/**
 * All the SAML federation plugins that need to be initialized should extend this.
 */
@EvolvingAll
public interface InitializablePlugin extends SAMLPlugin {


    /**
     * Constants for hosted entity id parameter
     */
    String HOSTED_ENTITY_ID = "HOSTED_ENTITY_ID";

    /**
     * Constants for the realm of the hosted entity parameter.
     */
    String REALM = "REALM";


    /**
     * Initializes the federation plugin, this method will only be executed
     * once after creation of the plugin instance.
     *
     * @param initParams  initial set of parameters configured in the service
     * 		provider for this plugin. One of the parameters named
     *          <code>HOSTED_ENTITY_ID</code> refers to the ID of this
     *          hosted service provider entity, one of the parameters named
     *          <code>REALM</code> refers to the realm of the hosted entity
     */
    default void initialize(Map initParams) {
    }

    /**
     * Initializes the federation plugin, this method will only be executed once after creation of the plugin instance.
     *
     * @deprecated since 7.3.0 use {@link InitializablePlugin#initialize(Map)}.
     *
     * @param hostedEntityID the hosted entity ID
     * @param realm realm of the hosted entity
     */
    @Deprecated(forRemoval = true, since = "7.3.0")
    default void initialize(String hostedEntityID, String realm) {
    }

}
