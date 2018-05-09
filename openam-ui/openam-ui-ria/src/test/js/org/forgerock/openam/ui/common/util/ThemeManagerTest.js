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
 * Copyright 2015-2018 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "sinon",
    "org/forgerock/openam/ui/common/util/Constants"
], ($, _, sinon, Constants) => {
    let ThemeManager;
    let Configuration;
    let EventManager;
    let URIUtils;
    let Router;
    let mock$;
    let themeConfig;
    let urlParams;
    let sandbox;
    describe("org/forgerock/openam/ui/common/util/ThemeManager", () => {
        beforeEach(() => {
            const injector = require("inject-loader!org/forgerock/openam/ui/common/util/ThemeManager");

            themeConfig = {
                themes: {
                    "default": {
                        path: "",
                        icon: "icon.png",
                        stylesheets: ["a.css", "c.css"]
                    },
                    other: {
                        name: "other",
                        path: "",
                        icon: "otherIcon.png",
                        stylesheets: ["b.css"]
                    }
                },
                mappings: [
                    { theme: "other", realms: ["/b"] }
                ]
            };

            urlParams = {};

            mock$ = sinon.spy(() => { return mock$; });
            mock$.remove = sinon.spy();
            mock$.appendTo = sinon.spy();
            mock$.Deferred = _.bind($.Deferred, $);

            sandbox = sinon.sandbox.create();

            Configuration = {
                globalData: {
                    theme: undefined,
                    realm: "/"
                }
            };

            EventManager = {
                sendEvent: sinon.stub()
            };

            URIUtils = {
                getCurrentCompositeQueryString: sinon.stub().returns(""),
                parseQueryString: sinon.stub().returns(urlParams)
            };

            Router = {
                currentRoute: {}
            };

            ThemeManager = injector({
                "jquery": mock$,
                "./getThemeConfiguration": () => Promise.resolve(themeConfig),
                "org/forgerock/commons/ui/common/util/URIUtils": URIUtils,
                "org/forgerock/commons/ui/common/main/Configuration": Configuration,
                "org/forgerock/commons/ui/common/main/EventManager": EventManager,
                Router
            });
        });

        afterEach(() => {
            sandbox.restore();
        });

        describe("#getTheme", () => {
            it("sends EVENT_THEME_CHANGED event", () =>
                ThemeManager.getTheme().then(() => {
                    expect(EventManager.sendEvent).to.be.calledOnce.calledWith(Constants.EVENT_THEME_CHANGED);
                })
            );
            it("rejects if theme configuration does not contain a theme object", (done) => {
                delete themeConfig.themes;
                ThemeManager.getTheme().then(undefined, () => done());
            });
            it("rejects if theme configuration does specify a default theme", (done) => {
                delete themeConfig.themes.default;
                ThemeManager.getTheme().then(undefined, () => done());
            });
            it("returns a promise", (done) => {
                const result = ThemeManager.getTheme();
                expect(result.then).to.not.be.undefined;
                result.then(() => {
                    done();
                });
            });
            it("places the selected theme onto the global data object", () =>
                ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.default);
                })
            );
            it("selects the correct theme based on the realm", () => {
                Configuration.globalData.realm = "/b";
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the correct theme based on the realm", () => {
                Configuration.globalData.realm = "/b";
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the default theme if no realms match", () => {
                Configuration.globalData.realm = "/c";
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.default);
                });
            });
            it("allows mappings to specify regular expressions to match realms", () => {
                themeConfig.mappings[0].realms[0] = /^\/hello.*/;
                Configuration.globalData.realm = "/hello/world";
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the correct theme based on the authentication chain", () => {
                urlParams.service = "test";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: ["test"]
                });
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("selects the default theme if no authentication chains match", () => {
                urlParams.service = "tester";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: ["test"]
                });
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.default);
                });
            });
            it("allows mappings to specify regular expressions to match authentication chains", () => {
                urlParams.service = "tester";
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: [/test/]
                });
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("matches realms and authentication chains if both are specified in a mapping", () => {
                Configuration.globalData.realm = "/a";
                urlParams.service = "test";
                // No match - wrong realm
                themeConfig.mappings.push({
                    theme: "default",
                    realms: ["/b"],
                    authenticationChains: ["test"]
                });
                // No match - wrong authentication chain
                themeConfig.mappings.push({
                    theme: "default",
                    realms: ["/a"],
                    authenticationChains: ["tester"]
                });
                // Match
                themeConfig.mappings.push({
                    theme: "other",
                    realms: ["/a"],
                    authenticationChains: ["test"]
                });
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("won't match a mapping that needs an authentication chain if none is present", () => {
                Configuration.globalData.realm = "/a";
                // No match - wants an authentication chain but none is present
                themeConfig.mappings.push({
                    theme: "default",
                    realms: ["/a"],
                    authenticationChains: ["test"]
                });
                // Match
                themeConfig.mappings.push({
                    theme: "other",
                    realms: ["/a"]
                });
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("matches a mapping that has an empty authentication chain if none is present", () => {
                themeConfig.mappings.push({
                    theme: "other",
                    authenticationChains: [""]
                });
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme).to.deep.equal(themeConfig.themes.other);
                });
            });
            it("fills in any missing properties from selected theme with the default theme", () => {
                Configuration.globalData.realm = "/b";
                delete themeConfig.themes.other.stylesheets;
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme.stylesheets)
                        .to.deep.equal(themeConfig.themes.default.stylesheets);
                });
            });
            it("doesn't try to merge arrays in the selected theme with the default theme", () => {
                Configuration.globalData.realm = "/b";
                return ThemeManager.getTheme().then(() => {
                    expect(Configuration.globalData.theme.stylesheets)
                        .to.deep.equal(themeConfig.themes.other.stylesheets);
                });
            });
            it("removes any existing CSS and favicons from the page", () =>
                ThemeManager.getTheme().then(() => {
                    expect(mock$).to.be.calledWith("link");
                    sinon.assert.calledOnce(mock$.remove);
                })
            );
            it("doesn't update the page if the theme hasn't changed since the last call", () =>
                ThemeManager.getTheme().then(() => {
                    mock$.reset();
                    return ThemeManager.getTheme();
                }).then(() => {
                    expect(mock$).to.not.be.called;
                })
            );
        });
    });
});
