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
 * Copyright 2018-2019 ForgeRock AS.
 */

import { arrayMove, SortableContainer, SortableElement } from "react-sortable-hoc";
import { find, findKey, isBoolean, isEmpty, isObject, map, mapValues, sortBy } from "lodash";
import { ListGroup, Well } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";
import ScrollIntoViewIfNeeded from "react-scroll-into-view-if-needed";

import ListItem from "./components/ListItem";
import ObjectInputControl from "./components/ObjectInputControl";
import StringInputControl from "./components/StringInputControl";

const SortableItem = SortableElement((props) => <ListItem { ...props } />);
const SortableArray = SortableContainer((
    { components, isDisabled, isSorting, items, onDelete, onEdit, itemSchema, lastTouchedItemIndex, scrollTo,
        ...restProps }) => {
    const isSortable = items.length > 1 && !isDisabled;
    const { Item } = components;

    const sortableItems = items.map((value, index) => {
        const content = isObject(itemSchema.properties)
            ? (
                <dl style={ { marginBottom: 0 } }>
                    {
                        map(sortBy(itemSchema.properties, "propertyOrder"), (property) => {
                            const key = findKey(itemSchema.properties, property);

                            const displayValue = property.type === "boolean" && isBoolean(value[key])
                                ? value[key].toString()
                                : value[key];

                            const displayKey = property.title || key;

                            return (
                                <span key={ `${displayKey}-${displayValue}` }>
                                    <dt className="small">{ displayKey }</dt>
                                    <dd style={ { marginLeft: 20 } } >{ displayValue } </dd>
                                </span>
                            );
                        })
                    }
                </dl>
            )
            : value;
        const isLastTouched = lastTouchedItemIndex === index;
        return (
            <Item
                disabled={ !isSortable }
                index={ index }
                isCursorActive={ isSortable }
                isDeleteable={ !isDisabled }
                isLastTouched={ isLastTouched }
                isListGroupSorting={ isSorting }
                key={ `${value}-${index}` } // eslint-disable-line react/no-array-index-key
                onDelete={ onDelete }
                onEdit={ onEdit }
                position={ index } // This duplicate of index is required because the HOC swallows the index property
                scrollTo={ isLastTouched && scrollTo }
                { ...restProps }
            >
                { content }
            </Item>
        );
    });

    return (
        <ListGroup className="sortable-array-field-list-group">
            { sortableItems }
        </ListGroup>
    );
});

const setObjectDefaults = (itemSchema) => {
    if (itemSchema.properties) {
        return mapValues(itemSchema.properties, ({ type }) => {
            return type === "boolean" ? false : undefined;
        });
    } else {
        return "";
    }
};

class SortableList extends Component {
    static propTypes = {
        components: PropTypes.objectOf(PropTypes.func),
        disabled: PropTypes.bool,
        id: PropTypes.string.isRequired,
        itemSchema: PropTypes.shape({
            minLength: PropTypes.number,
            properties: PropTypes.objectOf(PropTypes.any),
            type: PropTypes.string.isRequired
        }).isRequired,
        label: PropTypes.string.isRequired,
        onChange: PropTypes.func.isRequired,
        placeholder: PropTypes.string,
        uniqueItems: PropTypes.bool,
        values: PropTypes.arrayOf(PropTypes.any)
    };

    static defaultProps = {
        disabled: false,
        uniqueItems: false,
        values: []
    };

    state = { isSorting: false, lastTouchedItemIndex: -1 };

    componentDidMount () {
        this.setState({ inputValue: setObjectDefaults(this.props.itemSchema) }); // eslint-disable-line react/no-did-mount-set-state
    }

    isDuplicate = (newValue) => {
        const { itemSchema, uniqueItems, values } = this.props;

        if (uniqueItems) {
            if (itemSchema.type === "object") {
                return find(values, newValue);
            } else {
                return values.includes(newValue);
            }
        }

        return false;
    };

    handleAdd = (value) => {
        const { itemSchema, values, onChange } = this.props;

        if (this.isDuplicate(value)) {
            return false;
        }

        onChange([...values, value]);

        this.setState({ inputValue: setObjectDefaults(itemSchema), lastTouchedItemIndex: -1, scrollToList: false });

        return true;
    };

    handleCancel = () => {
        const inputValue = this.props.itemSchema.type === "object" ? {} : "";
        this.setState({ isEditing: false, lastTouchedItemIndex: -1, inputValue });
    };

    handleDelete = (index) => {
        const copy = [...this.props.values];
        copy.splice(index, 1);

        this.setState({ lastTouchedItemIndex: -1, scrollToList: false });
        this.props.onChange(copy);
    };

    handleEdit = (index) => {
        const inputValue = this.props.values[index];
        this.setState({ inputValue, isEditing: true, lastTouchedItemIndex: index, scrollToList: false });
    };

    handleEdited = (value) => {
        const { itemSchema, values, onChange } = this.props;

        if (this.isDuplicate(value)) {
            return false;
        }

        const copy = [...values];
        copy[this.state.lastTouchedItemIndex] = value;
        onChange(copy);

        const inputValue = itemSchema.type === "object" ? {} : "";
        this.setState({ isEditing: false, inputValue, scrollToList: true });
    };

    handleSortEnd = ({ oldIndex, newIndex }) => {
        const reorderedData = arrayMove(this.props.values, oldIndex, newIndex);
        this.props.onChange(reorderedData);
        this.setState({ isSorting: false, lastTouchedItemIndex: newIndex });
    };

    handleChangeValue = (inputValue) => {
        this.setState({ inputValue });
    };

    handleSortStart = () => {
        this.setState({ isSorting: true, lastTouchedItemIndex: -1, scrollToList: false });
    };

    render () {
        const { components, disabled, id, itemSchema, label, placeholder, values, ...restProps } = this.props;

        const list = isEmpty(values)
            ? undefined
            : (
                <Well
                    bsSize="small"
                    style={ { paddingBottom: 15 } }
                >
                    <SortableArray
                        components={ { Item: SortableItem, ...components } }
                        helperClass="dragged"
                        isDisabled={ disabled || this.state.isEditing }
                        isSorting={ this.state.isSorting }
                        items={ values }
                        itemSchema={ itemSchema }
                        lastTouchedItemIndex={ this.state.lastTouchedItemIndex }
                        lockAxis="y"
                        lockToContainerEdges
                        onDelete={ this.handleDelete }
                        onEdit={ this.handleEdit }
                        onSortEnd={ this.handleSortEnd }
                        onSortStart={ this.handleSortStart }
                        scrollTo={ this.state.scrollToList }
                        useDragHandle
                        { ...restProps }
                    />
                </Well>
            );

        const InputControl = itemSchema.type === "object" ? ObjectInputControl : StringInputControl;

        return (
            <div className="sortable-array-field">
                { list }
                <ScrollIntoViewIfNeeded active={ !!this.state.isEditing }>
                    <InputControl
                        disabled={ disabled }
                        id={ id }
                        isEditing={ this.state.isEditing }
                        itemSchema={ itemSchema }
                        label={ label }
                        onAdd={ this.handleAdd }
                        onCancel={ this.handleCancel }
                        onChange={ this.handleChangeValue }
                        onEdited={ this.handleEdited }
                        placeholder={ placeholder }
                        value={ this.state.inputValue }
                    />
                </ScrollIntoViewIfNeeded>
            </div>
        );
    }
}

export default SortableList;
