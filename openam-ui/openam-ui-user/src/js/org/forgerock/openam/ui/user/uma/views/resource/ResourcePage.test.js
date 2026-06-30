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
import injector from "inject-loader!./ResourcePage";

describe("org/forgerock/openam/ui/user/uma/views/resource/ResourcePage", () => {
    let renderLabelItem;
    let ResourcePage;
    let capturedDialogOptions;
    const mockBootstrapDialog = {
        show (options) { capturedDialogOptions = options; },
        TYPE_DANGER: "DANGER"
    };
    const mockJquery = (el) => ({ hasClass: (cls) => cls === "disabled" && !!(el && el._disabled) });
    mockJquery.t = (key) => key;

    before(() => {
        const module = injector({
            "selectize": {},
            "jquery": mockJquery,
            "backbone": {},
            "org/forgerock/commons/ui/common/main/AbstractView": {
                extend (proto) {
                    return function () { Object.assign(this, proto); };
                }
            },
            "org/forgerock/commons/ui/common/backgrid/Backgrid": {},
            "org/forgerock/openam/ui/common/util/BackgridUtils": {},
            "org/forgerock/commons/ui/common/components/BootstrapDialog": mockBootstrapDialog,
            "org/forgerock/openam/ui/user/uma/views/share/CommonShare": {},
            "org/forgerock/openam/ui/common/util/Constants": {},
            "org/forgerock/commons/ui/common/main/EventManager": {},
            "org/forgerock/openam/ui/user/uma/views/resource/LabelTreeNavigationView": {},
            "org/forgerock/commons/ui/common/components/Messages": {},
            "themes/default/templates/user/uma/backgrid/cell/RevokeCell": {},
            "themes/default/templates/user/uma/backgrid/cell/SelectizeCell": {},
            "org/forgerock/openam/ui/user/uma/models/UMAResourceSetWithPolicy": {},
            "org/forgerock/openam/ui/user/uma/services/UMAService": {},
            "themes/default/templates/user/uma/backgrid/cell/IconWithTooltipMessageCell": {}
        });
        renderLabelItem = module.renderLabelItem;
        ResourcePage = module.default;
    });

    describe("#renderLabelItem", () => {
        it("escapes HTML special characters in both the attribute and body positions", () => {
            const item = { name: "<>&\"'" };
            const result = renderLabelItem(item);
            expect(result).to.contain('data-value="&lt;&gt;&amp;&quot;&#39;"');
            expect(result).to.contain(">&lt;&gt;&amp;&quot;&#39;<");
        });

        it("renders a plain label name unchanged", () => {
            const item = { name: "work" };
            const result = renderLabelItem(item);
            expect(result).to.contain('data-value="work"');
            expect(result).to.contain(">work<");
        });
    });

    describe("#onUnshare", () => {
        beforeEach(() => { capturedDialogOptions = undefined; });

        it("HTML-escapes HTML tags in the resource name used as the dialog title", () => {
            const page = new ResourcePage();
            page.model = { get: (key) => key === "name" ? "<script>alert(1)</script>" : null };
            page.onUnshare({ currentTarget: {}, preventDefault: () => {} });
            expect(capturedDialogOptions.title).to.equal("&lt;script&gt;alert(1)&lt;/script&gt;");
        });

        it("HTML-escapes ampersands and quotes in the resource name used as the dialog title", () => {
            const page = new ResourcePage();
            page.model = { get: (key) => key === "name" ? "Foo & \"Bar\" '123'" : null };
            page.onUnshare({ currentTarget: {}, preventDefault: () => {} });
            expect(capturedDialogOptions.title).to.equal("Foo &amp; &quot;Bar&quot; &#39;123&#39;");
        });

        it("does not open the dialog when the menu item is disabled", () => {
            const page = new ResourcePage();
            page.model = { get: () => "name" };
            page.onUnshare({ currentTarget: { _disabled: true }, preventDefault: () => {} });
            expect(capturedDialogOptions).to.be.undefined;
        });
    });
});
