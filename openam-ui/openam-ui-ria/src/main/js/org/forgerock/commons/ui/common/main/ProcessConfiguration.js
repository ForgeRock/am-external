/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
