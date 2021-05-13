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
 * Copyright 2016-2019 ForgeRock AS.
 */

import _ from "lodash";
import { Badge } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import CallToAction
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/emptySection/CallToAction";
import NoItemAvailable
    from "org/forgerock/openam/ui/admin/views/realms/authorization/policies/summary/emptySection/NoItemAvailable";

const ActionsSummary = ({ actionValues, isReadOnly, onClick }) => {
    let content;
    if (_.isEmpty(actionValues)) {
        content = isReadOnly
            ? <NoItemAvailable sectionName={ t("console.authorization.common.actions") } />
            : <CallToAction onClick={ onClick } sectionName={ t("console.authorization.common.actions") } />;
    } else {
        content = (
            <span className="am-badge-group">
                {
                    _(actionValues).mapValues((value, key) => (
                        <Badge key={ key }>{ key.toUpperCase() }: {
                            value ? t("console.authorization.common.allowed") : t("console.authorization.common.denied")
                        }
                        </Badge>
                    )).values().value()
                }
            </span>
        );
    }
    return content;
};

ActionsSummary.propTypes = {
    actionValues: PropTypes.objectOf(PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.bool
    ])),
    isReadOnly: PropTypes.bool.isRequired,
    onClick: PropTypes.func.isRequired
};

export default ActionsSummary;
