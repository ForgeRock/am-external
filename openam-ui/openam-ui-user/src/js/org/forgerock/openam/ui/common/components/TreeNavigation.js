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
 * Copyright 2015-2019 ForgeRock AS.
 */

import _ from "lodash";
import React from "react";
import ReactDOM from "react-dom";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import unwrapDefaultExport from "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

const contentElementId = "#sidePageContent";
const isBackbonePage = (view) => view.prototype instanceof AbstractView;
const isReactPage = (view) =>
    view.prototype instanceof React.Component || view.WrappedComponent || _.isFunction(view);
const TreeNavigation = AbstractView.extend({
    events: {
        "click .sidenav a[href]:not([data-toggle]):not([data-event])": "navigateToPage"
    },
    findContentElement () {
        return this.$el.find(contentElementId)[0];
    },
    findActiveNavItem (fragment) {
        const anchor = this.$el.find(`.sidenav ol > li > a[href^="#${fragment}"]`);
        if (anchor.length) {
            const listItem = anchor.parent();
            listItem.addClass("active");

            const parentLists = listItem.parentsUntil(this.$el.find(".sidenav"), "ol.collapse");
            parentLists.addClass("in").attr("aria-expanded", "true").parent().addClass("active");
        } else {
            const fragmentSections = fragment.split("/");
            this.findActiveNavItem(fragmentSections.slice(0, -1).join("/"));
        }
    },
    navigateToPage () {
        this.renderSidePageContentOnly = true;
    },
    setElement (element) {
        AbstractView.prototype.setElement.call(this, element);
        if (this.route && this.renderSidePageContentOnly) {
            this.route.page().then(
                _.bind((module) => {
                    this.renderPage(module, this.args);
                }, this),
                _.bind(() => {
                    throw `Unable to render page for module ${this.route.page}`;
                }, this)
            );
        }
    },

    render (args, callback) {
        this.args = args;

        const element = this.findContentElement();
        if (element) {
            ReactDOM.unmountComponentAtNode(element);
        }

        const updateTreeAndLoadPage = () => {
            this.$el.find(".sidenav li").removeClass("active");
            this.findActiveNavItem(URIUtils.getCurrentFragment());
            this.route.page().then(unwrapDefaultExport).then((page) => {
                this.renderPage(page, args);
            });
            if (callback) {
                // used to re-render the breadcrumb
                callback();
            }
        };

        if (this.renderSidePageContentOnly) {
            updateTreeAndLoadPage();
        } else {
            this.parentRender(() => {
                updateTreeAndLoadPage();
            });
        }
    },
    renderPage (Module, args, callback) {
        if (isBackbonePage(Module)) {
            const page = new Module();
            page.element = contentElementId;
            page.render(args, callback);
            this.delegateEvents();
        } else if (isReactPage(Module)) {
            ReactDOM.render(React.createElement(Module), this.findContentElement());
        } else {
            throw new Error("[TreeNavigation] Unable to determine page type (Backbone or React).");
        }

        this.renderSidePageContentOnly = false;
    }
});

export default TreeNavigation;
