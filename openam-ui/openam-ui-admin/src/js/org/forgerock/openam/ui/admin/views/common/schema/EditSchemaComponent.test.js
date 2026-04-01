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
import EditSchemaComponent from "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent";
import PingOneWorkerService from "org/forgerock/openam/ui/admin/services/realm/PingOneWorkerService";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

describe("EditSchemaComponent", () => {
    let component;
    let testConnectionStub;
    let dialogShowStub;
    let updateInstanceStub;
    let messagesStub;

    beforeEach(() => {
        testConnectionStub = sinon.stub(PingOneWorkerService, "testConnection");
        dialogShowStub = sinon.stub(BootstrapDialog, "show");
        updateInstanceStub = sinon.stub().returns(Promise.resolve());
        messagesStub = sinon.stub(Messages, "addMessage");

        component = new EditSchemaComponent({
            data: {
                realmPath: "/testRealm",
                id: "testWorker",
                schema: {
                    isCollection: () => false,
                    raw: { properties: {} }
                },
                values: {
                    revertFalseCollections: () => ({ raw: {} }),
                    removeNullPasswords: () => ({ toJSON: () => ({}) })
                }
            },
            getInstance: () => Promise.resolve({ schema: { isCollection: () => false, raw: { properties: {} } }, values: { revertFalseCollections: () => ({ raw: {} }) } }),
            getSubSchemaTypes: () => Promise.resolve({ result: [] }),
            template: () => "",
            updateInstance: updateInstanceStub
        });

        component.getJSONSchemaView = () => ({
            isValid: () => true,
            getData: () => ({})
        });
    });

    afterEach(() => {
        testConnectionStub.restore();
        dialogShowStub.restore();
        messagesStub.restore();
    });

    it("should call onSaveAndTestConnection and show success dialog on success", async () => {
        const response = {environmentId: "env-123"};
        testConnectionStub.returns(Promise.resolve(response));

        await component.onSaveAndTestConnection();

        expect(updateInstanceStub).to.have.been.calledOnce;
        expect(testConnectionStub).to.have.been.calledWith("/testRealm", "testWorker");
        expect(dialogShowStub).to.have.been.calledOnce;
        const dialogConfig = dialogShowStub.firstCall.args[0];
        expect(dialogConfig.message.html()).to.contain("env-123");
    });

    it("should show error dialog on onSaveAndTestConnection failure", async () => {
        const error = {
            status: 400,
            responseJSON: {
                code: 400,
                message: "Bad Request"
            }
        };
        testConnectionStub.returns(Promise.reject(error));

        await component.onSaveAndTestConnection();

        expect(updateInstanceStub).to.have.been.calledOnce;
        expect(testConnectionStub).to.have.been.calledWith("/testRealm", "testWorker");
        expect(dialogShowStub).to.have.been.calledOnce;
        const dialogConfig = dialogShowStub.firstCall.args[0];
        expect(dialogConfig.message.html()).to.contain("alert-danger");
    });

    it("should show error dialog with connection details on onSaveAndTestConnection failure", async () => {
        const error = {
            status: 400,
            responseJSON: {
                code: 400,
                message: "Bad Request",
                detail: {
                    environmentId: "env-123",
                    apiServerUrl: "https://api.pingone.com",
                    authServerUrl: "https://auth.pingone.com"
                }
            }
        };
        testConnectionStub.returns(Promise.reject(error));

        await component.onSaveAndTestConnection();

        expect(updateInstanceStub).to.have.been.calledOnce;
        expect(testConnectionStub).to.have.been.calledWith("/testRealm", "testWorker");
        expect(dialogShowStub).to.have.been.calledOnce;
        const dialogConfig = dialogShowStub.firstCall.args[0];
        expect(dialogConfig.message.html()).to.contain("alert-danger");
        expect(dialogConfig.message.html()).to.contain("env-123");
        expect(dialogConfig.message.html()).to.contain("https://api.pingone.com");
        expect(dialogConfig.message.html()).to.contain("https://auth.pingone.com");
    });
});
