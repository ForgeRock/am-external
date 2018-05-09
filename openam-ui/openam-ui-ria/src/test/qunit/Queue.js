/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/util/Queue"
], function (Queue) {
    QUnit.module('Queue Functions');

    QUnit.test("core operations", function () {
        var q = new Queue(["a","b"]);

        QUnit.equal(q.peek(), "a");
        QUnit.equal(q.remove(), "a");
        QUnit.equal(q.remove(), "b");
        q.add("c");
        QUnit.equal(q.remove(), "c");
        QUnit.equal(q.peek(), undefined);
        QUnit.equal(q.remove(), undefined);

    });

});
