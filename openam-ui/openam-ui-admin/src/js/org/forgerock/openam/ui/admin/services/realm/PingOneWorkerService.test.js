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
import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import { testConnection } from "org/forgerock/openam/ui/admin/services/realm/PingOneWorkerService";

describe("PingOneWorkerService", () => {
    let serviceCallStub;

    beforeEach(() => {
        serviceCallStub = sinon.stub(AbstractDelegate.prototype, "serviceCall");
    });

    afterEach(() => {
        serviceCallStub.restore();
    });

    it("should call serviceCall with correct parameters for testConnection", () => {
        const realm = "/testRealm";
        const workerId = "testWorker";

        testConnection(realm, workerId);

        expect(serviceCallStub).to.have.been.calledOnce;
        const callArgs = serviceCallStub.firstCall.args[0];

        expect(callArgs.type).to.equal("POST");
        expect(callArgs.url).to.contain(`/realm-config/services/pingOneWorkerService/workers/${workerId}?_action=testConnection`);
        expect(callArgs.headers["Accept-API-Version"]).to.equal("protocol=1.0,resource=1.0");
        expect(callArgs.headers["Content-Type"]).to.equal("application/json");
        expect(callArgs.data).to.equal("{}");
    });
});
