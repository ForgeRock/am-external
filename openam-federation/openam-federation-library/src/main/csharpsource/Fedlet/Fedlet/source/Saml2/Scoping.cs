/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

using System;
using System.Collections;
using System.Text;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing the Scoping.
    /// </summary>
    public class Scoping
    {
        #region Members
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the Scoping class.
        /// </summary>
        public Scoping()
        {
            this.ProxyCount = 0;
            this.IDPEntry = new ArrayList();
        }
        #endregion

        #region Properties
        /// <summary>
        /// Gets or sets the ProxyCount.
        /// </summary>
        public int ProxyCount { get; set; }

        /// <summary>
        /// Gets the IDPEntry.
        /// </summary>
        public ArrayList IDPEntry { get; private set; }

        #endregion

        #region Methods

        /// <summary>
        /// Generates the XML string of the Scoping using
        /// the ProxyCount and IDPEntry information
        /// information.
        /// </summary>
        /// <returns>Returns the Scoping XML as a string.</returns>
        public string GenerateXmlString()
        {
            StringBuilder rawXml = new StringBuilder();

            rawXml.Append("<samlp:Scoping xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ProxyCount=\"");
            rawXml.Append(this.ProxyCount);
            rawXml.Append("\">");

            if (this.IDPEntry != null && this.IDPEntry.Count > 0)
            {
                rawXml.Append("<samlp:IDPList xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">");
                foreach (string value in this.IDPEntry)
                {
                    rawXml.Append("<samlp:IDPEntry xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ProviderID=\"");
                    rawXml.Append(value);
                    rawXml.Append("\"/>");
                }
                rawXml.Append("</samlp:IDPList>");
            }

            rawXml.Append("</samlp:Scoping>");

            return rawXml.ToString();
        }

        /// <summary>
        /// Sets the IDPEntry list.
        /// </summary>
        /// <param name="list">The list to become the IDPEntry.</param>
        public void SetIDPEntry(ArrayList list)
        {
            if (list == null)
            {
                this.IDPEntry = new ArrayList();
            }
            else
            {
                this.IDPEntry = list;
            }
        }

        #endregion
    }
}
