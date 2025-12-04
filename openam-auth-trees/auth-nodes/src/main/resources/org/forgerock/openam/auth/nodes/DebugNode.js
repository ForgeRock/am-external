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
 * Copyright 2022-2025 Ping Identity Corporation.
 */
// Ensure the frDebugNode object does not exist before we define the class
if (!window.frDebugNode) {
  /**
   * FrDebugNode Class for displaying tree debug logs in a popup window.
   * @class
   * @constructor
   * @public
   */
  class FrDebugNode {
    constructor() {
      /**
       * The name of the current login tree.
       * @type {String}
       * @public
       */
      this.tree = null;
      /**
       * The name of the current realm.
       * @type {String}
       * @public
       */
      this.realmName = null;
      /**
       * The popup window created to display logs in.
       * @type {Object}
       * @public
       */
      this.popup = null;
      /**
       * The title that will be given to the popup window.
       * @type {String}
       * @public
       */
      this.popupTitle = 'Tree Debugger';
      /**
       * Holds locale translated strings used throughout.
       * @type {String}
       * @public
       */
       this.tranlations = {
         'popup.title': 'Tree Debugger',
         'logs.initialStep': 'Initial Step',
         'logs.step': 'Step',
         'logs.realm': 'Realm'
       };
      /**
       * The array of logs being displayed. Each log contains the name
       * of the node, the date/time of the log and the log information (tree
       * state, transactionId, universalId).
       * @type {Array}
       * @public
       */
      this.logs = [];
      /**
       * Used to track the current node number and is displayed to the user
       * as the current step number.
       * @type {Number}
       * @public
       */
      this.nodeIndex = 0;
      /**
       * Some default styles that get loaded in to the popup.
       * @type {Number}
       * @public
       */
      this.css = `
        body { padding: 10px; }
        h1 { font-weight: 300; }
        h2 { color: rgb(42, 47, 50); margin: 0 0 40px 0; }
        h4 { margin-top: 0; margin-bottom: 0; display: inline-block; }
        p.fr-date { margin-bottom: 0; display: inline-block; }
        div.fr-log { margin-bottom: 40px; }
        pre {
          margin-top: 8px;
          background: #eee;
          line-height: 1.5;
          padding: 20px;
          border-radius: 4px;
          border: 1px solid #ddd;
          white-space: pre-wrap;
        }
      `;
    }

    /**
      * Creates the popup window that will display the logs, sets the window
      * title, some default titles and adds a placeholder element for the realm
      * name which will get set in the future.
      *
      */
    createPopup() {
      // Only create popup if it doesn't already exist
      if (this.popup === null) {
        this.popup = open('', 'debuggerWindow', `scrollbars=yes, width=${screen.availWidth / 2}, height=${screen.availHeight}`);

        if (!this.popup.document.querySelector('#fr-tree')) {
          this.popup.document.write(`
            <h2 id='fr-tree'></h2>
          `);
        }

        if (!this.popup.document.querySelector('#fr-realm')) {
          this.popup.document.write(`
            <h2 id='fr-realm'></h2>
          `);
        }


        const nativeStyles = document.querySelectorAll('link[rel=stylesheet]');
        if (nativeStyles.length) {
          for (let i = 0; i < nativeStyles.length; i++) {
            this.popup.document.head.append(nativeStyles[i].cloneNode(true));
          }
        }
        const localStyles = this.popup.document.createElement('style');
        localStyles.type = 'text/css';
        localStyles.appendChild(this.popup.document.createTextNode(this.css));
        this.popup.document.head.appendChild(localStyles);

        this.setPopupTitle(this.popupTitle);
      }
    }

    /**
      * Sets the locale translations used in the logs
      *
      * @param {String} tranlationString String of translations to be converted to json object
      */
    setTranslations(translationsString) {
      this.translations = JSON.parse(translationsString);
    };

    /**
      * Sets the html title of the popup window
      *
      * @param {String} title The title of the popup
      */
    setPopupTitle(title) {
      this.popup.document.title = title;
    }

    /**
      * Sets the current tree name in the popup window.
      *
      * @param {String} tree The name of the tree
      */
    setTree(tree) {
      this.tree = tree;
      this.popup.document.querySelector('#fr-tree').innerText = this.tree;
    }

    /**
      * Sets the name of the current realm in the popup window.
      *
      * @param {String} realm The name of the realm
      */
    setRealm(realm) {
      this.realm = realm;
      this.popup.document.querySelector('#fr-realm').innerText = `${this.translations['logs.realm']}: ${this.realm}`;
    }

    /**
      * Appends a new log to the list and displays it in the popup window. A log
      * contains the title of the current node (or index if it is not available),
      * a date/time of the log and log information such as the state of the tree.
      *
      * @param {String} realm The name of the realm
      */
    appendLog(log) {
      let date = new Date();
      // get a pretty printed version of the log
      const prettyLog = JSON.stringify(JSON.parse(log), null, 4);
      date = date.toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric' });
      const node = this.nodeIndex ? `${this.translations['logs.step']} ${this.nodeIndex}` : `${this.translations['logs.initialStep']}`;
      this.popup.document.write(`
        <div class='fr-log'>
          <div class='fr-log-title'>
            <h4>${node}</h4>
            <p class='fr-date text-primary'> - ${date}</p>
          </div>
          <pre>${prettyLog}</pre>
        </div>
      `);
      this.nodeIndex += 1;
    }

  /**
    * Every debug node renders a new login page which allows it to add new logs
    * to the popup window using javascript. Other than adding logs the page is
    * not very useful for the customer so this will auto submit the page to move
    * them along to the next node in the tree.
    */
    moveToNextNode() {
      document.querySelector('input[type=\"submit\"]').click();
    }
  }
  window.frDebugNode = new FrDebugNode();
}
