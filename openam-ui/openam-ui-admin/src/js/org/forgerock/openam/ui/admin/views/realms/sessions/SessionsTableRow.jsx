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
import { Badge, Button, ButtonGroup, ControlLabel } from "react-bootstrap";
import { t } from "i18next";
import moment from "moment";
import momentDurationFormatSetup from "moment-duration-format";
import PropTypes from "prop-types";
import React, { Component } from "react";

const momentSetup = (() => { momentDurationFormatSetup(moment); })(); // eslint-disable-line no-unused-vars

const durationFromNow = (time) => {
    const duration = Math.abs(moment(time).diff(moment.now(), "seconds"));
    const formatString = (duration < 300) ? "s _" : "d _ h _  m _ s _";
    return moment.duration(duration, "seconds").format(formatString, { trim: "both" });
};

class SessionsTableRow extends Component {
    handleDelete = () => {
        this.props.onDelete(this.props.data);
    };

    handleSelect = (event) => {
        this.props.onSelect(this.props.data, event.target.checked);
    };

    render () {
        const { checked, data, sessionHandle } = this.props;
        const selectId = _.uniqueId("select");
        const rowActions = data.sessionHandle === sessionHandle
            ? <Badge>{ t("console.sessions.yourSession") }</Badge>
            : (
                <ButtonGroup className="pull-right">
                    <Button bsStyle="link" onClick={ this.handleDelete } title={ t("console.sessions.invalidate") }>
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
                        onChange={ this.handleSelect }
                        type="checkbox"
                    />
                </td>

                <td>{ durationFromNow(data.latestAccessTime) }</td>

                <td
                    title={ t("console.sessions.table.expires", {
                        timestamp: moment(data.maxIdleExpirationTime).toISOString()
                    }) }
                >
                    { durationFromNow(data.maxIdleExpirationTime) }
                </td>

                <td
                    title={ t("console.sessions.table.expires", {
                        timestamp: moment(data.maxSessionExpirationTime).toISOString()
                    }) }
                >
                    { durationFromNow(data.maxSessionExpirationTime) }
                </td>

                <td className="fr-col-btn-1">
                    { rowActions }
                </td>
            </tr>
        );
    }
}

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
