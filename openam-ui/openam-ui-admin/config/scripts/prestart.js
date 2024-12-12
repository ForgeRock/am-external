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
 * Copyright 2019-2024 ForgeRock AS.
 */

require("dotenv").config();

const { forOwn, isEmpty, isFunction, reduce } = require("lodash");
const chalk = require("chalk");
const fs = require("fs");
const inquirer = require("inquirer");

const info = require("../support/log/colour/info");

const questions = [{
    "default": "http://am.example.com",
    message: `Enter the ${info("hostname")} of the target AM instance`,
    name: "AM_HOSTNAME"
}, {
    "default": 8080,
    message: `Enter the ${info("port")} of the target AM instance`,
    name: "AM_PORT"
}, {
    "default": "/openam",
    message: `Enter the ${info("path")} of the target AM instance`,
    name: "AM_PATH"
}, {
    message: `Enter the absolute ${info("path")} of the SSL private key`,
    name: "AM_SSL_KEY",
    when: (answers) => answers.AM_HOSTNAME && answers.AM_HOSTNAME.startsWith("https:")
}, {
    message: `Enter the absolute ${info("path")} of the SSL public certificate`,
    name: "AM_SSL_CERT",
    when: (answers) => answers.AM_SSL_KEY
}, {
    message: `Enter the absolute ${info("path")} of the CA certificate`,
    name: "AM_SSL_CA_CERT",
    when: (answers) => answers.AM_SSL_KEY
}];

console.log("Starting UI Development/Proxy Server...");

const questionsToAsk = [];
const knownAnswers = {};
questions.forEach((question) => {
    if (process.env[question.name] !== undefined) {
        knownAnswers[question.name] = process.env[question.name];
    } else if (isEmpty(knownAnswers) || !isFunction(question.when) || question.when(knownAnswers)) {
        questionsToAsk.push(question);
    }
});

const hasQuestions = questionsToAsk.length;
const message = questionsToAsk.length
    ? chalk.yellow("Required environment variables not found.")
    : chalk.green("Environment variables successfully loaded. See README.md for more information.");
console.log(message);

if (hasQuestions) {
    inquirer.prompt(questionsToAsk).then((answers) => {

        let envsToWrite = "";
        
        forOwn(answers, (answer, key) => {
          envsToWrite = `${envsToWrite}${key}=${answer}\n`;
          delete answers[key];
        });

        const missing = reduce(answers, (result, answer, key) => {
            return `${result}${key}=${answer}\n`;
        }, "");

        // Save environment variables
        fs.writeFileSync(".env", envsToWrite);

        if (missing) {
            const message = chalk.red("Set the following environment variable to continue");
            console.log(`${message}\n${missing}`);

            process.exit(1);
        }
    });
}
