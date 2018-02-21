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
 * Copyright 2016-2017 ForgeRock AS.
 */

/* global module, require, process */

module.exports = function (grunt) {
    const serverDeployDirectory = `${process.env.OPENAM_HOME}/api`;
    const compiledDirectory = "target/www/";
    grunt.initConfig({
        copy: {
            swagger: {
                files: [{
                    expand: true,
                    cwd: "node_modules/swagger-ui/dist/",
                    src: ["swagger-ui.js", "css/*", "fonts/*", "images/*", "lang/*", "lib/*"],
                    dest: compiledDirectory
                }],
                options: {
                    noProcess: ["**/*.{png,gif,jpg,ico,svg,ttf,eot,woff}"]
                }
            },
            swaggerThemes: {
                files: [{
                    expand: true,
                    cwd: "node_modules/swagger-ui-themes/themes/",
                    src: ["theme-flattop.css"],
                    dest: compiledDirectory
                }]
            },
            resources: {
                files: [{
                    expand: true,
                    cwd: "src/main/resources/",
                    src: ["**"],
                    dest: compiledDirectory
                }]
            },
            server: {
                files: [{
                    expand: true,
                    cwd: compiledDirectory,
                    src: ["**"],
                    dest: serverDeployDirectory
                }]
            }
        }
    });

    grunt.loadNpmTasks("grunt-contrib-copy");

    grunt.registerTask("build:dev", ["build:prod", "copy:server"]);
    grunt.registerTask("build:prod", ["copy:swagger", "copy:swaggerThemes", "copy:resources"]);

    grunt.registerTask("default", ["build:dev"]);
};
