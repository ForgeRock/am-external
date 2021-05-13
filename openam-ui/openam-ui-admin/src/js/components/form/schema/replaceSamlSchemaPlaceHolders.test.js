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
 * Copyright 2020 ForgeRock AS.
 */

import { expect } from "chai";
import Constants from "org/forgerock/openam/ui/common/util/Constants";

import {
    replaceIdpPlaceHolders,
    replaceSpPlaceHolders,
    getDefaultUrl
} from "./replaceSamlSchemaPlaceHolders";

describe("#replaceSamlSchemaPlaceHolders", () => {
    context("replace idp place holders", () => {
        it("replaces the base url and idp meta alias place holders with actual values", () => {
            const object = {
              serviceAttributes: {
                singleLogoutService: [
                  {
                    "location": "{baseUrl}/IDPSloRedirect/metaAlias{idpMetaAlias}",
                    "responseLocation": "{baseUrl}/IDPSloRedirect/metaAlias{idpMetaAlias}"
                  }
                ]
              }
            }
            const replaced = replaceIdpPlaceHolders(object, "http://test.com/", "/idp");

            expect(replaced).to.deep.equal({
              serviceAttributes: {
                singleLogoutService: [
                  {
                    "location": "http://test.com/IDPSloRedirect/metaAlias/idp",
                    "responseLocation": "http://test.com/IDPSloRedirect/metaAlias/idp"
                  }
                ]
              }
            });
        });
    });

    context("replace sp place holders", () => {
        it("replaces the base url and sp meta alias place holders with actual values", () => {
            const object = {
              serviceAttributes: {
                singleLogoutService: [
                  {
                    "location": "{baseUrl}/IDPSloRedirect/metaAlias{spMetaAlias}",
                    "responseLocation": "{baseUrl}/IDPSloRedirect/metaAlias{spMetaAlias}"
                  }
                ]
              }
            }
            const replaced = replaceSpPlaceHolders(object, "http://test.com", "sp");

            expect(replaced).to.deep.equal({
              serviceAttributes: {
                singleLogoutService: [
                  {
                    "location": "http://test.com/IDPSloRedirect/metaAlias/sp",
                    "responseLocation": "http://test.com/IDPSloRedirect/metaAlias/sp"
                  }
                ]
              }
            });
        });
    });

    context("get default url", () => {
        var windowLocation;
        var context;
        beforeEach("initialise window location and context", () => {
            windowLocation = window.location;
            context = `${Constants.context}`;
        });
        afterEach("reinstate the original window location and context", () => {
            window.location = windowLocation;
            Constants.context = context;
        });
        it("return the default url", () => {
            delete window.location;
            window = Object.create(window);
            window.location = {
              protocol: 'http:',
              host: 'openam.example.com:8080',
            };
            Constants.context = "/openam";

            const defaultUrl = getDefaultUrl();
            expect(defaultUrl).equal("http://openam.example.com:8080/openam");
        });
    });
});