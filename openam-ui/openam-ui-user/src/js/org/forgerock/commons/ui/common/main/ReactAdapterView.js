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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import _ from "lodash";
import Backbone from "backbone";
import React from "react";
import ReactDOM from "react-dom";

import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import DefaultBaseTemplate from "themes/default/templates/common/DefaultBaseTemplate";

function throwOnNoInitializationOptions (options) {
    if (!options) {
        throw new Error("[ReactAdapterView] No initialization options found.");
    }
}

function throwOnNoReactView (options) {
    if (!options.reactView) {
        throw new Error("[ReactAdapterView] No \"reactView\" option found on initialization options.");
    }
}

export default Backbone.View.extend({
    initialize (options) {
        throwOnNoInitializationOptions(options);
        throwOnNoReactView(options);

        this.options = options;

        _.defaults(this.options, {
            reactProps: {},
            needsBaseTemplate: true
        });
    },

    setBaseTemplate () {
        Configuration.setProperty("baseTemplate", DefaultBaseTemplate);
        EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
    },

    renderReactComponent () {
        ReactDOM.render(React.createElement(this.options.reactView, this.options.reactProps), this.el);
    },

    remove () {
        ReactDOM.unmountComponentAtNode(this.el);
    },

    render () {
        const view = this;

        if (this.options.needsBaseTemplate) {
            this.setBaseTemplate();

            document.getElementById("wrapper").innerHTML = DefaultBaseTemplate();
            view.setElement("#content");
            view.renderReactComponent();
        } else {
            view.renderReactComponent();
        }
    }
});
