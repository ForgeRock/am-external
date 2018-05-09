/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, Clearfix } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

const EditWebhookFooter = ({ onUpdateClick }) => {
    return (
        <Clearfix>
            <div className="pull-right">
                <div className="am-btn-action-group">
                    <Button bsStyle="primary" onClick={ onUpdateClick }>
                        { t("common.form.saveChanges") }
                    </Button>
                </div>
            </div>
        </Clearfix>
    );
};

EditWebhookFooter.propTypes = {
    onUpdateClick: PropTypes.func.isRequired
};

export default EditWebhookFooter;
