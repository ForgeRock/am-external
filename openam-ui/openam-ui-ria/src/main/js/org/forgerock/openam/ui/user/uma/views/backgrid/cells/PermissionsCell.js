/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/openam/ui/common/util/BackgridUtils",
    "templates/user/uma/backgrid/cell/PermissionsCell",

    // jquery dependencies
    "selectize"
], ($, BackgridUtils, PermissionsCellTemplate) => {
    return BackgridUtils.TemplateCell.extend({
        className: "permissions-cell",
        template: PermissionsCellTemplate,
        onChange () {},
        rendered () {
            this.$el.find("select").selectize({
                dropdownParent: "body",
                onChange: this.onChange.bind(this)
            });
        }
    });
});

// TODO: Extend in future with ability to specify selected permissions (currently all are selected)
