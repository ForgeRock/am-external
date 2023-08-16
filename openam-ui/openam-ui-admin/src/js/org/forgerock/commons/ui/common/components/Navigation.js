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
 * Copyright 2011-2019 ForgeRock AS.
 */

import "bootstrap";

import $ from "jquery";
import _ from "lodash";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AdminConstants from "org/forgerock/openam/ui/admin/utils/Constants";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import createUri from "org/forgerock/openam/ui/common/redirectToUser/createUri";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import NavigationDropdownMenuPartial from "partials/navigation/_NavigationDropdownMenu";
import NavigationFilter from "org/forgerock/openam/ui/common/components/navigation/filters/RouteNavGroupFilter";
import NavigationLinkPartial from "partials/navigation/_NavigationLink";
import Router from "org/forgerock/commons/ui/common/main/Router";
import template from "templates/common/NavigationTemplate";

const Navigation = {
    configuration: {
        username: {
            "label": "config.AppConfiguration.Navigation.userAvatar.signedInAs.title"
        },
        userBar: [{
            "href": createUri("profile/details"),
            "i18nKey": "common.user.selfService",
            "navGroup": "admin",
            "visibleToRoles": ["ui-self-service-user"]
        }, {
            "event" : AdminConstants.EVENT_AMADMIN_SECURITY_DIALOG,
            "i18nKey": "common.user.changePassword",
            "navGroup": "admin",
            "visibleToRoles": ["ui-amadmin"]
        }, {
            "href": createUri("logout"),
            "i18nKey": "common.form.logout"
        }],
        links: {
            "admin": {
                "urls": {
                    "realms": {
                        "url": "#realms",
                        "name": "config.AppConfiguration.Navigation.links.realms.title",
                        "icon": "fa fa-cloud hidden-md",
                        "dropdown": true,
                        "urls": [{
                            "url": "#realms",
                            "name": "config.AppConfiguration.Navigation.links.realms.showAll",
                            "icon": "fa fa-th"
                        }, {
                            "url": "#realms/new",
                            "name": "config.AppConfiguration.Navigation.links.realms.newRealm",
                            "icon": "fa fa-plus"
                        }, {
                            divider: true
                        }],
                        "visibleToRoles": ["ui-realm-admin"]
                    },
                    "configure": {
                        "url": "#configure",
                        "name": "config.AppConfiguration.Navigation.links.configure.title",
                        "icon": "fa fa-wrench hidden-md",
                        "dropdown": true,
                        "urls": [{
                            "url": "#configure/authentication",
                            "name": "config.AppConfiguration.Navigation.links.configure.authentication",
                            "icon": "fa fa-user"
                        }, {
                            "url": "#configure/globalServices",
                            "name": "config.AppConfiguration.Navigation.links.configure.global-services",
                            "icon": "fa fa-globe"
                        }, {
                            "url": "#configure/serverDefaults/general",
                            "name": "config.AppConfiguration.Navigation.links.configure.server-defaults",
                            "icon": "fa fa-server"
                        }, {
                            "url": "#configure/secretStores",
                            "name": "config.AppConfiguration.Navigation.links.configure.secret-stores",
                            "icon": "fa fa-eye-slash"
                        }],
                        "visibleToRoles": ["ui-global-admin"]

                    },
                    "deployment": {
                        "url": "#deployment",
                        "name": "config.AppConfiguration.Navigation.links.deployment.title",
                        "icon": "fa fa-sitemap hidden-md",
                        "dropdown": true,
                        "urls": [{
                            "url": "#deployment/servers",
                            "name": "config.AppConfiguration.Navigation.links.deployment.servers",
                            "icon": "fa fa-server"
                        }, {
                            "url": "#deployment/sites",
                            "name": "config.AppConfiguration.Navigation.links.deployment.sites",
                            "icon": "fa fa-bookmark-o"
                        }],
                        "visibleToRoles": ["ui-global-admin"]
                    },
                    "helpLinks": {
                        "url": "#api",
                        "icon": "fa fa-question-circle",
                        "dropdown": true,
                        "navbarRight": true,
                        "urls": [{
                            "url": "#api/explorer/",
                            "icon": "fa fa-code",
                            "name": "config.AppConfiguration.Navigation.helpLinks.apiExplorer"
                        }, {
                            "url": "#api/docs",
                            "icon": "fa fa-file-text",
                            "name": "config.AppConfiguration.Navigation.helpLinks.apiDocs"
                        }, {
                            "url": "https://backstage.forgerock.com/docs/am",
                            "icon": "fa fa-book",
                            "name": "config.AppConfiguration.Navigation.helpLinks.documentation"
                        }],
                        "visibleToRoles": ["ui-global-admin"]
                    }
                }
            }
        }
    }
};

const getUserName = function () {
    if (Configuration.loggedUser.has("userName")) {
        return Configuration.loggedUser.get("userName"); //idm
    } else if (Configuration.loggedUser.has("cn")) {
        return Configuration.loggedUser.get("cn"); //am
    } else {
        return Configuration.loggedUser.id; //fallback option
    }
};

const hasNavbarRight = function (data) {
    return (data.userBar && data.userBar.length) || (data.navbarRight && data.navbarRight.length);
};

/*
    Navigation is configured from AppConfiguration in each forgerock application.
    There are several items that can be controlled and configured.

    Username: Configuration of control of the username in userbar. This can be configured in two primary ways

        href - Link location. If provided the username (and any additional labels) will become a link,
               otherwise if nothing provided it will default to a static field.
        label - Provides a title that will sit above the username.
        secondaryLabel - Provides a secondary title that will sit below username.

        Example:

         username: {
             "href" : "#profile/",
             "label" : "config.AppConfiguration.Navigation.userAvatar.signedInAs.title",
             "secondaryLabel" : "config.AppConfiguration.Navigation.userAvatar.signedInAs.title.secondaryLabel"
         },

    Userbar: Configuration of the menu items in the userbar

        id - Element ID
        href - Link location
        i18nKey - Translation string
        divider - When set to true creates a divider for the dropdown menu items
        event - Rather then a href this will fire off a UI Event.

        Example:

         {
             "id": "user_link",
             "href": "../selfservice",
             "i18nKey": "common.form.userView"
         },
         {
            divider: true
         },
         {
             "id": "logout_link",
             "href": "#logout/",
             "i18nKey": "common.form.logout"
         },
         {
             "id": "changePasswordLink",
             "event" : AdminConstants.EVENT_SHOW_CHANGE_SECURITY_DIALOG,
             "i18nKey": "common.user.changePassword"
         }

    Navigation: Besides username and userbar specific controls the general navigation items can be controlled here.

        role - Controls the role a user must have set for this navigation item to display
        urls - A list of provided navigation. When tied to drop down controls drop down items. Currently this list
               can only go two levels deep.
        name - Name of the navigation element
        icon - Icon to display with the navigation name
        url - Link location
        data - A list of data attribute objects with "type" and "value" kays
            Example:

            "data" : [{
                "type" : "toggle",
                "value" : "popover"
            }]

        dropdown - Boolean that controls if a drop down element is used or a standard navigation element
        navbarRight - Boolean. If set to true the navigation item will render on the right

        Example:

        "admin" : {
            "role": "ui-admin",
                "urls": {
                "configuration": {
                    "name": "Configure",
                        "icon": "fa fa-wrench",
                        "dropdown": true,
                        "urls" : [
                        {
                            "url": "#connectors/",
                            "name": "config.AppConfiguration.Navigation.links.connectors",
                            "icon": "fa fa-cubes"
                        },
                        {
                            "url": "#managed/",
                            "name": "config.AppConfiguration.Navigation.links.managedObjects",
                            "icon": "fa fa-th"
                        },
                        {
                            "url": "#mapping/",
                            "name": "config.AppConfiguration.Navigation.links.mapping",
                            "icon": "fa fa-arrows-h"
                        },
                        {
                            "url": "#settings/",
                            "name": "config.AppConfiguration.Navigation.links.systemPref",
                            "icon": "fa fa-cog"
                        }
                    ]
                },
                "managed": {
                    "name": "config.AppConfiguration.Navigation.links.manage",
                        "icon": "fa fa-cogs",
                        "dropdown": true,
                        "urls" : []
                }
            }
        }
 */

Navigation.init = function (callback) {
    const CustomNavigation = AbstractView.extend({
        element: "#menu",
        template,
        noBaseTemplate: true,
        data: {},
        partials: {
            "navigation/_NavigationDropdownMenu": NavigationDropdownMenuPartial,
            "navigation/_NavigationLink": NavigationLinkPartial
        },
        events: {
            "click .event-link": "fireEvent"
        },

        fireEvent (e) {
            e.preventDefault();
            const event = $(e.currentTarget).data().event;
            if (event) {
                EventManager.sendEvent(event, e);
            }
        },

        render (args, callback) {
            /*
               The user information is shown at the top of the userBar widget,
               but it is stored in different ways for different products.
            */
            if (Configuration.loggedUser) {
                this.data.admin = _.includes(Configuration.loggedUser.uiroles, "ui-admin");

                this.data.userBar = _.chain(Navigation.configuration.userBar)
                    .map((link) => {
                        if (_.has(link, "i18nKey")) {
                            link.label = $.t(link.i18nKey);
                        }
                        return link;
                    })
                    .filter((link) => {
                        if (!link.visibleToRoles) {
                            return true;
                        }

                        if (link.navGroup !== Router.currentRoute.navGroup) {
                            return false;
                        }

                        return _.intersection(Configuration.loggedUser.uiroles, link.visibleToRoles).length > 0;
                    })
                    .value();

                this.data.user = {
                    username: getUserName(),
                    label: _.get(Navigation.configuration, "username.label"),
                    secondaryLabel: _.get(Navigation.configuration, "username.secondaryLabel"),
                    href: _.get(Navigation.configuration, "username.href")
                };

                this.reload();
                this.data.showNavbarRight = hasNavbarRight(this.data);
                this.parentRender(_.bind(() => {
                    if (callback) {
                        callback();
                    }
                }, this));
            } else {
                this.reload();
                this.data.showNavbarRight = hasNavbarRight(this.data);
                this.parentRender(_.bind(() => {
                    if (callback) {
                        callback();
                    }
                }, this));
            }
        },

        addLinksFromConfiguration (context) {
            let baseActive;
            const self = this;

            if (!Configuration.loggedUser || !context) {
                return;
            }

            _.each(context.urls, (navObj) => {
                const roles = _.intersection(Configuration.loggedUser.uiroles, navObj.visibleToRoles);
                const userHasRole = roles.length > 0;
                if (navObj.visibleToRoles && !userHasRole) {
                    return;
                }

                baseActive = self.isCurrent(navObj.url) ||
                    self.isCurrent(navObj.baseUrl) ||
                    self.childIsCurrent(navObj.urls);

                const navbar = navObj.navbarRight ? self.data.navbarRight : self.data.navbarLeft;
                navbar.push(self.buildNavElement(navObj, baseActive));
            });
        },

        buildNavElement (navObj, active) {
            const self = this;
            const subs = [];
            const navElement = {
                key: navObj.name,
                title: $.t(navObj.name),
                icon: navObj.icon
            };

            if (active) {
                navElement.active = active;
            }

            if (navObj.url) {
                navElement.hashurl = navObj.url;
            } else if (navObj.event) {
                navElement.event = navObj.event;
            }

            if (navObj.divider) {
                navElement.divider = navObj.divider;
            }

            if (navObj.header) {
                navElement.header = navObj.header;
                navElement.headerTitle = $.t(navObj.headerTitle);
            }

            if (navObj.cssClass) {
                navElement.cssClass = navObj.cssClass;
            }

            if (navObj.dropdown === true) {
                navElement.dropdown = true;

                _.each(navObj.urls, _.bind(function (obj) {
                    subs.push(self.buildNavElement(obj, this.isCurrent(obj.url)));
                }, this));

                navElement.urls = subs;
            }

            return navElement;
        },

        childIsCurrent (urls) {
            let urlName;

            for (urlName in urls) {
                if (this.isCurrent(urls[urlName].url)) {
                    return true;
                }
            }
            return false;
        },

        isCurrent (urlName) {
            let fromHash; const afterHash = window.location.href.split("#")[1];
            if (afterHash) {
                fromHash = `#${afterHash}`;
            } else {
                fromHash = "#/";
            }
            return fromHash.indexOf(urlName) !== -1;
        },

        clear () {
            this.data.navbarLeft = [];
            this.data.navbarRight = [];
            delete this.data.showNavbarRight;
        },

        reload () {
            this.clear();
            this.addLinksFromConfiguration(NavigationFilter.filter(Navigation.configuration.links));
        }
    });

    Navigation.navigation = new CustomNavigation();
    Navigation.navigation.render(null, callback);
};

Navigation.reload = function () {
    if (Navigation.navigation) {
        Navigation.navigation.render();
    }
};

Navigation.addUserBarLink = function (link, position) {
    if (!_.find(Navigation.configuration.userBar, (ub) => {
        return ub.id === link.id;
    })) {
        if (position === "top") {
            Navigation.configuration.userBar.unshift(link);
        } else {
            Navigation.configuration.userBar.push(link);
        }
    }
};

/**
 * Adds new link to the navigation bar. Can either be a top- or a second-level item.
 * Does nothing if this link already exists.
 * @param {Object} link Link to add.
 * @param {string} link.url - Link url.
 * @param {string} link.name - Link name.
 * @param {string} link.cssClass - Link css class.
 * @param {string} link.icon - Link icon font awesome class.
 * @param {string} link.event - Link event.
 * @param {string} role Role to add for ("admin" or "user").
 * @param {string} [secondLevelItem] If this parameter is absent, the new link will become a top-level link,
 *                                  in order for the new link to become a second-level item, this parameter should
 *                                  point to an existing top-level item (one of the keys of the "urls" object).
 */
Navigation.addLink = function (link, role, secondLevelItem) {
    let pathToTheNewLink = [role, "urls"];

    if (secondLevelItem) {
        pathToTheNewLink = pathToTheNewLink.concat([secondLevelItem, "urls"]);
    }

    const links = _.reduce(pathToTheNewLink, (prevVal, nextVal) => {
        if (prevVal) {
            return prevVal[nextVal];
        }
    }, Navigation.configuration.links);

    if (links && !_.find(links, { name: link.name })) {
        if (secondLevelItem) {
            links.push(link);
        } else {
            links[link.name] = link;
        }
    }
};

/**
 * Removes a link from the navigation bar. Can either be a top- or a second-level item.
 * Does nothing if this link does not exist.
 * Will require the nav bar to be reloaded.
 * @param {Object} link Link to remove.
 * @param {string} link.key - Link key.
 * @param {string} link.name - Link name.
 * @param {string} role Role to remove for ("admin" or "user").
 * @param {string} [secondLevelItem] If this parameter is absent, the link will be removed from the top-level,
 *                                  in order for the link to be removed from the second-level, this parameter should
 *                                  point to an existing top-level item (one of the keys of the "urls" object).
 */
Navigation.removeLink = function (link, role, secondLevelItem) {
    let pathToTheLink = [role, "urls"];

    if (secondLevelItem) {
        pathToTheLink = pathToTheLink.concat([secondLevelItem, "urls"]);
    }

    const links = _.reduce(pathToTheLink, (prevVal, nextVal) => {
        if (prevVal) {
            return prevVal[nextVal];
        }
    }, Navigation.configuration.links);

    if (links && _.find(links, { name: link.name })) {
        delete links[link.key];
    }
};

export default Navigation;