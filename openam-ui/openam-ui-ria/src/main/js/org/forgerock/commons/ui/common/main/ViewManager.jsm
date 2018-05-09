/*
 * Copyright 2012-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { isEqual, isFunction, map } from "lodash";
import $ from "jquery";

import Messages from "org/forgerock/commons/ui/common/components/Messages";

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

        Messages.messages.hideMessages();
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

            // For ES6 modules, we require that the view is the default export.
            if (view.__esModule) {
                view = view.default;
            }

            // TODO: Investigate whether this is required anymore
            if (view.init) {
                view.init();
            }

            if (isBackboneView(view)) {
                view.render(decodedArgs, callback);
            } else if (isReactView(view)) {
                import("org/forgerock/commons/ui/common/main/ReactAdapterView").then((ReactAdapterView) => {
                    const adapter = new ReactAdapterView({ reactView: view });
                    currentReactAdapterView = adapter;
                    adapter.render();
                });
            } else {
                throw new Error("[ViewManager] Unable to determine view type (Backbone or React).");
            }
        });
    } else {
        currentModule().then((view) => {
            view.rebind();

            if (callback) {
                callback();
            }
        });
    }

    currentArguments = args;
    currentModule = module;
}

export function refresh () {
    changeView(currentModule, currentArguments, () => {}, true);
}
