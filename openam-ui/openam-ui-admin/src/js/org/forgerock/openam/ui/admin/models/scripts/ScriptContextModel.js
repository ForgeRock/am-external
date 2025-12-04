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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

import Backbone from "backbone";

import URLHelper from "org/forgerock/openam/ui/common/util/URLHelper";
import ModelUtils from "org/forgerock/openam/ui/admin/utils/ModelUtils";

export default Backbone.Model.extend({
    idAttribute: "_id",
    urlRoot: URLHelper.substitute("__api__/contexts"),
    defaults () {
        return {
            _id: null,
            bindings: {},
            evaluatorVersions: "",
            allowlist: ""
        };
    },

    sync (method, model, options) {
        options = options || {};
        options.beforeSend = function (xhr) {
            xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
        };
        options.error = ModelUtils.errorHandler;

        method = method.toLowerCase();
        return Backbone.Model.prototype.sync.call(this, method, model, options);
    }
});
