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
 * Copyright 2018-2022 ForgeRock AS.
 */

import { identity, isEmpty, omit } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import dataFieldObjectPath from "components/table/cells/dataFieldObjectPath";
import dataFormatReact from "components/table/cells/dataFormatReact";
import EmphasizedTextCell from "components/table/cells/EmphasizedTextCell";
import FontAwesomeIconCell from "components/table/cells/FontAwesomeIconCell";
import List from "components/list/List";
import SearchFieldWithReset from "org/forgerock/openam/ui/admin/views/realms/common/SearchFieldWithReset";
import StatusCell from "components/table/cells/StatusCell";

class ListUsers extends Component {
    static propTypes = {
        isSearching: PropTypes.bool.isRequired,
        items: PropTypes.arrayOf(PropTypes.any),
        newHref: PropTypes.string.isRequired,
        onSearchKeyPress: PropTypes.func.isRequired
    };

    state = { searchTerm: "" };

    handleSearchClear = () => {
        this.setState({ searchTerm: "" }, () => {
            this.props.onSearchKeyPress(this.state.searchTerm);
        });
    };

    handleSearchChange = (event) => {
        this.setState({ searchTerm: event.currentTarget.value });
    };

    handleSearchKeyPress = (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            this.setState({ searchTerm: event.currentTarget.value });
            this.props.onSearchKeyPress(this.state.searchTerm);
        }
    };

    render () {
        const columns = [{
            title: identity,
            dataField: "username",
            formatter: dataFormatReact(
                <FontAwesomeIconCell icon="address-card" >
                    <EmphasizedTextCell match={ this.state.searchTerm } />
                </FontAwesomeIconCell>
            ),
            sort: true,
            text: t("console.identities.users.list.grid.0")
        }, {
            title: true,
            dataField: "cn",
            formatter: dataFieldObjectPath(dataFormatReact(
                <EmphasizedTextCell match={ this.state.searchTerm } />
            ), "[0]"),
            text: t("console.identities.users.list.grid.1")
        }, {
            dataField: "mail",
            formatter: dataFieldObjectPath(dataFormatReact(
                <EmphasizedTextCell match={ this.state.searchTerm } />
            ), "[0]"),
            text: t("console.identities.users.list.grid.2")
        }, {
            dataField: "inetUserStatus",
            formatter: dataFieldObjectPath(StatusCell, "[0]"),
            text: t("console.identities.users.list.grid.3")
        }];
        const list = (
            <List
                { ...omit(this.props, "children") }
                addButton={ {
                    href: this.props.newHref,
                    title: t("console.identities.users.list.callToAction.button")
                } }
                columns={ columns }
                description={ t("console.identities.users.list.callToAction.description") }
                keyField="username"
                title={ t("console.identities.users.list.callToAction.title") }
            />
        );
        const noResults = (
            t("console.identities.users.list.noSuchUser")
        );
        const content = this.props.isSearching && isEmpty(this.props.items) ? noResults : list;
        return (
            <Panel className="fr-panel-tab">
                <Panel.Body>
                    <SearchFieldWithReset
                        isResetEnabled={ !!this.state.searchTerm }
                        label={ t("console.identities.users.list.search") }
                        onChange={ this.handleSearchChange }
                        onClear={ this.handleSearchClear }
                        onKeyPress={ this.handleSearchKeyPress }
                        placeholder={ t("console.identities.users.list.search") }
                        value={ this.state.searchTerm }
                    />
                    { content }
                </Panel.Body>
            </Panel>
        );
    }
}

export default ListUsers;
