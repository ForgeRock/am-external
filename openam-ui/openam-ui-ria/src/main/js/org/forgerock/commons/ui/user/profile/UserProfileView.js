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

define([
    "jquery",
    "underscore",
    "form2js/src/form2js",
    "form2js/src/js2form",
    "org/forgerock/commons/ui/user/profile/AbstractUserProfileTab",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "partials/form/_basicInput",
    "partials/form/_basicSaveReset",
    "bootstrap"
], function($, _, form2js, js2form,
    AbstractUserProfileTab,
    AbstractView,
    Configuration,
    Constants,
    EventManager,
    basicInputPartial,
    basicSaveResetPartial) {

    /**
     * Manages the tabs and routing amongst them for the user's profile
     * @exports org/forgerock/commons/ui/user/profile/UserProfileView
     */
    var UserProfileView = AbstractView.extend({
        template: "user/UserProfileTemplate",
        partials: {
            "form/_basicInput": basicInputPartial,
            "form/_basicSaveReset": basicSaveResetPartial
        },
        events: {
            "click a[role=tab]": "updateRoute",
            "shown.bs.tab": "focusInput"
        },
        dynamicTabs: [],
        /**
         * Accepts an instance of AbstractUserProfileTab (or an extension of it) as a new tab
         * to include in the profile
         */
        registerTab : function (tabView) {
            this.dynamicTabs.push(tabView);
        },

        /**
         * Removes any tabs which had been added dynamically
         */
        resetTabs: function () {
            this.dynamicTabs = [];
        },

        /**
         * As the tabs change, place the focus on the first editable form field
         */
        focusInput: function (event) {
            $($(event.target).attr("href")).find(":input:not([readonly]):first").focus();
        },

        /**
         * When clicking on a new tab, the route needs to be updated to reflect the new nav state
         */
        updateRoute: function (event) {
            var tabPane = $($(event.target).attr("href")),
                form = tabPane.find("form"),
                tabRoute = form.attr("id");

            EventManager.sendEvent(Constants.ROUTE_REQUEST, {routeName: "profile", args: [tabRoute], trigger: false});
        },

        /**
         * Show the main view container along with any tabs which are declared statically within
         * the UserProfileView template. Then load any additional tabs which have been registered.
         * Finally, show the appropriate tab based on the "args" provided (via URL params).
         */
        render: function (args, callback) {
            var tabName = args[0] || "details";

            this.data.user = Configuration.loggedUser.toJSON();

            this.parentRender(function () {

                // instantiate a profile tab view which covers the DOM elements created
                // statically as part of the UserProfileTemplate
                this.staticTabs = _.map(this.$el.find('.tab-content>.tab-pane'), function (tabPanel) {
                    var tab = new AbstractUserProfileTab({
                        el: tabPanel
                    });
                    //tab.delegateEvents();
                    return tab;
                });

                // build the dynamically-registered tabs
                $.when.apply($, _.map(this.dynamicTabs, function (tab) {
                    var promise = $.Deferred(),
                        tabDetail = tab.getTabDetail(),
                        tabPanel = $('<div role="tabpanel" class="tab-pane">');
                    tabPanel.attr('id', tabDetail.panelId);
                    this.$el.find(".tab-content").append(tabPanel);
                    tab.element = tabPanel[0];

                    this.$el.find(".fr-profile-nav").append(
                        $('<li role="presentation">').append(
                            $('<a href="#'+tabDetail.panelId+'" role="tab" data-toggle="tab">')
                                .html(tabDetail.label)
                        )
                    );

                    tab.render(_.cloneDeep(this.data), _.bind(function () {
                        promise.resolve();
                    }, this));
                    return promise;
                }, this)).then(_.bind(function () {
                    var selectedTabId = this.$el.find('form#'+tabName).closest(".tab-pane").attr("id"),
                        selectedTab = this.$el.find("ul.fr-profile-nav a[href='#"+selectedTabId+"']");

                    _.each(this.staticTabs.concat(this.dynamicTabs), _.bind(function (tab) {
                        tab.reloadFormData(_.cloneDeep(this.data.user));
                    }, this));

                    selectedTab.tab('show');

                    this.$el.find("#" + selectedTabId).find(":input:not([readonly]):first").focus();

                    if (callback) {
                        callback();
                    }
                }, this));
            });
        }
    });

    return new UserProfileView();
});
