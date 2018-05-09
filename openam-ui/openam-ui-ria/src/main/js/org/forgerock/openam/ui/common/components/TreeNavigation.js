/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "react-dom",
    "react",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/openam/ui/common/util/es6/normaliseModule",
    "templates/admin/views/common/navigation/TreeNavigationTemplate",
    "partials/breadcrumb/_BreadcrumbTitle",
    "templates/admin/views/common/navigation/_TreeNavigationLeaf"
], ($, _, ReactDOM, React, AbstractView, URIUtils, normaliseModule, TreeNavigationTemplate, BreadcrumbTitlePartial,
    TreeNavigationLeafPartial) => {
    const contentElementId = "#sidePageContent";
    const isBackbonePage = (view) => view.prototype instanceof AbstractView;
    const isReactPage = (view) =>
        view.prototype instanceof React.Component || view.WrappedComponent || _.isFunction(view);
    const TreeNavigation = AbstractView.extend({
        template: TreeNavigationTemplate,
        partials: {
            "breadcrumb/_BreadcrumbTitle": BreadcrumbTitlePartial,
            "templates/admin/views/common/navigation/_TreeNavigationLeaf": TreeNavigationLeafPartial
        },
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
                this.route.page().then((page) => {
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
            Module = normaliseModule.default(Module);

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

    return TreeNavigation;
});
