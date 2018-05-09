/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone"
], (Backbone) => {
    var User = Backbone.Model.extend({
        initialize (username) {
            this.username = username;
        }
    });

    return User;
});
