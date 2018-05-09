/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import React from "react";

import humanizeRealmPath from "org/forgerock/openam/ui/admin/views/realms/humanizeRealmPath";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

/**
 * Renders the sub navigation realm and link to the default realms page.
 * @module org/forgerock/openam/ui/admin/views/realms/SubNavRealmHomeLink
 * @returns {ReactElement} Renderable React element
 */
const SubNavRealmHomeLink = ({ router }) => {
    const realmPath = router.params[0];
    const realmName = humanizeRealmPath(realmPath);
    const realmHome = `#${Router.getLink(Router.configuration.routes.realmDefault, [
        encodeURIComponent(realmPath)
    ])}`;

    return (
        <span>
            <i className="fa fa-cloud" /> <a href={ realmHome }>{ realmName }</a>
        </span>
    );
};

SubNavRealmHomeLink.propTypes = {
    router: withRouterPropType
};

export default withRouter(SubNavRealmHomeLink);
