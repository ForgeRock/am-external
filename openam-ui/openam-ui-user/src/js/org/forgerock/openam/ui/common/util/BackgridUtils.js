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
 * Copyright 2015-2019 ForgeRock AS.
 */

import "org/forgerock/commons/ui/common/backgrid/extension/ThemeableServerSideFilter";

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";
import moment from "moment";

import Backgrid from "org/forgerock/commons/ui/common/backgrid/Backgrid";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";

/**
 * @exports org/forgerock/openam/ui/common/util/BackgridUtils
 */
const BackgridUtils = {};

/**
 * Datetime Ago Cell Renderer
 * <p>
 * Displays human friendly date time text (e.g. 4 "hours ago") with a tooltip of the exact time
 */
BackgridUtils.DatetimeAgoCell = Backgrid.Cell.extend({
    className: "date-time-ago-cell",
    formatter: {
        fromRaw (rawData) {
            return moment(rawData).fromNow();
        }
    },
    render () {
        BackgridUtils.DatetimeAgoCell.__super__.render.apply(this);
        this.$el.attr("title", moment(this.model.get(this.column.get("name"))).format("Do MMMM YYYY, h:mm:ssa"));
        return this;
    }
});

/**
 * Array Cell
 * <p>
 * Displays cell content as an unordered list. Used for cells which values are arrays
 */
BackgridUtils.ArrayCell = Backgrid.Cell.extend({
    className: "array-formatter-cell",

    buildHtml (arrayVal) {
        let result = "<ul>";

        let i = 0;

        for (; i < arrayVal.length; i++) {
            if (_.isString(arrayVal[i])) {
                result += `<li>${arrayVal[i]}</li>`;
            } else {
                result += `<li>${JSON.stringify(arrayVal[i])}</li>`;
            }
        }
        result += "</ul>";

        return result;
    },

    render () {
        this.$el.empty();

        const arrayVal = this.model.get(this.column.attributes.name);
        this.$el.append(this.buildHtml(arrayVal));

        this.delegateEvents();
        return this;
    }
});

/**
 * Object Cell
 * <p>
 * Displays cell content as a definition list. Used for cells which values are objects
 */
BackgridUtils.ObjectCell = Backgrid.Cell.extend({
    className: "object-formatter-cell",

    render () {
        this.$el.empty();

        const object = this.model.get(this.column.attributes.name);

        let result = '<dl class="dl-horizontal">';

        let prop;

        for (prop in object) {
            if (_.isString(object[prop])) {
                result += `<dt>${prop}</dt><dd>${object[prop]}</dd>`;
            } else {
                result += `<dt>${prop}</dt><dd>${JSON.stringify(object[prop])}</dd>`;
            }
        }
        result += "</dl>";

        this.$el.append(result);

        this.delegateEvents();
        return this;
    }
});

BackgridUtils.UniversalIdToUsername = Backgrid.Cell.extend({
    formatter: {
        fromRaw (rawData) {
            return rawData.substring(3, rawData.indexOf(",ou=user"));
        }
    },
    render () {
        BackgridUtils.UniversalIdToUsername.__super__.render.apply(this);
        this.$el.attr("title", this.model.get(this.column.get("name")));
        return this;
    }
});

/**
 * Clickable Row
 * <p>
 * You must extend this row and specify a "callback" attribute e.g.
 * <p>
 * MyRow = BackgridUtils.ClickableRow.extend({
 *     callback: myCallback
 * });
 */
BackgridUtils.ClickableRow = Backgrid.Row.extend({
    events: {
        "click": "onClick",
        "keyup": "onKeyup"
    },

    onKeyup (e) {
        if (e.keyCode === 13 && this.callback) {
            this.callback(e);
        }
    },

    onClick (e) {
        if (this.callback) {
            this.callback(e);
        }
    }
});

/**
 * Handlebars Template Cell Renderer
 * <p>
 * You must extend this renderer and specify a "template" attribute e.g.
 * <p>
 * MyCell = backgridUtils.TemplateCell.extend({
 *     template: "templates/MyTemplate.html"
 * });
 */
BackgridUtils.TemplateCell = Backgrid.Cell.extend({
    className: "template-cell",
    render () {
        const content = this.template(this.model);
        this.$el.html(content);
        if (this.rendered) {
            this.rendered();
        }

        this.delegateEvents();

        return this;
    }
});

BackgridUtils.ClassHeaderCell = Backgrid.HeaderCell.extend({
    className: "",
    render () {
        BackgridUtils.ClassHeaderCell.__super__.render.apply(this);
        this.delegateEvents();
        return this;
    }
});

BackgridUtils.UriExtCell = Backgrid.UriCell.extend({
    events: {
        "click": "gotoUrl"
    },
    render () {
        this.$el.empty();
        const rawValue = this.model.get(this.column.get("name"));

        const formattedValue = this.formatter.fromRaw(rawValue, this.model);

        const href = _.isFunction(this.column.get("href"))
            ? this.column.get("href")(rawValue, formattedValue, this.model) : this.column.get("href");

        this.$el.append($("<a>", {
            href: href || rawValue,
            title: this.title || formattedValue
        }).text(formattedValue));

        if (href) {
            this.$el.data("href", href);
            this.$el.prop("title", this.title || formattedValue);
        }

        this.delegateEvents();
        return this;
    },

    gotoUrl (e) {
        e.preventDefault();
        const href = $(e.currentTarget).data("href");
        Router.navigate(href, { trigger: true });
    }

});

BackgridUtils.FilterHeaderCell = Backgrid.HeaderCell.extend({
    className: "filter-header-cell enable-pointer",
    render () {
        const filter = new Backgrid.Extension.ThemeableServerSideFilter({
            name: this.column.get("name"),
            placeholder: $.t("common.form.filter"),
            collection: this.collection
        });

        if (this.addClassName) {
            this.$el.addClass(this.addClassName);
        }

        this.collection.state.filters = this.collection.state.filters ? this.collection.state.filters : [];
        this.collection.state.filters.push(filter);
        BackgridUtils.FilterHeaderCell.__super__.render.apply(this);
        this.$el.prepend(filter.render().el);
        return this;
    }
});

BackgridUtils.queryFilter = function (data) {
    if (data === undefined) { data = {}; }

    let params = [];

    const additionalFilters = data._queryFilter || [];

    const getFilter = (function () {
        return data && data.filterName && data.filterName === "eq"
            ? function (filterName, filterQuery) {
                // Policies endpoints do not support 'co', so we emulate it using 'eq' and wildcards
                return `${filterName}+eq+${encodeURIComponent(`"*${filterQuery}*"`)}`;
            }
            : function (filterName, filterQuery) {
                return `${filterName}+co+${encodeURIComponent(`"${filterQuery}"`)}`;
            };
    }());

    _.each(this.state.filters, (filter) => {
        if (filter.query() !== "") {
            params.push(getFilter(filter.name, filter.query()));
        }
    });
    params = params.concat(additionalFilters);

    return params.length === 0 ? true : params.join("+AND+");
};

BackgridUtils.parseState = function (resp) {
    if (!this.state.totalRecords) {
        this.state.totalRecords = resp.remainingPagedResults + resp.resultCount;
    }
    if (!this.state.totalPages) {
        this.state.totalPages = Math.ceil(this.state.totalRecords / this.state.pageSize);
    }
    return this.state;
};

BackgridUtils.pagedResultsOffset = function () {
    return (this.state.currentPage - 1) * this.state.pageSize;
};

BackgridUtils.sortKeys = function () {
    return this.state.order === 1 ? `-${this.state.sortKey}` : this.state.sortKey;
};

BackgridUtils.sync = function (method, model, options) {
    const params = [];

    const includeList = ["_pageSize", "_pagedResultsOffset", "_sortKeys"];

    // TODO: UMA: Workaround as resource set end point requires a _queryId=* to indicate a blank query,
    // while the historyAudit end point requires a _queryFilter=true to indicate a blank query.
    if (options.data._queryId === "*" && options.data._queryFilter === true) {
        includeList.push("_queryId");
    } else {
        includeList.push("_queryFilter");
    }

    _.forIn(options.data, (val, key) => {
        if (_.includes(includeList, key)) {
            params.push(`${key}=${val}`);
        }
    });

    options.data = params.join("&");
    options.processData = false;

    if (!options.beforeSend) {
        options.beforeSend = function (xhr) {
            xhr.setRequestHeader("Accept-API-Version", "protocol=1.0,resource=1.0");
        };
    }

    options.error = function (response) {
        Messages.addMessage({
            type: Messages.TYPE_DANGER,
            response
        });
    };

    return Backbone.sync(method, model, options);
};

BackgridUtils.parseRecords = function (data) {
    return data.result;
};

BackgridUtils.getQueryParams = function (data) {
    data = data || {};

    return {
        _sortKeys: this.sortKeys,
        _queryFilter () {
            return BackgridUtils.queryFilter.call(this, data);
        },
        pageSize: "_pageSize",
        _pagedResultsOffset: this.pagedResultsOffset
    };
};

// FIXME: Workaround to fix "Double sort indicators" issue
// @see https://github.com/wyuenho/backgrid/issues/453
BackgridUtils.doubleSortFix = function (model) {
    // No ids so identify model with CID
    const cid = model.cid;

    const filtered = model.collection.filter((model) => {
        return model.cid !== cid;
    });

    _.each(filtered, (model) => {
        model.set("direction", null);
    });
};

BackgridUtils.getState = function (data) {
    const state = {
        pageSize: 20,
        sortKey: "name"
    };

    if (data && typeof data === "object") {
        _.extend(state, data);
    }
    return state;
};

export default BackgridUtils;
