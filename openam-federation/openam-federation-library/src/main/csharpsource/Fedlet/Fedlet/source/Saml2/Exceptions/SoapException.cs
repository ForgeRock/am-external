/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

using System;
using System.Runtime.Serialization;

namespace Sun.Identity.Saml2.Exceptions
{
    /// <summary>
    /// Exception class specific for SOAP logic.
    /// </summary>
    [SerializableAttribute]
    public class SoapException : Exception, ISerializable
    {
        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        public SoapException()
            : base()
        {
        }

        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        /// <param name="message">Message associated with this exception.</param>
        public SoapException(string message)
            : base(message)
        {
        }

        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        /// <param name="message">Message associated with this exception.</param>
        /// <param name="inner">Inner exception associated with this exception.</param>
        public SoapException(string message, Exception inner)
            : base(message, inner)
        {
        }

        /// <summary>
        /// Initializes a new instance of the SoapException class.
        /// </summary>
        /// <param name="info">SerializationInfo used for base class support.</param>
        /// <param name="context">StreamingContext used for base class support.</param>
        protected SoapException(SerializationInfo info, StreamingContext context)
            : base(info, context)
        {
        }
    }
}
