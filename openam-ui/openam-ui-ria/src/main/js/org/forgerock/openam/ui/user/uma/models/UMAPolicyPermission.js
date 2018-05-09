/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "backbone",
    "backbone-relational",
    "org/forgerock/openam/ui/user/uma/models/UMAPolicyPermissionScope"
], (Backbone, BackboneRelational, UMAPolicyPermissionScope) => {
    return Backbone.RelationalModel.extend({
        idAttribute: "subject",
        relations: [{
            type: Backbone.HasMany,
            key: "scopes",
            relatedModel: UMAPolicyPermissionScope,
            includeInJSON: Backbone.Model.prototype.idAttribute,
            parse: true
        }],
        validate (attributes) {
            if (!attributes.subject) { return "no subject"; } // FIXME i18n
            if (!attributes.scopes || !attributes.scopes.length) { return "no scopes"; } // FIXME i18n
        }
    });
});
