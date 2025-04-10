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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import themeConfiguration from "config/ThemeConfiguration";
import validate from "./validate";

const createMessage = (message) => `Theme configuration error. ${message}.`;

describe("org/forgerock/openam/ui/common/util/theme/validate", () => {
    it("returns undefined", () => {
        expect(validate(themeConfiguration.themes)).to.eq(undefined);
    });

    context("whole configuration", () => {
        context("is valid", () => {
            it("does not throw an Error", () => {
                expect(() => validate(themeConfiguration.themes)).to.not.throw(Error);
            });
        });

        context("is not an object", () => {
            it("throws an Error", () => {
                expect(() => validate(true)).to.throw(Error, createMessage("Configuration should be object"));
            });
        });

        context("has no \"default\" attribute", () => {
            it("throws an Error", () => {
                expect(() => validate({})).to.throw(Error,
                    createMessage("Configuration should have required property '.default'")
                );
            });
        });
    });

    context("each configuration", () => {
        context("is not an object", () => {
            it("throws an Error", () => {
                expect(() => validate({
                    "default": true
                })).to.throw(Error, createMessage("\"['default']\" should be object"));
            });
        });

        context(".path", () => {
            context("is not a uri-reference", () => {
                it("throws an Error", () => {
                    expect(() => validate({
                        "default": {
                            path: "  /"
                        }
                    })).to.throw(Error, createMessage("\"['default'].path\" should match format \"uri-reference\""));
                });
            });

            context("is not a string", () => {
                it("throws an Error", () => {
                    expect(() => validate({
                        "default": {
                            path: true
                        }
                    })).to.throw(Error, createMessage("\"['default'].path\" should be string"));
                });
            });

            context("shorter than 2 characters", () => {
                it("throws an Error", () => {
                    expect(() => validate({
                        "default": {
                            path: "."
                        }
                    })).to.throw(Error, createMessage("\"['default'].path\" should NOT be shorter than 2 characters"));
                });
            });

            context("leads with a forward slash \"/\"", () => {
                it("throws an Error", () => {
                    expect(() => validate({
                        "default": {
                            path: "/path/"
                        }
                    })).to.throw(Error, createMessage("\"['default'].path\" should match pattern \"^[^/].*/$\""));
                });
            });

            context("does not trail with a forward slash \"/\"", () => {
                it("throws an Error", () => {
                    expect(() => validate({
                        "default": {
                            path: "path"
                        }
                    })).to.throw(Error, createMessage("\"['default'].path\" should match pattern \"^[^/].*/$\""));
                });
            });
        });
    });
});
