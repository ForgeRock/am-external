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
 * Copyright 2016-2018 ForgeRock AS.
 */
 /*globals QUnit */

define([
    "jquery",
    "sinon",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function ($, sinon, AbstractCollection, ServiceInvoker) {
    QUnit.module('AbstractCollection Functions');

    QUnit.test("query operations", function () {
        var testCollection = new AbstractCollection(),
            restCallArg;

        testCollection.url = "/crestResource?_queryFilter=true";

        sinon.stub(ServiceInvoker, "restCall", function (options) {
            var response = {
                "result": [{
                    "_id": 1,
                    "givenName": "Boaty",
                    "sn": "McBoatface"
                },{
                    "_id": 2,
                    "givenName": "Testy",
                    "sn": "Testerton"
                }],
                "resultCount": 2,
                "pagedResultsCookie": "2",
                "totalPagedResultsPolicy": "EXACT",
                "totalPagedResults": 5
            };
            // backbone uses the success handler associated with the fetch request to invoke the parse method
            if (options.success) {
                options.success(response);
            }
            return $.Deferred().resolve(response);
        });

        testCollection.setPageSize(2, {fetch: false});
        testCollection.setSorting("givenName");
        testCollection.setPagingType("cookie");
        testCollection.setTotalPagedResultsPolicy("EXACT");

        testCollection.getFirstPage().then(function () {
            QUnit.equal(ServiceInvoker.restCall.callCount, 1, "Only one REST call produced");
            restCallArg = ServiceInvoker.restCall.args[0][0]; // first invocation, first argument
            QUnit.equal(testCollection.length, 2, "collection contains two records from the backend");
            QUnit.equal(testCollection.where({givenName: "Boaty"}).length, 1,
                "able to find expected model content in collection");
            QUnit.ok(testCollection.hasNext(), "response with cookie indicates that hasNext is true");
            QUnit.equal(testCollection.state.totalRecords, 5, "Total records correctly populated in collection state");
            QUnit.equal(testCollection.state.totalPages, 3, "Total pages correctly populated in collection state");
            QUnit.equal(restCallArg.url, "/crestResource", "correct url used to query backend");
            QUnit.equal(restCallArg.data,
                "_queryFilter=true&_pageSize=2&_sortKeys=givenName&_totalPagedResultsPolicy=EXACT",
                "correct data submitted to backend for first page");
        }).then(function () {
            testCollection.setSorting("givenName", 1);
            return testCollection.getFirstPage();
        }).then(function () {
            restCallArg = ServiceInvoker.restCall.args[1][0]; // second invocation, first argument
            QUnit.equal(restCallArg.data,
                "_queryFilter=true&_pageSize=2&_sortKeys=-givenName&_totalPagedResultsPolicy=EXACT",
                "correct data submitted to backend for descending sortKey");
        }).then(function () {
            return testCollection.getNextPage();
        }).then(function () {
            restCallArg = ServiceInvoker.restCall.args[2][0]; // third invocation, first argument
            QUnit.equal(restCallArg.data,
                "_queryFilter=true&_pageSize=2&_sortKeys=-givenName"+
                "&_totalPagedResultsPolicy=EXACT&_pagedResultsCookie=2",
                "correct data submitted to backend for next page");
        }).then(function () {
            ServiceInvoker.restCall.restore();
        });

    });

});
