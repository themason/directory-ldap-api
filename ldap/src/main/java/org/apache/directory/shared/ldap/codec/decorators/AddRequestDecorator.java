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
package org.apache.directory.shared.ldap.codec.decorators;


import java.util.List;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.AbandonListener;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.AddRequestImpl;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.message.ResultResponse;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A decorator for the AddRequest message
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddRequestDecorator extends MessageDecorator implements AddRequest
{
    /** The add request length */
    private int addRequestLength;

    /** The Entry length */
    private int entryLength;

    /** The list of all attributes length */
    private List<Integer> attributesLength;

    /** The list of all vals length */
    private List<Integer> valuesLength;


    /**
     * Makes a AddRequest encodable.
     *
     * @param decoratedMessage the decorated AddRequest
     */
    public AddRequestDecorator( AddRequest decoratedMessage )
    {
        super( decoratedMessage );
    }


    /**
     * @return The decorated AddRequest
     */
    public AddRequest getAddRequest()
    {
        return ( AddRequest ) getMessage();
    }


    /**
     * Stores the encoded length for the AddRequest
     * @param addRequestLength The encoded length
     */
    public void setAddRequestLength( int addRequestLength )
    {
        this.addRequestLength = addRequestLength;
    }


    /**
     * @return The encoded AddRequest's length
     */
    public int getAddRequestLength()
    {
        return addRequestLength;
    }


    /**
     * Stores the encoded length for the Entry
     * @param entryLength The encoded length
     */
    public void setEntryLength( int entryLength )
    {
        this.entryLength = entryLength;
    }


    /**
     * @return The encoded Entry's length
     */
    public int getEntryLength()
    {
        return entryLength;
    }


    /**
     * Stores the encoded length for the attributes
     * @param attributesLength The encoded length
     */
    public void setAttributesLength( List<Integer> attributesLength )
    {
        this.attributesLength = attributesLength;
    }


    /**
     * @return The encoded values length
     */
    public List<Integer> getAttributesLength()
    {
        return attributesLength;
    }


    /**
     * Stores the encoded length for the values
     * @param valuesLength The encoded length
     */
    public void setValuesLength( List<Integer> valuesLength )
    {
        this.valuesLength = valuesLength;
    }


    /**
     * @return The encoded values length
     */
    public List<Integer> getValuesLength()
    {
        return valuesLength;
    }


    //-------------------------------------------------------------------------
    // The AddRequest methods
    //-------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public MessageTypeEnum getResponseType()
    {
        return ((AddRequestImpl)decoratedMessage).getResponseType();
    }


    /**
     * {@inheritDoc}
     */
    public ResultResponse getResultResponse()
    {
        return ((AddRequestImpl)decoratedMessage).getResultResponse();
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasResponse()
    {
        return ((AddRequestImpl)decoratedMessage).hasResponse();
    }


    /**
     * {@inheritDoc}
     */
    public void abandon()
    {
        ((AddRequestImpl)decoratedMessage).abandon();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAbandoned()
    {
        return ((AddRequestImpl)decoratedMessage).isAbandoned();
    }


    /**
     * {@inheritDoc}
     */
    public void addAbandonListener( AbandonListener listener )
    {
        ((AddRequestImpl)decoratedMessage).addAbandonListener( listener );
    }


    /**
     * {@inheritDoc}
     */
    public Dn getEntryDn()
    {
        return ((AddRequestImpl)decoratedMessage).getEntryDn();
    }


    /**
     * {@inheritDoc}
     */
    public void setEntryDn( Dn entry )
    {
        ((AddRequestImpl)decoratedMessage).setEntryDn( entry );
    }


    /**
     * {@inheritDoc}
     */
    public Entry getEntry()
    {
        return ((AddRequestImpl)decoratedMessage).getEntry();
    }


    /**
     * {@inheritDoc}
     */
    public void setEntry( Entry entry )
    {
        ((AddRequestImpl)decoratedMessage).setEntry( entry );
    }


    /**
     * {@inheritDoc}
     */
    public void addAttributeType( String type ) throws LdapException
    {
        ((AddRequestImpl)decoratedMessage).addAttributeType( type );
    }


    /**
     * {@inheritDoc}
     */
    public String getCurrentAttributeType()
    {
        return ((AddRequestImpl)decoratedMessage).getCurrentAttributeType();
    }


    /**
     * {@inheritDoc}
     */
    public void addAttributeValue( String value )
    {
        ((AddRequestImpl)decoratedMessage).addAttributeValue( value );
    }


    /**
     * {@inheritDoc}
     */
    public void addAttributeValue( Value<?> value )
    {
        ((AddRequestImpl)decoratedMessage).addAttributeValue( value );
    }


    /**
     * {@inheritDoc}
     */
    public void addAttributeValue( byte[] value )
    {
        ((AddRequestImpl)decoratedMessage).addAttributeValue( value );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return ((AddRequestImpl)decoratedMessage).toString();
    }
}
