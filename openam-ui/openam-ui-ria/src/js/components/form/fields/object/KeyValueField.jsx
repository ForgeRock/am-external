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
 * Copyright 2017-2022 ForgeRock AS.
 */

import { Button, FormControl, Form, Table as BootstrapTable } from "react-bootstrap";
import cellEditFactory from "react-bootstrap-table2-editor";
import { map, omit, partial } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import ReactBootstrapTable from "components/table/Table";

const initialState = {
    error: false,
    key: "",
    showAdd: true,
    value: ""
};

class KeyValueField extends Component {
    constructor () {
        super();

        this.state = initialState;

        this.handleAddClick = this.handleAddClick.bind(this);
        this.handleAfterSaveCell = this.handleAfterSaveCell.bind(this);
        this.handleChangeKey = this.handleChangeKey.bind(this);
        this.handleChangeValue = this.handleChangeValue.bind(this);
        this.handleRowDelete = this.handleRowDelete.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.rowDeleteComponent = this.rowDeleteComponent.bind(this);
        this.rowTextComponent = this.rowTextComponent.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    handleAddClick () {
        this.setState({ showAdd: false });
    }

    handleAfterSaveCell (before, after, { key, value }) {
        this.props.onChange({
            ...this.props.formData,
            [key]: value
        });
    }

    handleChangeKey (event) {
        this.setState({ key: event.target.value });
    }

    handleChangeValue (event) {
        this.setState({ value: event.target.value });
    }

    handleRowDelete (row) {
        this.props.onChange(omit(this.props.formData, row.key));
    }

    handleSubmit () {
        this.props.onChange({
            ...this.props.formData,
            [this.state.key]: this.state.value
        });
        this.setState(initialState);
    }

    rowDeleteComponent (data, row) {
        return (
            <Button bsSize="small" bsStyle="link" onClick={ partial(this.handleRowDelete, row) }>
                <i className="fa fa-trash" />
            </Button>
        );
    }

    rowTextComponent (data) {
        return <span title={ data }>{ data }</span>;
    }

    setRef (element) {
        this.tableRef = element;
    }

    render () {
        const arrayData = map(this.props.formData, (value, key) => ({ key, value }));
        const newKeyValueComponent = this.state.showAdd
            ? null
            : (
                <Form onSubmit={ this.handleSubmit }>
                    <BootstrapTable condensed>
                        <tbody>
                            <tr>
                                <td>
                                    { /* eslint-disable jsx-a11y/no-autofocus */ }
                                    <FormControl
                                        autoFocus
                                        bsSize="small"
                                        onChange={ this.handleChangeKey }
                                        placeholder={ t("common.form.key") }
                                        type="text"
                                        value={ this.state.key }
                                    />
                                    { /* eslint-enable */ }
                                </td>
                                <td>
                                    <FormControl
                                        bsSize="small"
                                        onChange={ this.handleChangeValue }
                                        placeholder={ t("common.form.value") }
                                        type="text"
                                        value={ this.state.value }
                                    />
                                </td>
                                <td className="fr-col-btn-1">
                                    <Button bsSize="small" type="submit">
                                        <i className="fa fa-plus" />
                                    </Button>
                                </td>
                            </tr>
                        </tbody>
                    </BootstrapTable>
                </Form>
            );
        const errorComponent = this.state.error
            ? <span className="text-danger small">{ this.state.error }</span>
            : null;
        const addButton = this.state.showAdd
            ? (
                <Button bsSize="small" className="pull-right" onClick={ this.handleAddClick }>
                    { t("common.form.add") }
                </Button>
            )
            : null;

        const columns = [{
            dataField: "key",
            formatter: this.rowTextComponent,
            sort: false,
            text: t("common.form.key")
        }, {
            dataField: "value",
            formatter: this.rowTextComponent,
            text: t("common.form.value")
        }, {
            formatter: this.rowDeleteComponent,
            text: "",
            align: "center",
            classes: "fr-col-btn-1",
            editable: false
        }];

        return (
            <div className="key-value-field clearfix">
                <ReactBootstrapTable
                    cellEdit={ cellEditFactory({
                        afterSaveCell: this.handleAfterSaveCell,
                        mode: "click"
                    }) }
                    columns={ columns }
                    condensed
                    data={ arrayData }
                    idField="key"
                    keyField="key"
                    selectRow={ { hideSelectColumn: true } }
                    tableRef={ this.setRef }
                />
                { newKeyValueComponent }
                { errorComponent }
                { addButton }
            </div>
        );
    }
}

KeyValueField.propTypes = {
    formData: PropTypes.objectOf(PropTypes.string).isRequired,
    onChange: PropTypes.func.isRequired
};

export default KeyValueField;
