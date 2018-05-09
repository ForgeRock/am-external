/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
!function ($) {

    /**
    This plugin is for bootstrap and extends the existing plugin, popover. It was created to add
    functionality which popover doesn't provide, specifically that, when the user clicks the
    popover opens and when the user clicks it closes, UNLESS the user clicked on the open popover's text.
    */
    "use strict";

    var Popoverclickaway = function (element, options) {
        this.initialize("popoverclickaway", element, options);
    };

    Popoverclickaway.prototype = $.extend({}, $.fn.popover.Constructor.prototype, {
        constructor: Popoverclickaway,
        initialize: function (type, element, options) {
            this.init(type, element, options);
            this.$element.on("click", this.options.selector, $.proxy(this.clickHandler, this));
        },
        addClickListener : function (handler) {
            $("body").on("click", handler);
        },
        removeClickListener : function (handler) {
            $("body").off("click", handler);
        },
        clickHandler : function (event) {
            if (event) {
                event.preventDefault();
            }
            this.triggerEvent = event;
            if (this.isVisible()) {
                this.hide();
                this.removeClickListener(this.clickawayHandler.bind(this));
            } else {
                this.show();
                this.addClickListener(this.clickawayHandler.bind(this));
            }
        },
        isVisible : function () {
            return this.tip().hasClass("in");
        },
        hasClickedOnOpenPopover : function (event) {
            return this.tip().has(event.target).length > 0;
        },
        clickawayHandler : function (event) {
            /** the click event can come from the initial click which opened the popover. we need to check that
                it's not the same event, otherwise we are opening the popover and closing it immeadiately.
                we also check that we haven't clicked on the popover text itself. */
            if (this.triggerEvent.originalEvent !== event.originalEvent && !this.hasClickedOnOpenPopover(event)) {
                this.hide();
                this.removeClickListener(this.clickawayHandler.bind(this));
            }
        }
    });

    $.fn.popoverclickaway = function (option) {
        return this.each(function () {
            var $this = $(this), data = $this.data("popoverclickaway"), options = typeof option == "object" && option;
            if (!data) {
                $this.data("popoverclickaway", (data = new Popoverclickaway(this, options)));
            }
            if (typeof option == "string") {
                data[option]();
            }
        });
    };

    $.fn.popoverclickaway.defaults = $.extend({}, $.fn.popover.defaults, {
        trigger: "manual"
    });

    $.fn.popoverclickaway.Constructor = Popoverclickaway;

}(window.jQuery);
