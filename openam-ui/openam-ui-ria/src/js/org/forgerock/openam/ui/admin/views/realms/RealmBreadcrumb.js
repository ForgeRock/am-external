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
 * Copyright 2018 ForgeRock AS.
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
    render (data, pattern) {
        this.data = data;
        this.data.crumbs = createBreadcrumbs(pattern);
        this.parentRender();
    }
}

export default new RealmBreadcrumb();
