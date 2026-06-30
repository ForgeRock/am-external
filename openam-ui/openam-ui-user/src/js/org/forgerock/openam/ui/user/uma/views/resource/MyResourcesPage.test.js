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
 * Copyright 2026 Ping Identity Corporation.
 */

import { expect } from "chai";
import injector from "inject-loader!./MyResourcesPage";

describe("org/forgerock/openam/ui/user/uma/views/resource/MyResourcesPage", () => {
    let MyResourcesPage;
    let mockUMAService;

    before(() => {
        mockUMAService = {
            labels: {
                get: () => Promise.resolve({ id: "label-1", name: "<img src=x>" })
            },
            unshareAllResources: () => Promise.resolve()
        };

        const module = injector({
            "jquery": { t: (key) => key },
            "org/forgerock/openam/ui/user/uma/views/resource/BasePage": {
                extend (proto) {
                    return function () {
                        Object.assign(this, proto);
                        this.data = {};
                    };
                }
            },
            "org/forgerock/commons/ui/common/components/BootstrapDialog": {},
            "org/forgerock/openam/ui/common/util/Constants": {},
            "org/forgerock/commons/ui/common/main/EventManager": {},
            "org/forgerock/openam/ui/user/uma/services/UMAService": mockUMAService
        });
        MyResourcesPage = module.default;
    });

    describe("#renderResources", () => {
        it("HTML-escapes the label name before assigning to data.labelName", () => {
            const page = new MyResourcesPage();
            page.data.labelId = "label-1";
            page.data.topLevel = false;
            page.createLabelCollection = () => ({});
            page.createColumns = () => [{ name: "col1" }, { name: "col2" }];
            page.renderGrid = () => {};

            return page.renderResources().then(() => {
                expect(page.data.labelName).to.equal("&lt;img src=x&gt;");
            });
        });
    });
});
