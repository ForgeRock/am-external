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
 * Copyright 2016-2017 ForgeRock AS.
 */

import { Button } from "react-bootstrap";
import { t } from "i18next";
import React, { PropTypes } from "react";

const EmptySectionCallToAction = ({ onClick, sectionName }) => {
    const onCallToActionClick = () => { onClick(sectionName); };
    return (
        <div className="am-empty-section">
            <Button bsStyle="link" onClick={ onCallToActionClick }>
                <i className="fa fa-plus" /> { t("common.form.addItem", { item: sectionName }) }
            </Button>
        </div>
    );
};

EmptySectionCallToAction.propTypes = {
    onClick: PropTypes.func.isRequired,
    sectionName: PropTypes.string.isRequired
};

export default EmptySectionCallToAction;
