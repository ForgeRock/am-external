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

import { sanitize } from "./Sanitizer";

describe("org/forgerock/openam/ui/common/util/Sanitizer", () => {
    context("when sanitizing allowed content", () => {
        it("preserves anchors with permitted attributes", () => {
            const result = sanitize("<a href=\"https://example.com\" target=\"_blank\">link</a>");

            expect(result).to.equal("<a href=\"https://example.com\" target=\"_blank\">link</a>");
        });

        it("preserves images with data scheme sources", () => {
            const dataUri = "data:image/png;base64,iVBORw0KGgo=";
            const result = sanitize(`<img src="${dataUri}" alt="dot" height="1" />`);

            expect(result).to.equal(`<img src="${dataUri}" alt="dot" height="1" />`);
        });
    });

    context("when sanitizing disallowed content", () => {
        it("strips script tags but keeps allowed siblings", () => {
            const result = sanitize("<p>hello</p><script>alert(1)</script>");

            expect(result).to.not.contain("<script>");
            expect(result).to.equal("<p>hello</p>");
        });

        it("strips event handler attributes", () => {
            const result = sanitize("<img src=\"x\" onerror=\"alert(1)\" />");

            expect(result).to.not.contain("onerror");
            expect(result).to.equal("<img src=\"x\" />");
        });
    });

    // Regression test for CVE-2026-44990 / CVE-2026-53606: a disallowed <xmp> wrapper could smuggle
    // live markup through the default 'discard' path, allowing stored XSS. Fixed in sanitize-html >= 2.17.5.
    context("when sanitizing the xmp bypass payloads (CVE-2026-44990)", () => {
        it("discards a script payload wrapped in xmp", () => {
            const result = sanitize("<xmp><script>alert(1)</script></xmp>");

            expect(result).to.not.contain("<script>");
            expect(result).to.equal("");
        });

        it("discards an img onerror payload wrapped in xmp", () => {
            const result = sanitize("<xmp><img src=x onerror=alert(1)></xmp>");

            expect(result).to.not.contain("<img");
            expect(result).to.not.contain("onerror");
            expect(result).to.equal("");
        });

        it("discards an svg script payload wrapped in xmp", () => {
            const result = sanitize("<xmp><svg><script>alert(1)</script></svg></xmp>");

            expect(result).to.not.contain("<svg>");
            expect(result).to.not.contain("<script>");
            expect(result).to.equal("");
        });
    });
});
