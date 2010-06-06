/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.codec.add;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;


/**
 * An AddResponse Message. Its syntax is : 
 * 
 * AddResponse ::= [APPLICATION 9] LDAPResult
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddResponseCodec extends LdapResponseCodec
{
    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new AddResponse object.
     */
    public AddResponseCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public MessageTypeEnum getMessageType()
    {
        return MessageTypeEnum.ADD_RESPONSE;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "ADD_RESPONSE";
    }


    /**
     * Compute the AddResponse length 
     * 
     * AddResponse : 
     * 
     * 0x69 L1
     *  |
     *  +--> LdapResult
     * 
     * L1 = Length(LdapResult)
     * 
     * Length(AddResponse) = Length(0x69) + Length(L1) + L1
     */
    protected int computeLengthProtocolOp()
    {
        int ldapResultLength = super.computeLdapResultLength();

        return 1 + TLV.getNbBytes( ldapResultLength ) + ldapResultLength;
    }


    /**
     * Encode the AddResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The tag
            buffer.put( LdapConstants.ADD_RESPONSE_TAG );
            buffer.put( TLV.getBytes( getLdapResponseLength() ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The ldapResult
        super.encode( buffer );
    }


    /**
     * Get a String representation of an AddResponse
     * 
     * @return An AddResponse String
     */
    public String toString()
    {
        return toString( "    Add Response" );
    }
}
