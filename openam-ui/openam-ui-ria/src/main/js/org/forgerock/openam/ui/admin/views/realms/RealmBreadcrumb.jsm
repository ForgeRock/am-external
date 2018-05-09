/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import createBreadcrumbs from "org/forgerock/openam/ui/admin/views/common/navigation/createBreadcrumbs";
import RealmsBreadcrumbTemplate from "templates/admin/views/realms/RealmsBreadcrumbTemplate";
import BreadcrumbPartial from "partials/breadcrumb/_Breadcrumb";
import BreadcrumbTitlePartial from "partials/breadcrumb/_BreadcrumbTitle";

class RealmBreadcrumb extends AbstractView {
    constructor () {
        super();
        this.element = "#breadcrumbContent";
        this.template = RealmsBreadcrumbTemplate;
        this.partials = {
            "breadcrumb/_Breadcrumb": BreadcrumbPartial,
            "breadcrumb/_BreadcrumbTitle": BreadcrumbTitlePartial
        };
    }
    render (title, pattern) {
        this.data.crumbs = createBreadcrumbs(pattern);
        this.data.title = title;
        this.parentRender();
    }
}

export default new RealmBreadcrumb();
