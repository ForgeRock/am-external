/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
