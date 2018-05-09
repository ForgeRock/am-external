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
 * Copyright 2011-2018 ForgeRock AS.
 */

define([
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "config/process/AMConfig",
    "config/process/CommonConfig"
], (_, Constants, EventManager, AMConfig, CommonConfig) => {
    const obj = {};

    obj.callRegisterListenerFromConfig = function (config) {
        EventManager.registerListener(config.startEvent, (event) => {
            return config.processDescription(event);
        });
    };

    obj.loadEventHandlers = function () {
        let processArray = [...AMConfig, ...CommonConfig];
        // processes which override the default of the same name
        const overrideArray = _.filter(processArray, (process) => {
            return !!process.override;
        });

        // remove those processes which have been overridden
        processArray = _.reject(processArray, (process) => {
            return !process.override && _.find(overrideArray, (override) => {
                return override.startEvent === process.startEvent && !!override.override;
            });
        });

        _.map(processArray, obj.callRegisterListenerFromConfig);

        EventManager.sendEvent(Constants.EVENT_READ_CONFIGURATION_REQUEST);
    };

    return obj;
});
