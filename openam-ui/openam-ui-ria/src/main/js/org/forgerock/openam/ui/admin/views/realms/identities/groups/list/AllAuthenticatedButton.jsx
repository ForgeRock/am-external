/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button } from "react-bootstrap";
import { t } from "i18next";
import React from "react";

import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const AllAuthenticatedButton = ({ router }) => {
    const realm = router.params[0];
    const href = `#${Router.getLink(Router.configuration.routes.realmsIdentitiesAllAuthenticatedEdit, [
        encodeURIComponent(realm), "allAuthenticatedIdentities"
    ])}`;

    return (
        <Button
            bsStyle="link"
            className="pull-right"
            href={ href }
        >
            <i className="fa fa-users" /> { t("console.identities.groups.list.allAuthenticatedButton") }
        </Button>
    );
};

AllAuthenticatedButton.propTypes = {
    router: withRouterPropType
};

export default withRouter(AllAuthenticatedButton);
