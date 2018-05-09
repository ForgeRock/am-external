/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

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
