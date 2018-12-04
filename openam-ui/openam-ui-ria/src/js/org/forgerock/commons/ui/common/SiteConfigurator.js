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

import _ from "lodash";
import $ from "jquery";

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import eventManager from "org/forgerock/commons/ui/common/main/EventManager";
import conf from "org/forgerock/commons/ui/common/main/Configuration";
import SiteConfigurationService from "org/forgerock/openam/ui/common/services/SiteConfigurationService";

var SiteConfigurator = {
    configuration: {
        selfRegistration: false,
        enterprise: false
    }
};

SiteConfigurator.initialized = false;

eventManager.registerListener(Constants.EVENT_READ_CONFIGURATION_REQUEST, function() {
    if (!conf.globalData) {
        conf.setProperty('globalData', {});
        conf.globalData.auth = {};
    }

    if (!conf.delegateCache) {
        conf.setProperty('delegateCache', {});
    }

    if (SiteConfigurator.configuration && SiteConfigurator.initialized === false) {
        SiteConfigurator.initialized = true;

        SiteConfigurationService.getConfiguration(function(config) {
            SiteConfigurator.processConfiguration(config);
            eventManager.sendEvent(Constants.EVENT_APP_INITIALIZED);
        }, function() {
            SiteConfigurator.processConfiguration({});
            eventManager.sendEvent(Constants.EVENT_APP_INITIALIZED);
        });
    }
});

SiteConfigurator.processConfiguration = function(config) {
    // whatever settings were found will be saved in globalData
    _.extend(conf.globalData, config);

    if (config.defaultNotificationType) {
        conf.defaultType = config.defaultNotificationType;
    }

    if (config.notificationTypes) {
        conf.notificationTypes = config.notificationTypes;
    }

    if (config.roles) {
        conf.globalData.userRoles = config.roles;
    }

    conf.globalData.auth.cookieName = config.cookieName;
    conf.globalData.auth.cookieDomains = config.domains;
};

SiteConfigurator.configurePage = function (route, params) {
    var promise = $.Deferred();

    if (typeof SiteConfigurationService.checkForDifferences === "function") {
        SiteConfigurationService.checkForDifferences(route, params).then(function (config) {
            if (config) {
                SiteConfigurator.processConfiguration(config);
            }
            promise.resolve();
        });
    } else {
        promise.resolve();
    }

    return promise;
};

export default SiteConfigurator;
