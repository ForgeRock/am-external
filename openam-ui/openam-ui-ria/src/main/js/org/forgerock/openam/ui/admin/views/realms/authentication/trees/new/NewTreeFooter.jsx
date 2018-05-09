/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, Clearfix } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const NewTreeFooter = ({ disableCreate, onCreateClick, router }) => {
    const cancelHref = Router.getLink(Router.configuration.routes.realmsAuthenticationTrees, [
        encodeURIComponent(router.params[0])
    ]);

    return (
        <Clearfix>
            <div className="pull-right">
                <div className="am-btn-action-group">
                    <Button href={ `#${cancelHref}` }>
                        { t ("common.form.cancel") }
                    </Button>
                    <Button bsStyle="primary" disabled={ disableCreate } onClick={ onCreateClick }>
                        { t("common.form.create") }
                    </Button>
                </div>
            </div>
        </Clearfix>
    );
};

NewTreeFooter.propTypes = {
    disableCreate: PropTypes.bool.isRequired,
    onCreateClick: PropTypes.func.isRequired,
    router: withRouterPropType
};

export default withRouter(NewTreeFooter);
