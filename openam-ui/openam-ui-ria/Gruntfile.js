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
 * Copyright 2015-2017 ForgeRock AS.
 */

/* global module, require, process */

const _ = require("lodash");
const mavenSrcPath = "/src/main/js";
const mavenTestPath = "/src/test/js";

const mavenProjectSource = (projectDir) => [
    projectDir + mavenSrcPath,
    `${projectDir}/src/main/resources`
];

const mavenProjectTestSource = (projectDir) => [
    projectDir + mavenTestPath,
    `${projectDir}/src/test/resources`
];

const copyFromNodeModules = (files, destination) => files.map((file) => {
    const src = _.isArray(file) ? file[0] : file;
    return {
        dest: destination,
        expand: true,
        flatten: true,
        src: `node_modules/${src}`,
        rename: (dest, src) => {
            if (_.isArray(file)) {
                return dest + file[1];
            } else {
                return dest + src;
            }
        }
    };
});

module.exports = function (grunt) {
    /**
     * List of CSS files to be copied from node_modules to the css directory.
     * Two formats are possible:
     * 1) String entries will be copied directly. e.g. "bootstrap/bootstrap.css"
     * 2) Array entries will copy to a specified location. e.g. ["bootstrap/bootstrap.css", "bootstrap.min.css"]
     */
    const nodeModulesCSSFiles = [];
    /**
     * List of JavaScript files to be copied from node_modules to the libs directory.
     * Two formats are possible:
     * 1) String entries will be copied directly. e.g. "redux/redux.js"
     * 2) Array entries will copy to a specified location. e.g. ["redux/redux.js", "redux.min.js"]
     */
    const nodeModulesJSFiles = [
        ["react-draggable/dist/react-draggable.min.js", "react-draggable-2.2.6.min.js"],
        ["react-measure/dist/react-measure.min.js", "react-measure-1.4.7.min.js"],
        ["react-redux/dist/react-redux.min.js", "react-redux-5.0.3-min.js"],
        ["redux-actions/dist/redux-actions.min.js", "redux-actions-2.0.1-min.js"],
        ["redux/dist/redux.min.js", "redux-3.5.2-min.js"],
        ["react-jsonschema-form/dist/react-jsonschema-form.js", "react-jsonschema-form-0.49.0.js"],
        ["react-dnd/dist/ReactDnD.min.js", "ReactDnD-2.4.0-min.js"],
        ["react-dnd-html5-backend/dist/ReactDnDHTML5Backend.min.js", "ReactDnDHTML5Backend-2.4.1-min.js"],
        "get-node-dimensions/dist/get-node-dimensions.min.js",
        "resize-observer-polyfill/dist/ResizeObserver.js"
    ];
    const compositionDirectory = "target/XUI";
    const compiledDirectory = "target/compiled";
    const transpiledDirectory = "target/transpiled";
    const testClassesDirectory = "target/test-classes";
    const forgeRockCommonsDirectory = `${process.env.FORGEROCK_UI_SRC}/forgerock-ui-commons`;
    const forgeRockUiDirectory = `${process.env.FORGEROCK_UI_SRC}/forgerock-ui-user`;
    const targetVersion = grunt.option("target-version") || "dev";
    const buildCompositionDirs = _.flatten([
        "target/dependencies",
        // When building, dependencies are downloaded and expanded by Maven
        "target/dependencies-expanded/forgerock-ui-user",
        // This must come last so that it overwrites any conflicting files!
        mavenProjectSource(".")
    ]);
    const watchCompositionDirs = _.flatten([
        // When watching, we want to get the dependencies directly from the source
        mavenProjectSource(forgeRockCommonsDirectory),
        mavenProjectSource(forgeRockUiDirectory),
        // This must come last so that it overwrites any conflicting files!
        mavenProjectSource(".")
    ]);
    const testWatchDirs = _.flatten([
        mavenProjectTestSource(".")
    ]);
    const testInputDirs = _.flatten([
        mavenProjectTestSource(".")
    ]);
    const nonCompiledFiles = [
        "**/*.html",
        "**/*.ico",
        "**/*.json",
        "**/*.png",
        "**/*.eot",
        "**/*.svg",
        "**/*.woff",
        "**/*.woff2",
        "**/*.otf",
        "**/*.js.map",
        "css/bootstrap-3.3.5-custom.css",
        "themes/**/*.*"
    ];
    const serverDeployDirectory = `${process.env.OPENAM_HOME}/XUI`;

    grunt.initConfig({
        babel: {
            options: {
                env: {
                    development: {
                        sourceMaps: true
                    }
                },
                ignore: ["libs/"],
                presets: ["es2015", "react"],
                plugins: [
                    ["transform-es2015-classes", { "loose": true }],
                    "transform-object-rest-spread"
                ]
            },
            transpileJS: {
                files: [{
                    expand: true,
                    cwd: compositionDirectory,
                    src: ["**/*.js"],
                    dest: transpiledDirectory
                }]
            },
            transpileJSM: {
                files: [{
                    expand: true,
                    cwd: compositionDirectory,
                    src: ["**/*.jsm", "**/*.jsx"],
                    dest: transpiledDirectory,
                    rename: (dest, src) => `${dest}/${src.replace(".jsm", ".js").replace(".jsx", ".js")}`
                }],
                options: {
                    plugins: ["transform-es2015-modules-amd", "transform-object-rest-spread"]
                }
            }
        },
        copy: {
            /**
             * Copies JavaScript files defined within nodeModulesJSFiles to the libs directory.
             */
            nodeModulesJS: {
                files: copyFromNodeModules(nodeModulesJSFiles, `${compositionDirectory}/libs/`)
            },
            /**
             * Copies CSS files defined within nodeModulesJSFiles to the css directory.
             */
            nodeModulesCSS: {
                files: copyFromNodeModules(nodeModulesCSSFiles, `${compositionDirectory}/css/`)
            },
            /**
             * Copy all the sources and resources from this project and all dependencies into the composition directory.
             *
             * TODO: This copying shouldn't really be necessary, but is required because the dependencies are all over
             * the place. If we move to using npm for our dependencies, this can be greatly simplified.
             */
            compose: {
                files: buildCompositionDirs.map((dir) => {
                    return {
                        expand: true,
                        cwd: dir,
                        src: ["**"],
                        dest: compositionDirectory
                    };
                })
            },
            /**
             * Copy files that do not need to be compiled into the compiled directory.
             */
            compiled: {
                files: [{
                    expand: true,
                    cwd: compositionDirectory,
                    src: nonCompiledFiles.concat([
                        "!main.js", // Output by r.js
                        "!index.html" // Output by grunt-text-replace
                    ]),
                    dest: compiledDirectory
                }]
            },
            /**
             * Copy files that have been transpiled into the compiled directory.
             */
            transpiled: {
                files: [{
                    expand: true,
                    cwd: transpiledDirectory,
                    src: [
                        "**/*.js",
                        "!main.js" // Output by r.js
                    ],
                    dest: compiledDirectory
                }]
            }
        },
        eslint: {
            /**
             * Check the JavaScript source code for common mistakes and style issues.
             */
            lint: {
                src: [
                    `.${mavenSrcPath}/**/*.js`,
                    `.${mavenSrcPath}/**/*.jsm`,
                    `.${mavenSrcPath}/**/*.jsx`,
                    `.${mavenTestPath}/**/*.js`
                ],
                options: {
                    format: require.resolve("eslint-formatter-warning-summary")
                }
            }
        },
        karma: {
            options: {
                configFile: "karma.conf.js"
            },
            build: {
                singleRun: true,
                reporters: "progress"
            },
            dev: {
            }
        },
        less: {
            /**
             * Compile LESS source code into minified CSS files.
             */
            compile: {
                files: [{
                    src: `${compositionDirectory}/css/structure.less`,
                    dest: `${compiledDirectory}/css/structure.css`
                }, {
                    src: `${compositionDirectory}/css/theme.less`,
                    dest: `${compiledDirectory}/css/theme.css`
                }, {
                    src: `${compositionDirectory}/css/styles-admin.less`,
                    dest: `${compiledDirectory}/css/styles-admin.css`
                }],
                options: {
                    compress: true,
                    plugins: [
                        new (require("less-plugin-clean-css"))({})
                    ],
                    relativeUrls: true
                }
            }
        },
        replace: {
            /**
             * Include the version of AM in the index file.
             *
             * This is needed to force the browser to refetch JavaScript files when a new version of AM is deployed.
             */
            buildNumber: {
                src: `${compositionDirectory}/index.html`,
                dest: `${compiledDirectory}/index.html`,
                replacements: [{
                    from: "${version}",
                    to: targetVersion
                }]
            }
        },
        requirejs: {
            /**
             * Concatenate and uglify the JavaScript.
             */
            compile: {
                options: {
                    baseUrl: transpiledDirectory,
                    mainConfigFile: `${transpiledDirectory}/main.js`,
                    out: `${compiledDirectory}/main.js`,
                    include: ["main"],
                    preserveLicenseComments: false,
                    generateSourceMaps: true,
                    optimize: "uglify2",
                    // These files are excluded from optimization so that the UI can be customized without having to
                    // repackage it.
                    excludeShallow: [
                        "config/AppConfiguration",
                        "config/ThemeConfiguration"
                    ]
                }
            }
        },
        /**
         * Sync is used when watching to speed up the build.
         */
        sync: {
            /**
             * Copy all the sources and resources from this project and all dependencies into the composition directory.
             */
            compose: {
                files: watchCompositionDirs.map((dir) => {
                    return {
                        cwd: dir,
                        src: ["**"],
                        dest: compositionDirectory
                    };
                }),
                compareUsing: "md5"
            },
            /**
             * Copy files that do not need to be compiled into the compiled directory.
             *
             * Note that this also copies main.js because the requirejs step is not being performed when watching (it
             * is too slow).
             */
            compiled: {
                files: [{
                    cwd: compositionDirectory,
                    src: nonCompiledFiles.concat([
                        "!index.html" // Output by grunt-text-replace
                    ]),
                    dest: compiledDirectory
                }],
                compareUsing: "md5"
            },
            /**
             * Copy files that have been transpiled (with their source maps) into the compiled directory.
             */
            transpiled: {
                files: [{
                    cwd: transpiledDirectory,
                    src: [
                        "**/*.js",
                        "**/*.js.map"
                    ],
                    dest: compiledDirectory
                }],
                compareUsing: "md5"
            },
            /**
             * Copy the test source files into the test-classes target directory.
             */
            test: {
                files: testInputDirs.map((inputDirectory) => {
                    return {
                        cwd: inputDirectory,
                        src: ["**"],
                        dest: testClassesDirectory
                    };
                }),
                verbose: true,
                compareUsing: "md5" // Avoids spurious syncs of touched, but otherwise unchanged, files (e.g. CSS)
            },
            /**
             * Copy the compiled files to the server deploy directory.
             */
            server: {
                files: [{
                    cwd: compiledDirectory,
                    src: ["**"],
                    dest: serverDeployDirectory
                }],
                verbose: true,
                compareUsing: "md5" // Avoids spurious syncs of touched, but otherwise unchanged, files (e.g. CSS)
            }
        },
        watch: {
            /**
             * Redeploy whenever any source files change.
             */
            source: {
                files: watchCompositionDirs.concat(testWatchDirs).map((dir) => `${dir}/**`),
                tasks: ["deploy"]
            }
        }
    });

    grunt.loadNpmTasks("grunt-babel");
    grunt.loadNpmTasks("grunt-contrib-copy");
    grunt.loadNpmTasks("grunt-contrib-less");
    grunt.loadNpmTasks("grunt-contrib-requirejs");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-eslint");
    grunt.loadNpmTasks("grunt-karma");
    grunt.loadNpmTasks("grunt-newer");
    grunt.loadNpmTasks("grunt-sync");
    grunt.loadNpmTasks("grunt-text-replace");

    /**
     * Resync the compiled directory and deploy to the web server.
     */
    grunt.registerTask("deploy", [
        "sync:compose",
        "newer:babel",
        "less",
        "replace",
        "sync:compiled",
        "sync:transpiled",
        "sync:test",
        "sync:server"
    ]);

    /**
     * Rebuild the compiled directory. Maven then packs this directory into the final archive artefact.
     */
    grunt.registerTask("build", () => {
        const tasks = [
            "copy:nodeModulesCSS",
            "copy:nodeModulesJS",
            "copy:compose",
            "eslint",
            "babel",
            "requirejs",
            "less",
            "replace",
            "copy:compiled",
            "copy:transpiled"
        ];
        if (process.env.SKIP_TESTS !== "true") {
            tasks.push("karma:build");
        }
        grunt.task.run(tasks);
    });

    grunt.registerTask("dev", [
        "copy:nodeModulesCSS",
        "copy:nodeModulesJS",
        "copy:compose",
        "babel",
        "deploy",
        "watch"
    ]);
    grunt.registerTask("prod", ["build"]);

    grunt.registerTask("default", ["dev"]);
};
