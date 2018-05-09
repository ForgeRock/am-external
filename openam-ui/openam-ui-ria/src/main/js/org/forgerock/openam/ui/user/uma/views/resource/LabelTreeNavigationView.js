/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "jquery",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/components/TreeNavigation",
    "org/forgerock/openam/ui/user/uma/services/UMAService",
    "templates/user/uma/views/resource/_NestedList"
], (_, $, Router, TreeNavigation, UMAService, NestedListPartial) => {
    var LabelTreeNavigationView = TreeNavigation.extend({
        template: "user/uma/views/resource/LabelTreeNavigationTemplate",
        partials: {
            "templates/user/uma/views/resource/_NestedList": NestedListPartial
        },
        findActiveNavItem (fragment) {
            var myLabelsRoute = Router.configuration.routes.umaResourcesMyLabelsResource,
                isCurrentRouteForResource = Router.currentRoute === myLabelsRoute,
                subFragment = isCurrentRouteForResource ? _.initial(fragment.split("/")).join("/") : fragment,
                anchor = this.$el.find(`.sidenav ol > li > a[href='#${subFragment}']`),
                parentOls;

            if (anchor.length) {
                this.$el.find(".sidenav ol").removeClass("in");

                parentOls = anchor.parentsUntil(this.$el.find(".sidenav"), "ol.collapse");
                parentOls.addClass("in").parent().children("span[data-toggle]").attr("aria-expanded", "true");
                anchor.parent().addClass("active");

                if (anchor.attr("aria-expanded") === "false") {
                    anchor.attr("aria-expanded", "true");
                }
            }
        },
        navigateToPage (event) {
            this.$el.find(".sidenav li").removeClass("active");
            $(event.currentTarget).addClass("active");
            this.renderSidePageContentOnly = true;
        },
        render (args, callback) {
            var self = this,
                userLabels,
                sortedUserLabels;

            this.args = args;
            this.callback = callback;

            UMAService.labels.all().done((data) => {
                if (!_.any(data.result, (label) => {
                    return label.name.toLowerCase() === "starred";
                })) {
                    UMAService.labels.create("starred", "STAR");
                }

                userLabels = _.filter(data.result, (label) => { return label.type.toLowerCase() === "user"; });
                sortedUserLabels = _.sortBy(userLabels, (label) => { return label.name; });

                self.data.labels = {
                    starred: _.filter(data.result, (label) => { return label.type.toLowerCase() === "starred"; }),
                    system: _.filter(data.result, (label) => { return label.type.toLowerCase() === "system"; }),
                    user: sortedUserLabels
                };
                self.data.nestedLabels = [];

                _.each(self.data.labels.user, (label) => {
                    self.addToParent(self.data.nestedLabels, label);
                });

                TreeNavigation.prototype.render.call(self, args, callback);
            });
        },

        addToParent (collection, label) {
            if (label.name.indexOf("/") === -1) {
                label.title = label.name;
                label.children = [];
                label.viewId = _.uniqueId("viewId_");
                collection.push(label);
            } else {
                var shift = label.name.split("/"),
                    parentName = shift.shift(),
                    parent;
                label.name = shift.join("/");
                parent = _.find(collection, { title: parentName });
                if (!parent) {
                    parent = { title: parentName, children: [], viewId: _.uniqueId("viewId_") };
                    collection.push(parent);
                }
                this.addToParent(parent.children, label);
            }
        },

        addUserLabels (userLabels) {
            var self = this;

            this.data.nestedLabels = [];
            this.data.labels.user = _.sortBy(userLabels, (label) => { return label.name; });

            _.each(this.data.labels.user, (label) => {
                self.addToParent(self.data.nestedLabels, label);
            });

            this.renderSidePageContentOnly = true;
            TreeNavigation.prototype.render.call(this, this.args, this.callback);
        }
    });

    return new LabelTreeNavigationView();
});