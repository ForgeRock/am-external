/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

using System;
using System.Collections.Specialized;
using System.Globalization;
using System.Text;
using System.Xml;
using System.Xml.XPath;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;
using System.Collections.Generic;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// SAMLv2 AttributeQuery object constructed from either a response obtained
    /// from an Identity Provider for the hosted Service Provider or generated
    /// by this Service Provider to be sent to a desired Identity Provider.
    /// </summary>
    public class AttributeQueryRequest
    {
        #region Members
        /// <summary>
        /// Namespace Manager for this attribute query request.
        /// </summary>
        private XmlNamespaceManager nsMgr;

        /// <summary>
        /// XML representation of the attribute query request.
        /// </summary>
        private XmlDocument xml;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the AttributeQueryRequest class.
        /// </summary>
        /// <param name="identityProvider">
        /// IdentityProvider of the AttributeQueryRequest
        /// </param>
        /// <param name="serviceProvider">
        /// ServiceProvider of the AttributeQueryRequest
        /// </param>
        /// <param name="parameters">
        /// NameValueCollection of varying parameters for use in the 
        /// construction of the AttributeQueryRequest.
        /// </param>
        /// <param name="attributes">
        /// List of SamlAttribute to query
        /// </param>
        public AttributeQueryRequest(IdentityProvider identityProvider,
            ServiceProvider serviceProvider, NameValueCollection parameters, List<SamlAttribute> attributes)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;
                this.X509SubjectName = false;

                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");

                string subjectNameId = null;

                if (parameters != null)
                {
                    subjectNameId = parameters[Saml2Constants.SubjectNameId];
                    this.X509SubjectName = Saml2Utils.GetBoolean(parameters[Saml2Constants.X509SubjectName]);
                }

                if (string.IsNullOrWhiteSpace(subjectNameId))
                {
                    throw new Saml2Exception(Resources.AttributeQueryRequestSubjectNameIdNotDefined);
                }
                else if (serviceProvider == null)
                {
                    throw new Saml2Exception(Resources.AttributeQueryRequestServiceProviderIsNull);
                }
                else if (identityProvider == null)
                {
                    throw new Saml2Exception(Resources.AttributeQueryRequestIdentityProviderIsNull);
                }
                else if (attributes == null || attributes.Count == 0)
                {
                    throw new Saml2Exception(Resources.AttributeQueryRequestIsEmpty);
                }

                StringBuilder rawXml = new StringBuilder();
                rawXml.Append("<samlp:AttributeQuery xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"");
                rawXml.Append(" ID=\"" + Saml2Utils.GenerateId() + "\"");
                rawXml.Append(" Version=\"2.0\"");
                rawXml.Append(" IssueInstant=\"" + Saml2Utils.GenerateIssueInstant() + "\">");
                rawXml.Append("<saml:Issuer>" + serviceProvider.EntityId + "</saml:Issuer>");
                rawXml.Append("<saml:Subject>");
                rawXml.Append("<saml:NameID");

                if (this.X509SubjectName)
                {
                    rawXml.Append(" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\"");
                }
                else
                {
                    rawXml.Append(" Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"");
                }

                rawXml.Append(" SPNameQualifier=\"" + serviceProvider.EntityId + "\"");
                rawXml.Append(" NameQualifier=\"" + identityProvider.EntityId + "\">" + subjectNameId + "</saml:NameID>");
                rawXml.Append("</saml:Subject>");

                foreach (SamlAttribute attr in attributes)
                {
                    if (attr != null)
                    {
                        rawXml.Append(attr.ToString());
                    }
                }

                rawXml.Append("</samlp:AttributeQuery>");

                this.xml.LoadXml(rawXml.ToString());
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.AttributeQueryRequestNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.AttributeQueryRequestXmlException, xe);
            }
        }
        #endregion

        #region Properties

        /// <summary>
        /// Gets the Subject type of the AttributeQuery request.
        /// </summary>
        public bool X509SubjectName { get; private set; }

        #endregion

        #region Methods
        /// <summary>
        /// Gets the ID attribute value of the AttributeQuery request.
        /// </summary>
        public string Id
        {
            get
            {
                string xpath = "/samlp:AttributeQuery";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                if (node != null)
                {
                    return node.Attributes["ID"].Value.Trim();
                }
                return null;

            }
        }

        /// <summary>
        /// Gets the XML representation of the received logout request.
        /// </summary>
        public IXPathNavigable XmlDom
        {
            get
            {
                return this.xml;
            }
        }

        #endregion

    }
}
