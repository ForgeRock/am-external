/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import { Badge, Button, ButtonGroup, ControlLabel } from "react-bootstrap";
import { t } from "i18next";
import moment from "moment";
import PropTypes from "prop-types";
import React from "react";

const SessionsTableRow = ({ checked, data, onDelete, onSelect, sessionHandle }) => {
    const handleDelete = () => onDelete(data);
    const handleSelect = (event) => onSelect(data, event.target.checked);
    const selectId = _.uniqueId("select");
    const rowActions = data.sessionHandle === sessionHandle
        ? <Badge>{ t("console.sessions.yourSession") }</Badge>
        : (
            <ButtonGroup className="pull-right">
                <Button bsStyle="link" onClick={ handleDelete } title={ t("console.sessions.invalidate") }>
                    <i className="fa fa-close" />
                </Button>
            </ButtonGroup>
        );

    return (
        <tr className={ checked ? "selected" : undefined } >
            <td>
                <ControlLabel htmlFor={ selectId } srOnly>{ t("common.form.select") }</ControlLabel>
                <input
                    checked={ checked }
                    disabled={ data.sessionHandle === sessionHandle }
                    id={ selectId }
                    onChange={ handleSelect }
                    type="checkbox"
                />
            </td>

            <td>{ moment(data.latestAccessTime).fromNow(true) }</td>

            <td
                title={ t("console.sessions.table.expires", {
                    timestamp: moment(data.maxIdleExpirationTime).toISOString()
                }) }
            >
                { moment(data.maxIdleExpirationTime).fromNow(true) }
            </td>

            <td
                title={ t("console.sessions.table.expires", {
                    timestamp: moment(data.maxSessionExpirationTime).toISOString()
                }) }
            >
                { moment(data.maxSessionExpirationTime).fromNow(true) }
            </td>

            <td className="fr-col-btn-1">
                { rowActions }
            </td>
        </tr>
    );
};

SessionsTableRow.propTypes = {
    checked: PropTypes.bool.isRequired,
    data: PropTypes.arrayOf(PropTypes.shape({
        latestAccessTime: PropTypes.string.isRequired,
        maxIdleExpirationTime: PropTypes.string.isRequired,
        maxSessionExpirationTime: PropTypes.string.isRequired,
        sessionHandle: PropTypes.string.isRequired
    })).isRequired,
    onDelete: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    sessionHandle: PropTypes.string.isRequired
};

export default SessionsTableRow;
