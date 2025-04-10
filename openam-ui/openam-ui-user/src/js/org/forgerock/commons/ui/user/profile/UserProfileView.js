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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import "bootstrap";

import _ from "lodash";
import $ from "jquery";

import AbstractUserProfileTab from "org/forgerock/commons/ui/user/profile/AbstractUserProfileTab";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";

/**
 * Manages the tabs and routing amongst them for the user's profile
 * @exports org/forgerock/commons/ui/user/profile/UserProfileView
 */
const UserProfileView = AbstractView.extend({
    template: "user/UserProfileTemplate",
    partials: {
        "form/_basicInput": "form/_basicInput",
        "form/_basicSaveReset": "form/_basicSaveReset"
    },
    events: {
        "click a[role=tab]": "updateRoute"
    },
    dynamicTabs: [],
    /**
     * Accepts an instance of AbstractUserProfileTab (or an extension of it) as a new tab
     * to include in the profile
     * @param {*} tabView Tab to register
     */
    registerTab (tabView) {
        this.dynamicTabs.push(tabView);
    },

    /**
     * Removes any tabs which had been added dynamically
     */
    resetTabs () {
        this.dynamicTabs = [];
    },

    /**
     * When clicking on a new tab, the route needs to be updated to reflect the new nav state
     * @param {Event} event Event
     */
    updateRoute (event) {
        const tabPane = $($(event.target).attr("href"));
        const form = tabPane.find("form");
        const tabRoute = form.attr("id");

        EventManager.sendEvent(Constants.ROUTE_REQUEST, { routeName: "profile", args: [tabRoute], trigger: false });
    },

    /**
     * Show the main view container along with any tabs which are declared statically within
     * the UserProfileView template. Then load any additional tabs which have been registered.
     * Finally, show the appropriate tab based on the "args" provided (via URL params).
     * @param {string[]} args Arguments
     * @param {Function} callback Callback function
     */
    render (args, callback) {
        const tabName = args[0] || "details";

        this.data.user = Configuration.loggedUser.toJSON();

        this.parentRender(function () {
            // instantiate a profile tab view which covers the DOM elements created
            // statically as part of the UserProfileTemplate
            this.staticTabs = _.map(this.$el.find(".tab-content>.tab-pane"), (tabPanel) => {
                const tab = new AbstractUserProfileTab({
                    el: tabPanel
                });
                //tab.delegateEvents();
                return tab;
            });

            // build the dynamically-registered tabs
            $.when(..._.map(this.dynamicTabs, _.bind(function (tab) {
                const promise = $.Deferred();
                const tabDetail = tab.getTabDetail();
                const tabPanel = $('<div role="tabpanel" class="tab-pane">');
                tabPanel.attr("id", tabDetail.panelId);
                this.$el.find(".tab-content").append(tabPanel);
                tab.element = tabPanel[0];

                this.$el.find(".fr-profile-nav").append(
                    $('<li role="presentation">').append(
                        $(`<a href="#${tabDetail.panelId}" role="tab" data-toggle="tab">`)
                            .html(tabDetail.label)
                    )
                );

                tab.render(_.cloneDeep(this.data), _.bind(() => {
                    promise.resolve();
                }, this));
                return promise;
            }, this))).then(_.bind(function () {
                const selectedTabId = this.$el.find(`form#${tabName}`).closest(".tab-pane").attr("id");
                const selectedTab = this.$el.find(`ul.fr-profile-nav a[href='#${selectedTabId}']`);

                _.each(this.staticTabs.concat(this.dynamicTabs), _.bind(function (tab) {
                    tab.reloadFormData(_.cloneDeep(this.data.user));
                }, this));

                selectedTab.tab("show");

                if (callback) {
                    callback();
                }
            }, this));
        });
    }
});

export default new UserProfileView();
