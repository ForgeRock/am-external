/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { ButtonToolbar, Button } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

import EditTreeToolbarButton from "./EditTreeToolbarButton";
import EditTreeToolbarDivider from "./EditTreeToolbarDivider";
import EditTreeToolbarTooltip from "./EditTreeToolbarTooltip";

const EditTreeToolbar = ({
    invertTooltipPlacement,
    isDeleteNodeEnabled,
    onAutoLayout,
    onFullscreenToggle,
    onNodeDelete,
    onTreeSave
}) => {
    const tooltipPlacement = invertTooltipPlacement ? "bottom" : "top";
    const autoLayoutTitle = t("console.authentication.trees.edit.toolbar.autoLayout");
    const fullscreenTitle = t("console.authentication.trees.edit.toolbar.fullscreen");
    const deleteNodeTitle = t("console.authentication.trees.edit.toolbar.deleteNode");

    return (
        <ButtonToolbar className="authtree-btn-toolbar">

            <EditTreeToolbarTooltip
                placement={ tooltipPlacement }
                tooltip={ autoLayoutTitle }
            >
                <EditTreeToolbarButton
                    onClick={ onAutoLayout }
                    title={ autoLayoutTitle }
                >
                    <i className="fa fa-sitemap fa-rotate-270" />
                </EditTreeToolbarButton>
            </EditTreeToolbarTooltip>

            <EditTreeToolbarTooltip
                placement={ tooltipPlacement }
                tooltip={ fullscreenTitle }
            >
                <EditTreeToolbarButton
                    onClick={ onFullscreenToggle }
                    title={ fullscreenTitle }
                >
                    <i className="fa fa-arrows-alt" />
                </EditTreeToolbarButton>
            </EditTreeToolbarTooltip>

            <EditTreeToolbarDivider />

            <EditTreeToolbarTooltip
                placement={ tooltipPlacement }
                tooltip={ deleteNodeTitle }
            >
                <EditTreeToolbarButton
                    disabled={ !isDeleteNodeEnabled }
                    onClick={ onNodeDelete }
                    title={ deleteNodeTitle }
                >
                    <i className="fa fa-trash" />
                </EditTreeToolbarButton>
            </EditTreeToolbarTooltip>

            <Button bsStyle="primary" className="pull-right" onClick={ onTreeSave }>
                { t("common.form.save") }
            </Button>
        </ButtonToolbar>
    );
};

EditTreeToolbar.propTypes = {
    invertTooltipPlacement: PropTypes.bool.isRequired,
    isDeleteNodeEnabled: PropTypes.bool.isRequired,
    onAutoLayout: PropTypes.func.isRequired,
    onFullscreenToggle: PropTypes.func.isRequired,
    onNodeDelete: PropTypes.func.isRequired,
    onTreeSave: PropTypes.func.isRequired
};

export default EditTreeToolbar;
