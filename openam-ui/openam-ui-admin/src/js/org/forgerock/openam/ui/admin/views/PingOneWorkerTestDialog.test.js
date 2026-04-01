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
import sinon from "sinon";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import PingOneWorkerTestDialog from "org/forgerock/openam/ui/admin/views/PingOneWorkerTestDialog";

describe("PingOneWorkerTestDialog", () => {
    let showStub;

    beforeEach(() => {
        showStub = sinon.stub(BootstrapDialog, "show");
    });

    afterEach(() => {
        showStub.restore();
    });

    it("should show success dialog with correct content", () => {
        const results = {
            environmentId: "env-123",
            apiServerUrl: "https://api.pingone.com"
        };
        const workerId = "testWorker";

        PingOneWorkerTestDialog(results, "success");

        expect(showStub).to.have.been.calledOnce;
        const dialogConfig = showStub.firstCall.args[0];

        expect(dialogConfig.type).to.equal(BootstrapDialog.TYPE_PRIMARY);

        const messageHtml = dialogConfig.message.html();
        expect(messageHtml).to.contain("alert-success");
        expect(messageHtml).to.contain("fa-check-circle");
        expect(messageHtml).to.contain("Environment ID");
        expect(messageHtml).to.contain("env-123");
        expect(messageHtml).to.contain("API Server URL");
        expect(messageHtml).to.contain("https://api.pingone.com");
    });

    it("should show error dialog with correct content", () => {
        const results = {
            message: "Something went wrong"
        };
        const workerId = "testWorker";

        PingOneWorkerTestDialog(results, "error");

        expect(showStub).to.have.been.calledOnce;
        const dialogConfig = showStub.firstCall.args[0];

        const messageHtml = dialogConfig.message.html();
        expect(messageHtml).to.contain("alert-danger");
        expect(messageHtml).to.contain("fa-exclamation-triangle");
        expect(messageHtml).to.contain("Something went wrong");

        expect(dialogConfig.buttons[0].cssClass).to.equal("btn-danger");
    });
});
