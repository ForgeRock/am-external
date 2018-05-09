/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Button, FormControl, Form, Table as BootstrapTable } from "react-bootstrap";
import { map, omit, partial } from "lodash";
import { t } from "i18next";
import { TableHeaderColumn } from "react-bootstrap-table";
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
        this.handleChangeKey = this.handleChangeKey.bind(this);
        this.handleChangeValue = this.handleChangeValue.bind(this);
        this.handleRowDelete = this.handleRowDelete.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.setRef = this.setRef.bind(this);
    }

    handleAddClick () {
        this.setState({ showAdd: false });
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
        const error = this.tableRef.handleAddRow(this.state);

        if (error) {
            this.setState({ error });
        } else {
            this.props.onChange({
                ...this.props.formData,
                [this.state.key]: this.state.value
            });
            this.setState(initialState);
        }
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
        const rowTextComponent = (data) => <span title={ data }>{ data }</span>;
        const rowDeleteComponent = (data, row) => (
            <Button bsSize="small" bsStyle="link" onClick={ partial(this.handleRowDelete, row) }>
                <i className="fa fa-trash" />
            </Button>
        );
        const addButton = this.state.showAdd
            ? (
                <Button bsSize="small" className="pull-right" onClick={ this.handleAddClick }>
                    { t("common.form.add") }
                </Button>
            )
            : null;

        return (
            <div className="key-value-field clearfix">
                <ReactBootstrapTable
                    cellEdit={ { mode: "click" } }
                    condensed
                    data={ arrayData }
                    idField="key"
                    onSelectedChange={ this.handleOnSelectedChange }
                    selectRow={ { hideSelectColumn: true } }
                    tableRef={ this.setRef }
                >
                    <TableHeaderColumn dataField="key" dataFormat={ rowTextComponent } isKey>
                        { t("common.form.key") }
                    </TableHeaderColumn>
                    <TableHeaderColumn dataField="value" dataFormat={ rowTextComponent }>
                        { t("common.form.value") }
                    </TableHeaderColumn>
                    <TableHeaderColumn
                        columnClassName="fr-col-btn-1"
                        dataAlign="center"
                        dataFormat={ rowDeleteComponent }
                        editable={ false }
                    />
                </ReactBootstrapTable>
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
