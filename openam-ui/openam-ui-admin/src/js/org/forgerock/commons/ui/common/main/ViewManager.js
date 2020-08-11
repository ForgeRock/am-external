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
 * Copyright 2012-2019 ForgeRock AS.
 */

import { isEqual, isFunction, map } from "lodash";
import $ from "jquery";

import Messages from "org/forgerock/commons/ui/common/components/Messages";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";

const decodeArgs = function (args) {
    return map(args, (a) => {
        return (a && decodeURIComponent(a)) || "";
    });
};
const isBackboneView = function (view) { return view && view.render && !isFunction(view); };
const isReactView = function (view) { return view && !view.render && isFunction(view); };

let currentArguments = null;
let currentModule = null;
let currentReactAdapterView;

export function changeView (module, args, callback, forceUpdate) {
    const decodedArgs = decodeArgs(args);

    if (currentModule !== module || forceUpdate || !isEqual(currentArguments, args)) {
        //close all existing dialogs
        if (typeof $.prototype.modal === "function") {
            $(".modal.in").modal("hide");
        }

        Messages.messages.clearMessages();
        module().then((view) => {
            /**
             * A reference to the current ReactAdapterView is cached to ensure it can be removed here.
             * Only ReactAdapterViews are removed, as invoking #remove on Backbone views causes undesirable
             * side effects.
             */
            if (currentReactAdapterView) {
                currentReactAdapterView.remove();
                currentReactAdapterView = null;
            }

            view = unwrapDefaultExport(view);

            // TODO: Investigate whether this is required anymore
            if (view.init) {
                view.init();
            }

            if (isBackboneView(view)) {
                view.render(decodedArgs, callback);
            } else if (isReactView(view)) {
                import("org/forgerock/commons/ui/common/main/ReactAdapterView")
                    .then(unwrapDefaultExport)
                    .then((ReactAdapterView) => {
                        const adapter = new ReactAdapterView({ reactView: view });
                        currentReactAdapterView = adapter;
                        adapter.render();
                    });
            } else {
                throw new Error("[ViewManager] Unable to determine view type (Backbone or React).");
            }
        });
    } else {
        currentModule().then(unwrapDefaultExport).then((view) => {
            view.rebind();

            if (callback) {
                callback();
            }
        });
    }

    currentArguments = args;
    currentModule = module;
}
