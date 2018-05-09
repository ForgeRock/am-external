/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function ($, sinon, AbstractModel, ServiceInvoker) {
    QUnit.module('AbstractModel Functions');

    QUnit.test("create with server-assigned id", function () {
        var testModel = new AbstractModel(),
            newRecord = {
                "foo": "bar",
                "hello": "world"
            },
            restCallArg;

        testModel.url = "/crestResource";

        sinon.stub(ServiceInvoker, "restCall", function (opts) {
            return $.Deferred().resolve(_.extend(JSON.parse(opts.data), {
                "_id": 1,
                "_rev": 1
            }));
        });

        testModel.save(newRecord).then(function () {
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument
            QUnit.equal(testModel.id, 1, "Newly-created model has id from backend");
            QUnit.equal(restCallArg.url, "/crestResource?_action=create&", "correct url used to create model");
            QUnit.equal(restCallArg.type, "POST", "correct method used to create model");
            ServiceInvoker.restCall.restore();
        });

    });

    QUnit.test("create with client-supplied id", function () {
        var testModel = new AbstractModel(),
            newRecord = {
                "foo": "bar",
                "hello": "world"
            },
            restCallArg;

        testModel.url = "/crestResource";
        testModel.id = "myCustomId";

        sinon.stub(ServiceInvoker, "restCall", function (opts) {
            return $.Deferred().resolve(_.extend(JSON.parse(opts.data), {
                "_rev": 1
            }));
        });

        testModel.save(newRecord).then(function () {
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument
            QUnit.equal(testModel.get("_rev"), 1, "Model has new rev from backend");
            QUnit.equal(restCallArg.url, "/crestResource/myCustomId?", "correct url used to create model");
            QUnit.equal(restCallArg.headers["If-None-Match"], "*", "correct revision header provided");
            QUnit.equal(restCallArg.type, "PUT", "correct method used to create model");

            ServiceInvoker.restCall.restore();
        })
    });

    QUnit.test("read operation", function () {
        var testModel = new AbstractModel(),
            restCallArg;

        testModel.url = "/crestResource";
        testModel.id = 1;

        sinon.stub(ServiceInvoker, "restCall", function () {
            return $.Deferred().resolve({
                "_id": 1,
                "_rev": 1,
                "name": "foo"
            });
        });

        testModel.fetch().then(function () {
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument
            QUnit.equal(testModel.get("name"), "foo", "example data populated from fetch call");
            QUnit.equal(testModel.get("_rev"), 1, "revision populated from fetch call");
            QUnit.equal(restCallArg.url, "/crestResource/1?", "correct url used to read model");
            QUnit.equal(restCallArg.type, "GET", "correct method used to read model");
        }).then(function () {
            testModel.additionalParameters = {
                "_fields": "name"
            };
            return testModel.fetch();
        }).then(function () {
            restCallArg = ServiceInvoker.restCall.args[1][0]; // second invocation, first argument
            QUnit.equal(restCallArg.url, "/crestResource/1?_fields=name", "url includes additionalParameters");
        }).then(function () {
            testModel.parse = function (response) {
                return _.extend({"addedByParseFunction": true}, response);
            };
            return testModel.fetch();
        }).then(function () {
            QUnit.equal(testModel.get("addedByParseFunction"), true, "parse function successfully modified model content");
            ServiceInvoker.restCall.restore();
        });
    });

    QUnit.test("update operations", function () {
        var testModel = new AbstractModel({
                "_id": 1,
                "_rev": 1,
                "foo": "bar",
                "hello": "world"
            }),
            restCallArg;

        testModel.url = "/crestResource";

        sinon.stub(ServiceInvoker, "restCall", function (opts) {
            return $.Deferred().resolve(_.extend(JSON.parse(opts.data), {
                "_rev": 2
            }));
        });

        testModel.save().then(function () {
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument
            QUnit.equal(testModel.get("_rev"), 2, "Model has new rev from backend");
            QUnit.equal(restCallArg.url, "/crestResource/1?", "correct url used to update model");
            QUnit.equal(restCallArg.headers["If-Match"], 1, "correct revision header provided");
            QUnit.equal(restCallArg.type, "PUT", "correct method used to update model");

            ServiceInvoker.restCall.restore();
        });
    });

    QUnit.test("delete operations", function () {
        var testModel = new AbstractModel({
                "_id": 1,
                "_rev": 1,
                "foo": "bar",
                "hello": "world"
            }),
            restCallArg;

        testModel.url = "/crestResource";

        sinon.stub(ServiceInvoker, "restCall", function (opts) {
            return $.Deferred().resolve();
        });

        testModel.destroy().then(function () {
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument
            QUnit.equal(restCallArg.url, "/crestResource/1?", "correct url used to delete model");
            QUnit.equal(restCallArg.type, "DELETE", "correct method used to DELETE model");

            ServiceInvoker.restCall.restore();
        });
    });

    QUnit.test("patch operations", function () {
        var testModel = new AbstractModel({
                "_id": 1,
                "_rev": 1,
                "foo": "bar",
                "hello": "world"
            }),
            restCallArg;

        testModel.url = "/crestResource";

        sinon.stub(ServiceInvoker, "restCall", function (opts) {
            return $.Deferred().resolve();
        });

        testModel.save({"foo": "baz"}, {patch: true}).then(function () {
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument

            QUnit.equal(restCallArg.url, "/crestResource/1?", "correct url used to patch model");
            QUnit.equal(restCallArg.type, "PATCH", "correct method used to patch model");
            QUnit.equal(restCallArg.data, '[{"operation":"replace","field":"/foo","value":"baz"}]', "correct patch content provided");

            ServiceInvoker.restCall.restore();
        });
    });

    QUnit.test("custom get method to support JSONPointer", function () {
        var testModel = new AbstractModel({
                "_id": 1,
                "_rev": 1,
                "simpleKey": "simpleValue",
                "foo": {
                    "hello": "world"
                }
            });
        QUnit.equal(testModel.get("simpleKey"), "simpleValue", "basic get behavior used to get simple value");
        QUnit.equal(testModel.get("/simpleKey"), "simpleValue", "jsonpointer used to get simple value");
        QUnit.equal(testModel.get("/foo/hello"), "world", "jsonpointer used to get deeply-nested value");
    })

});
