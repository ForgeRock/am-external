/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, Clearfix } from "react-bootstrap";
import { map } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const Footer = ({ backRoute, backRouteArgs, isCreateAllowed, onCreateClick, router }) => {
    const realm = router.params[0];
    const backLinkArguments = map([realm, ...backRouteArgs], encodeURIComponent);
    const backLink = `#${Router.getLink(backRoute, backLinkArguments)}`;

    return (
        <Clearfix>
            <div className="pull-right">
                <div className="am-btn-action-group">
                    <Button href={ backLink } >
                        { t ("common.form.cancel") }
                    </Button>
                    <Button
                        bsStyle="primary"
                        disabled={ !isCreateAllowed }
                        onClick={ onCreateClick }
                    >
                        { t("common.form.create") }
                    </Button>
                </div>
            </div>
        </Clearfix>
    );
};

Footer.propTypes = {
    backRoute: PropTypes.objectOf({
        url: PropTypes.string.isRequired,
        pattern: PropTypes.string.isRequired
    }).isRequired,
    backRouteArgs: PropTypes.arrayOf(PropTypes.string),
    isCreateAllowed: PropTypes.bool.isRequired,
    onCreateClick: PropTypes.func.isRequired,
    router: withRouterPropType
};

Footer.defaultProps = {
    backRouteArgs: []
};

export default withRouter(Footer);
