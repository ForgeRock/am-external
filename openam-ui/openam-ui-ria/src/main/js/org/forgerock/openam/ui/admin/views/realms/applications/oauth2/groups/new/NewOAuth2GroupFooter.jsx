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
 * Copyright 2017 ForgeRock AS.
 */

import { Button, Clearfix } from "react-bootstrap";
import { t } from "i18next";
import React, { PropTypes } from "react";

import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const NewOAuth2GroupFooter = ({ disableCreate, onCreateClick, router }) => {
    return (
        <Clearfix>
            <div className="pull-right">
                <div className="am-btn-action-group">
                    <Button
                        href={ `#${Router.getLink(Router.configuration.routes.realmsApplicationsOAuth2,
                            [encodeURIComponent(router.params[0])])}` }
                    >
                        { t ("common.form.cancel") }
                    </Button>
                    <Button
                        bsStyle="primary"
                        disabled={ disableCreate }
                        onClick={ onCreateClick }
                    >
                        { t("common.form.create") }
                    </Button>
                </div>
            </div>
        </Clearfix>
    );
};

NewOAuth2GroupFooter.propTypes = {
    disableCreate: PropTypes.bool.isRequired,
    onCreateClick: PropTypes.func.isRequired,
    router: withRouterPropType
};

export default withRouter(NewOAuth2GroupFooter);
