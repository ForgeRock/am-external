/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "qrcode-generator"
], ($, QRCodeGenerator) => {
    var obj = {},
        getCode = function (options) {
            var qr = new QRCodeGenerator(options.version || 4, options.code || "M");
            qr.addData(options.text);
            qr.make();

            //3 is the size of the painted squares, 8 is the white border around the edge
            return qr.createImgTag(3, 8);
        };

    /**
     * Creates QRCode and places it on in the target id.
     *
     * This method is called by <strong>org.forgerock.openam.utils.qr</strong> via the
     * ScriptTextOutputCallback in the RestLoginView.
     *
     * @param  {Object} options takes the 4 params below:
     * @param  {String} version - used to generate QR code.
     * @param  {String} code - used to generate QR code.
     * @param  {String} text - used to generate QR code.
     * @param  {String} id - used to select target.
     */
    obj.createCode = function (options) {
        const code = getCode(options);
        const element = $("<div class='text-center'/>");
        element.append(code);
        const container = $(`#${options.id}`);
        container.append(element);
        container.append(
            `<div class="form-group">
                <a href="${options.text}" class="btn btn-lg btn-block btn-uppercase btn-default"
                >${$.t("templates.user.LoginTemplate.onMobileDevice")}</a>
            </div>`);
    };

    return obj;
});
