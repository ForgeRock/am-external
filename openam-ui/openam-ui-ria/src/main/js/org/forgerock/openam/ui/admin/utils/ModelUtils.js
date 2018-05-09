/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], ($, _, Messages, EventManager, Constants) => {
    /**
     * @exports org/forgerock/openam/ui/admin/utils/ModelUtils
     */
    var obj = {};

    function hasError (response) {
        return _.has(response, "responseJSON.message") && _.isString(response.responseJSON.message);
    }

    obj.errorHandler = function (response) {
        if (_.get(response, "status") === 401) {
            EventManager.sendEvent(Constants.EVENT_UNAUTHORIZED, { error: response.error() });
        } else if (hasError(response)) {
            Messages.addMessage({ type: Messages.TYPE_DANGER, escape: true, response });
        } else {
            Messages.addMessage({ type: Messages.TYPE_DANGER, message: $.t("config.messages.CommonMessages.unknown") });
        }
    };

    return obj;
});
