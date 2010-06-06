/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.util.UTFUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A client side entry attribute. The client is not aware of the schema,
 * so we can't tell if the stored value will be String or Binary. We will
 * default to Binary.<p>
 * To define the kind of data stored, the client must set the isHR flag.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultEntryAttribute implements EntryAttribute
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultEntryAttribute.class );

    /** The associated AttributeType */
    private AttributeType attributeType;
    
    /** The set of contained values */
    private Set<Value<?>> values = new LinkedHashSet<Value<?>>();
    
    /** The User provided ID */
    private String upId;

    /** The normalized ID (will be the OID if we have a AttributeType) */
    private String id;

    /** Tells if the attribute is Human Readable or not. When not set, 
     * this flag is null. */
    private Boolean isHR;
    
    /** The computed hashcode. We don't want to compute it each time the hashcode() method is called */
    private volatile int h;
    
    //-------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------
    private Value<String> createStringValue( AttributeType attributeType, String value )
    {
        Value<String> stringValue = null;
        
        if ( attributeType != null )
        {
            stringValue = new StringValue( attributeType, value );
            
            try
            {
                stringValue.normalize();
            }
            catch( LdapException ne )
            {
                // The value can't be normalized : we don't add it.
                LOG.error( I18n.err( I18n.ERR_04449, value ) );
                return null;
            }
    
            if ( !stringValue.isValid() )
            {
                // The value is not valid : we don't add it.
                LOG.error( I18n.err( I18n.ERR_04450, value ) );
                return null;
            }
        }
        else
        {
            stringValue = new StringValue( value );
        }
        
        return stringValue;
    }


    private Value<byte[]> createBinaryValue( AttributeType attributeType, byte[] value )
    {
        Value<byte[]> binaryValue = null;
        
        if ( attributeType != null )
        {
            binaryValue = new BinaryValue( attributeType, value );
            
            try
            {
                binaryValue.normalize();
            }
            catch( LdapException ne )
            {
                // The value can't be normalized : we don't add it.
                LOG.error( I18n.err( I18n.ERR_04449, value ) );
                return null;
            }
            
            if ( !binaryValue.isValid() )
            {
                // The value is not valid : we don't add it.
                LOG.error( I18n.err( I18n.ERR_04450, value ) );
                return null;
            }
        }
        else
        {
            binaryValue = new BinaryValue( value );
        }
        
        return binaryValue;
    }



    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]
    /**
     * Create a new instance of a EntryAttribute, without ID nor value.
     */
    public DefaultEntryAttribute()
    {
    }


    /**
     * Create a new instance of a EntryAttribute, without ID nor value.
     * 
     * @param attributeType the attributeType for the empty attribute added into the entry
     */
    public DefaultEntryAttribute( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04442_NULL_AT_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        setAttributeType( attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, without value.
     */
    public DefaultEntryAttribute( String upId )
    {
        setUpId( upId );
    }


    /**
     * Create a new instance of a EntryAttribute, without value.
     * 
     * @param upId the ID for the added attributeType
     * @param attributeType the added AttributeType
     */
    public DefaultEntryAttribute( String upId, AttributeType attributeType )
    {
        if ( attributeType == null ) 
        {
            String message = I18n.err( I18n.ERR_04442_NULL_AT_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        setAttributeType( attributeType );
        setUpId( upId, attributeType );
    }


    /**
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ClientValue which uses the specified
     * attributeType.
     * 
     * Otherwise, the value is stored, but as a reference. It's not a copy.
     *
     * @param upId
     * @param vals an initial set of values for this attribute
     */
    public DefaultEntryAttribute( String upId, Value<?>... vals )
    {
        // The value can be null, this is a valid value.
        if ( vals[0] == null )
        {
             add( new StringValue() );
        }
        else
        {
            for ( Value<?> val:vals )
            {
                if ( ( val instanceof StringValue ) || ( val.isBinary() ) )
                {
                    add( val );
                }
                else
                {
                    String message = I18n.err( I18n.ERR_04129, val.getClass().getName() );
                    LOG.error( message );
                    throw new IllegalStateException( message );
                }
            }
        }
        
        setUpId( upId );
    }


    /**
     * Create a new instance of a EntryAttribute, without ID but with some values.
     * 
     * @param attributeType The attributeType added on creation
     * @param vals The added value for this attribute
     */
    public DefaultEntryAttribute( AttributeType attributeType, String... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute.
     * 
     * @param upId the ID for the added attribute
     * @param attributeType The attributeType added on creation
     * @param vals the added values for this attribute
     */
    public DefaultEntryAttribute( String upId, AttributeType attributeType, String... vals )
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04442_NULL_AT_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        setAttributeType( attributeType );
        add( vals );
        setUpId( upId, attributeType );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new Value which uses the specified
     * attributeType.
     * 
     * Otherwise, the value is stored, but as a reference. It's not a copy.
     *
     * @param upId the ID of the added attribute
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     */
    public DefaultEntryAttribute( String upId, AttributeType attributeType, Value<?>... vals )
    {
        if ( attributeType == null )
        {
            String message = I18n.err( I18n.ERR_04442_NULL_AT_NOT_ALLOWED );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        setAttributeType( attributeType );
        setUpId( upId, attributeType );
        add( vals );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new Value which uses the specified
     * attributeType.
     *
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     */
    public DefaultEntryAttribute( AttributeType attributeType, Value<?>... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute.
     */
    public DefaultEntryAttribute( String upId, String... vals )
    {
        add( vals );
        setUpId( upId );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     */
    public DefaultEntryAttribute( String upId, byte[]... vals )
    {
        add( vals );
        setUpId( upId );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     * 
     * @param attributeType The attributeType added on creation
     * @param vals The value for the added attribute
     */
    public DefaultEntryAttribute( AttributeType attributeType, byte[]... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     * 
     * @param upId the ID for the added attribute
     * @param attributeType the AttributeType to be added
     * @param vals the values for the added attribute
     */
    public DefaultEntryAttribute( String upId, AttributeType attributeType, byte[]... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04442_NULL_AT_NOT_ALLOWED ) );
        }

        setAttributeType( attributeType );
        add( vals );
        setUpId( upId, attributeType );
    }
    
    
    /**
     * 
     * Creates a new instance of DefaultServerAttribute, by copying
     * another attribute, which can be a ClientAttribute. If the other
     * attribute is a ServerAttribute, it will be copied.
     *
     * @param attributeType The attribute's type 
     * @param attribute The attribute to be copied
     */
    public DefaultEntryAttribute( AttributeType attributeType, EntryAttribute attribute )
    {
        // Copy the common values. isHR is only available on a ServerAttribute 
        this.attributeType = attributeType;
        this.id = attribute.getId();
        this.upId = attribute.getUpId();

        if ( attributeType == null )
        {
            isHR = attribute.isHR();

            // Copy all the values
            for ( Value<?> value:attribute )
            {
                add( value.clone() );
            }
        }
        else
        {
            
            isHR = attributeType.getSyntax().isHumanReadable();

            // Copy all the values
            for ( Value<?> clientValue:attribute )
            {
                Value<?> serverValue = null; 

                // We have to convert the value first
                if ( clientValue instanceof StringValue )
                {
                    if ( isHR )
                    {
                        serverValue = new StringValue( attributeType, clientValue.getString() );
                    }
                    else
                    {
                        // We have to convert the value to a binary value first
                        serverValue = new BinaryValue( attributeType, 
                            clientValue.getBytes() );
                    }
                }
                else if ( clientValue instanceof BinaryValue )
                {
                    if ( isHR )
                    {
                        // We have to convert the value to a String value first
                        serverValue = new StringValue( attributeType, 
                            clientValue.getString() );
                    }
                    else
                    {
                        serverValue = new BinaryValue( attributeType, clientValue.getBytes() );
                    }
                }

                add( serverValue );
            }
        }
    }

    
    /**
     * <p>
     * Get the byte[] value, if and only if the value is known to be Binary,
     * otherwise a InvalidAttributeValueException will be thrown
     * </p>
     * <p>
     * Note that this method returns the first value only.
     * </p>
     *
     * @return The value as a byte[]
     * @throws LdapInvalidAttributeValueException If the value is a String
     */
    public byte[] getBytes() throws LdapInvalidAttributeValueException
    {
        Value<?> value = get();
        
        if ( value.isBinary() )
        {
            return value.getBytes();
        }
        else
        {
            String message = I18n.err( I18n.ERR_04130 );
            LOG.error( message );
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, message );
        }
    }


    /**
     * <p>
     * Get the String value, if and only if the value is known to be a String,
     * otherwise a InvalidAttributeValueException will be thrown
     * </p>
     * <p>
     * Note that this method returns the first value only.
     * </p>
     *
     * @return The value as a String
     * @throws LdapInvalidAttributeValueException If the value is a byte[]
     */
    public String getString() throws LdapInvalidAttributeValueException
    {
        Value<?> value = get();
        
        if ( value instanceof StringValue )
        {
            return value.getString();
        }
        else
        {
            String message = I18n.err( I18n.ERR_04131 );
            LOG.error( message );
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, message );
        }
    }


    /**
     * Get's the attribute identifier. Its value is the same than the
     * user provided ID.
     *
     * @return the attribute's identifier
     */
    public String getId()
    {
        return id;
    }


    /**
     * <p>
     * Set the attribute to Human Readable or to Binary. 
     * </p>
     * @param isHR <code>true</code> for a Human Readable attribute, 
     * <code>false</code> for a Binary attribute.
     */
    public void setHR( boolean isHR )
    {
        //TODO : deal with the values, we may have to convert them.

        if ( attributeType == null )
        {
            this.isHR = isHR;
            
            // Compute the hashCode
            rehash();
        }
    }

    
    /**
     * Set the Attribute ID. 
     *
     * @param id The attribute ID
     * @throws IllegalArgumentException If the ID is empty or null or
     * resolve to an empty value after being trimmed
     */
    public void setId( String id )
    {
        String newId = StringTools.trim( StringTools.lowerCaseAscii( id ) );

        if ( newId.length() == 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04132 ) );
        }
        
        if ( attributeType != null )
        {
            if ( attributeType.getName() == null )
            {
                // If the name is null, then we may have to store an OID
                if ( !OID.isOID( newId )  || !attributeType.getOid().equals( newId ) )
                {
                    // This is an error
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_04132 ) );
                }
            }
            else
            {
                // We have at least one name. Check that the normalized upId
                // is one of those names. Otherwise, the upId may be an OID too.
                // In this case, it must be equals to the attributeType OID.
                for ( String atName:attributeType.getNames() )
                {
                    if ( atName.equalsIgnoreCase( newId ) )
                    {
                        // Found ! We can store the upId and get out
                        this.id = newId;
                        this.upId = id;
                        
                        // Compute the hashCode
                        rehash();
                        
                        return;
                    }
                }
                
                // Last case, the UpId is an OID
                if ( !OID.isOID( newId ) || !attributeType.getOid().equals( newId ) )
                {
                    // The id is incorrect : this is not allowed 
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_04455, id, attributeType.getName() ) );
                }
            }
        }

        this.id = newId;
        this.upId = id;
        
        // Compute the hashCode
        rehash();
    }

    
    /**
     * Get's the user provided identifier for this entry.  This is the value
     * that will be used as the identifier for the attribute within the
     * entry.  If this is a commonName attribute for example and the user
     * provides "COMMONname" instead when adding the entry then this is
     * the format the user will have that entry returned by the directory
     * server.  To do so we store this value as it was given and track it
     * in the attribute using this property.
     *
     * @return the user provided identifier for this attribute
     */
    public String getUpId()
    {
        return upId;
    }


    /**
     * Set the user provided ID. It will also set the ID, normalizing
     * the upId (removing spaces before and after, and lowercasing it)<br>
     * <br>
     * If the Attribute already has an AttributeType, then the upId must
     * be either the AttributeType name, or OID
     *
     * @param upId The attribute ID
     * @throws IllegalArgumentException If the ID is empty or null or
     * resolve to an empty value after being trimmed
     */
    public void setUpId( String upId )
    {
        setUpId( upId, null );
    }

    
    /**
     * Check that the upId is either a name or the OID of a given AT
     */
    private boolean areCompatible( String id, AttributeType attributeType )
    {
        // First, get rid of the options, if any
        int optPos = id.indexOf( ";" );
        String idNoOption = id;
        
        if ( optPos != -1 )
        {
            idNoOption = id.substring( 0, optPos );
        }
        
        // Check that we find the ID in the AT names
        for ( String name : attributeType.getNames() )
        {
            if ( name.equalsIgnoreCase( idNoOption ) )
            {
                return true;
            }
        }
        
        // Not found in names, check the OID
        if ( OID.isOID( id )  && attributeType.getOid().equals( id ) )
        {
            return true;
        }

        return false;
    }
    

    /**
     * <p>
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     * </p>
     * <p>
     * If we have an upId and an AttributeType, they must be compatible. :
     *  - if the upId is an OID, it must be the AttributeType's OID
     *  - otherwise, its normalized form must be equals to ones of
     *  the attributeType's names.
     * </p>
     * <p>
     * In any case, the ATtributeType will be changed. The caller is responsible for
     * the present values to be compatoble with the new AttributeType.
     * </p>
     *
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    public void setUpId( String upId, AttributeType attributeType )
    {
        String trimmed = StringTools.trim( upId );

        if ( StringTools.isEmpty( trimmed ) && ( attributeType == null ) )
        {
            throw new IllegalArgumentException( "Cannot set a null ID with a null AttributeType" );
        }
        
        String id = StringTools.toLowerCase( trimmed );
        
        if ( attributeType == null )
        {
            if ( this.attributeType == null )
            {
                this.upId = upId;
                this.id = id;
                
                // Compute the hashCode
                rehash();

                return;
            }    
            else
            {
                if ( areCompatible( id, this.attributeType ) )
                {
                    this.upId = upId;
                    this.id = id;
                    
                    // Compute the hashCode
                    rehash();

                    return;
                }
                else
                {
                    return;
                }
            }
        }
        
        if ( StringTools.isEmpty( id ) )
        {
            this.attributeType = attributeType;
            this.upId = attributeType.getName();
            this.id = StringTools.trim( this.upId );
            
            // Compute the hashCode
            rehash();

            return;
        }

        if ( areCompatible( id, attributeType ) )
        {
            this.upId = upId;
            this.id = id;
            this.attributeType = attributeType;
            
            // Compute the hashCode
            rehash();

            return;
        }

        throw new IllegalArgumentException( "ID '" + id + "' and AttributeType '" + attributeType.getName() + "' are not compatible " );
    }


    /**
     * <p>
     * Tells if the attribute is Human Readable. 
     * </p>
     * <p>This flag is set by the caller, or implicitly when adding String 
     * values into an attribute which is not yet declared as Binary.
     * </p> 
     * @return
     */
    public boolean isHR()
    {
        return isHR != null ? isHR : false; 
    }

    
    /**
     * Checks to see if this attribute is valid along with the values it contains.
     *
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws LdapException if there is a failure to check syntaxes of values
     */
    public boolean isValid() throws LdapException
    {
        if ( attributeType != null )
        {
            // First check if the attribute has more than one value
            // if the attribute is supposed to be SINGLE_VALUE
            if ( attributeType.isSingleValued() && ( values.size() > 1 ) )
            {
                return false;
            }

            // Check that we can have no value for this attributeType
            if ( values.size() == 0 )
            {
                return attributeType.getSyntax().getSyntaxChecker().isValidSyntax( null );
            }
        }

        for ( Value<?> value:values )
        {
            if ( !value.isValid() )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Checks to see if this attribute is valid along with the values it contains.
     *
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws LdapException if there is a failure to check syntaxes of values
     */
    public boolean isValid( SyntaxChecker checker ) throws LdapException
    {
        for ( Value<?> value : values )
        {
            if ( !value.isValid( checker ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * <p>
     * If the value's type is different from the attribute's type,
     * a conversion is done. For instance, if we try to set some 
     * StringValue into a Binary attribute, we just store the UTF-8 
     * byte array encoding for this StringValue.
     * </p>
     * <p>
     * If we try to store some BinaryValue in a HR attribute, we try to 
     * convert those BinaryValue assuming they represent an UTF-8 encoded
     * String. Of course, if it's not the case, the stored value will
     * be incorrect.
     * </p>
     * <p>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * </p>
     * <p>
     * The caller can set the HR flag in order to enforce a type for 
     * the current attribute, otherwise this type will be set while
     * adding the first value, using the value's type to set the flag.
     * </p>
     * <p>
     * <b>Note : </b>If the entry contains no value, and the unique added value
     * is a null length value, then this value will be considered as
     * a binary value.
     * </p>
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings( value="NP_LOAD_OF_KNOWN_NULL_VALUE", 
        justification="Validity of null depends on the checker")
    public int add( Value<?>... vals )
    {
        int nbAdded = 0;
        BinaryValue nullBinaryValue = null;
        StringValue nullStringValue = null;
        boolean nullValueAdded = false;
        
        if ( attributeType != null )
        {
            for ( Value<?> val:vals )
            {
                if ( attributeType.getSyntax().isHumanReadable() )
                {
                    if ( ( val == null ) || val.isNull() )
                    {
                        Value<String> nullSV = new StringValue( attributeType, (String)null );
                        
                        if ( values.add( nullSV ) )
                        {
                            nbAdded++;
                        }
                    }
                    else if ( val instanceof StringValue )
                    {
                        StringValue stringValue = (StringValue)val;
                        
                        if ( stringValue.getAttributeType() == null )
                        {
                            stringValue.apply( attributeType );
                        }
                        
                        if ( values.add( val ) )
                        {
                            nbAdded++;
                        }
                    }
                    else
                    {
                        String message = I18n.err( I18n.ERR_04451 );
                        LOG.error( message );
                    }
                }
                else
                {
                    if ( val == null )
                    {
                        if ( attributeType.getSyntax().getSyntaxChecker().isValidSyntax( val ) )
                        {
                            Value<byte[]> nullSV = new BinaryValue( attributeType, (byte[])null );
                            
                            if ( values.add( nullSV ) )
                            {
                                nbAdded++;
                            }
                        }
                        else
                        {
                            String message = I18n.err( I18n.ERR_04452 );
                            LOG.error( message );
                        }
                    }
                    else
                    {
                        if ( val instanceof BinaryValue )
                        {
                            BinaryValue binaryValue = (BinaryValue)val;
                            
                            if ( binaryValue.getAttributeType() == null )
                            {
                                binaryValue = new BinaryValue( attributeType, val.getBytes() ); 
                            }
        
                            if ( values.add( binaryValue ) )
                            {
                                nbAdded++;
                            }
                        }
                        else
                        {
                            String message = I18n.err( I18n.ERR_04452 );
                            LOG.error( message );
                        }
                    }
                }
            }
        }
        else
        {
            for ( Value<?> val:vals )
            {
                if ( val == null )
                {
                    // We have a null value. If the HR flag is not set, we will consider 
                    // that the attribute is not HR. We may change this later
                    if ( isHR == null )
                    {
                        // This is the first value. Add both types, as we 
                        // don't know yet the attribute type's, but we may
                        // know later if we add some new value.
                        // We have to do that because we are using a Set,
                        // and we can't remove the first element of the Set.
                        nullBinaryValue = new BinaryValue( (byte[])null );
                        nullStringValue = new StringValue( (String)null );
                        
                        values.add( nullBinaryValue );
                        values.add( nullStringValue );
                        nullValueAdded = true;
                        nbAdded++;
                    }
                    else if ( !isHR )
                    {
                        // The attribute type is binary.
                        nullBinaryValue = new BinaryValue( (byte[])null );
                        
                        // Don't add a value if it already exists. 
                        if ( !values.contains( nullBinaryValue ) )
                        {
                            values.add( nullBinaryValue );
                            nbAdded++;
                        }
                        
                    }
                    else
                    {
                        // The attribute is HR
                        nullStringValue = new StringValue( (String)null );
                        
                        // Don't add a value if it already exists. 
                        if ( !values.contains( nullStringValue ) )
                        {
                            values.add( nullStringValue );
                        }
                    }
                }
                else
                {
                    // Let's check the value type. 
                    if ( val instanceof StringValue )
                    {
                        // We have a String value
                        if ( isHR == null )
                        {
                            // The attribute type will be set to HR
                            isHR = true;
                            values.add( val );
                            nbAdded++;
                        }
                        else if ( !isHR )
                        {
                            // The attributeType is binary, convert the
                            // value to a BinaryValue
                            BinaryValue bv = new BinaryValue( val.getBytes() );
                            
                            if ( !contains( bv ) )
                            {
                                values.add( bv );
                                nbAdded++;
                            }
                        }
                        else
                        {
                            // The attributeType is HR, simply add the value
                            if ( !contains( val ) )
                            {
                                values.add( val );
                                nbAdded++;
                            }
                        }
                    }
                    else
                    {
                        // We have a Binary value
                        if ( isHR == null )
                        {
                            // The attribute type will be set to binary
                            isHR = false;
                            values.add( val );
                            nbAdded++;
                        }
                        else if ( !isHR )
                        {
                            // The attributeType is not HR, simply add the value if it does not already exist
                            if ( !contains( val ) )
                            {
                                values.add( val );
                                nbAdded++;
                            }
                        }
                        else
                        {
                            // The attribute Type is HR, convert the
                            // value to a StringValue
                            StringValue sv = new StringValue( val.getString() );
                            
                            if ( !contains( sv ) )
                            {
                                values.add( sv );
                                nbAdded++;
                            }
                        }
                    }
                }
            }
        }

        // Last, not least, if a nullValue has been added, and if other 
        // values are all String, we have to keep the correct nullValue,
        // and to remove the other
        if ( nullValueAdded )
        {
            if ( isHR ) 
            {
                // Remove the Binary value
                values.remove( nullBinaryValue );
            }
            else
            {
                // Remove the String value
                values.remove( nullStringValue );
            }
        }

        return nbAdded;
    }


    /**
     * @see EntryAttribute#add(String...)
     */
    public int add( String... vals )
    {
        int nbAdded = 0;
        
        // First, if the isHR flag is not set, we assume that the
        // attribute is HR, because we are asked to add some strings.
        if ( isHR == null )
        {
            isHR = true;
        }

        // Check the attribute type.
        if ( attributeType == null )
        {
            if ( isHR )
            {
                for ( String val:vals )
                {
                    Value<String> value = createStringValue( attributeType, val );
                    
                    if ( value == null )
                    {
                        // The value can't be normalized : we don't add it.
                        LOG.error( I18n.err( I18n.ERR_04449, val ) );
                        continue;
                    }
                    
                    // Call the add(Value) method, if not already present
                    if ( add( value ) == 1 )
                    {
                        nbAdded++;
                    }
                    else
                    {
                        LOG.error( I18n.err( I18n.ERR_04450, val ) );
                    }
                }
            }
            else
            {
                // The attribute is binary. Transform the String to byte[]
                for ( String val:vals )
                {
                    byte[] valBytes = null;
                    
                    if ( val != null )
                    {
                        valBytes = StringTools.getBytesUtf8( val );
                    }
                    
                    Value<byte[]> value = createBinaryValue( attributeType, valBytes );
                    
                    if ( value == null )
                    {
                        // The value can't be normalized or is invalid : we don't add it.
                        LOG.error( I18n.err( I18n.ERR_04449, val ) );
                        continue;
                    }
                    
                    // Now call the add(Value) method
                    if ( add( value ) == 1 )
                    {
                        nbAdded++;
                    }
                }
            }
        }
        else
        {
            if ( isHR )
            {
                for ( String val:vals )
                {
                    Value<String> value = createStringValue( attributeType, val );
                    
                    if ( value == null )
                    {
                        // The value can't be normalized : we don't add it.
                        LOG.error( I18n.err( I18n.ERR_04449, val ) );
                        continue;
                    }
                    
                    // Call the add(Value) method, if not already present
                    if ( add( value ) == 1 )
                    {
                        nbAdded++;
                    }
                    else
                    {
                        LOG.error( I18n.err( I18n.ERR_04450, val ) );
                    }
                }
            }
            else
            {
                // The attribute is binary. Transform the String to byte[]
                for ( String val:vals )
                {
                    byte[] valBytes = null;
                    
                    if ( val != null )
                    {
                        valBytes = StringTools.getBytesUtf8( val );
                    }
                    
                    Value<byte[]> value = createBinaryValue( attributeType, valBytes );
                    
                    if ( value == null )
                    {
                        // The value can't be normalized or is invalid : we don't add it.
                        LOG.error( I18n.err( I18n.ERR_04449, val ) );
                        continue;
                    }
                    
                    // Now call the add(Value) method
                    if ( add( value ) == 1 )
                    {
                        nbAdded++;
                    }
                }
            }
        }
        
        return nbAdded;
    }    
    
    
    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * If the value's type is different from the attribute's type,
     * a conversion is done. For instance, if we try to set some String
     * into a Binary attribute, we just store the UTF-8 byte array 
     * encoding for this String.
     * If we try to store some byte[] in a HR attribute, we try to 
     * convert those byte[] assuming they represent an UTF-8 encoded
     * String. Of course, if it's not the case, the stored value will
     * be incorrect.
     * <br>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * <br>
     * The caller can set the HR flag in order to enforce a type for 
     * the current attribute, otherwise this type will be set while
     * adding the first value, using the value's type to set the flag.
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    public int add( byte[]... vals )
    {
        int nbAdded = 0;
        
        // First, if the isHR flag is not set, we assume that the
        // attribute is not HR, because we are asked to add some byte[].
        if ( isHR == null )
        {
            isHR = false;
        }
        
        if ( !isHR )
        {
            for ( byte[] val:vals )
            {
                Value<byte[]> value = null;
                
                if ( attributeType == null )
                {
                    value = new BinaryValue( val );
                }
                else
                {
                    value = new BinaryValue( attributeType, val );
                    
                    try
                    {
                        value.normalize();
                    }
                    catch( LdapException ne )
                    {
                        // The value can't be normalized : we don't add it.
                        LOG.error( I18n.err( I18n.ERR_04449, StringTools.dumpBytes( val ) ) );
                        return 0;
                    }
                }
                
                if ( add( value ) != 0 )
                {
                    nbAdded++;
                }
                else
                {
                    LOG.error( I18n.err( I18n.ERR_04450, StringTools.dumpBytes( val ) ) );
                }
            }
        }
        else
        {
            // We can't add Binary values into a String Attribute
            LOG.info( I18n.err( I18n.ERR_04451 ) );
            return 0;
        }
        
        return nbAdded;
    }    
    
    
    /**
     * Remove all the values from this attribute.
     */
    public void clear()
    {
        values.clear();
    }


    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is HR, the binary values will be converted to String before
     * being checked.
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( Value<?>... vals )
    {
        if ( isHR == null )
        {
            // If this flag is null, then there is no values.
            return false;
        }

        if ( attributeType == null )
        {
            if ( isHR )
            {
                // Iterate through all the values, convert the Binary values
                // to String values, and quit id any of the values is not
                // contained in the object
                for ( Value<?> val:vals )
                {
                    if ( val instanceof StringValue )
                    {
                        if ( !values.contains( val ) )
                        {
                            return false;
                        }
                    }
                    else
                    {
                        byte[] binaryVal = val.getBytes();
                        
                        // We have to convert the binary value to a String
                        if ( ! values.contains( new StringValue( StringTools.utf8ToString( binaryVal ) ) ) )
                        {
                            return false;
                        }
                    }
                }
            }
            else
            {
                // Iterate through all the values, convert the String values
                // to binary values, and quit id any of the values is not
                // contained in the object
                for ( Value<?> val:vals )
                {
                    if ( val.isBinary() )
                    {
                        if ( !values.contains( val ) )
                        {
                            return false;
                        }
                    }
                    else
                    {
                        String stringVal = val.getString();
                        
                        // We have to convert the binary value to a String
                        if ( ! values.contains( new BinaryValue( StringTools.getBytesUtf8( stringVal ) ) ) )
                        {
                            return false;
                        }
                    }
                }
            }
        }
        else
        {
            // Iterate through all the values, and quit if we 
            // don't find one in the values. We have to separate the check
            // depending on the isHR flag value.
            if ( isHR )
            {
                for ( Value<?> val:vals )
                {
                    if ( val instanceof StringValue )
                    {
                        StringValue stringValue = (StringValue)val;
                        
                        if ( stringValue.getAttributeType() == null )
                        {
                            stringValue.apply( attributeType );
                        }
                        
                        if ( !values.contains( val ) )
                        {
                            return false;
                        }
                    }
                    else
                    {
                        // Not a String value
                        return false;
                    }
                }
            }
            else
            {
                for ( Value<?> val:vals )
                {
                    if ( val instanceof BinaryValue )
                    {
                        if ( !values.contains( val ) )
                        {
                            return false;
                        }
                    }
                    else
                    {
                        // Not a Binary value
                        return false;
                    }
                }
            }
        }
        
        return true;
    }


    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is not HR, the values will be converted to byte[]
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( String... vals )
    {
        if ( isHR == null )
        {
            // If this flag is null, then there is no values.
            return false;
        }

        if ( attributeType == null )
        {
            if ( isHR )
            {
                for ( String val:vals )
                {
                    
                    if ( !contains( new StringValue( val ) ) )
                    {
                        return false;
                    }
                }
            }
            else
            {
                // As the attribute type is binary, we have to convert 
                // the values before checking for them in the values
                // Iterate through all the values, and quit if we 
                // don't find one in the values
                for ( String val:vals )
                {
                    byte[] binaryVal = StringTools.getBytesUtf8( val );
    
                    if ( !contains( new BinaryValue( binaryVal ) ) )
                    {
                        return false;
                    }
                }
            }
        }
        else
        {
            if ( isHR )
            {
                // Iterate through all the values, and quit if we 
                // don't find one in the values
                for ( String val:vals )
                {
                    StringValue value = new StringValue( attributeType, val );
                    
                    if ( !values.contains( value ) )
                    {
                        return false;
                    }
                }
                
                return true;
            }
            else
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is HR, the values will be converted to String
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( byte[]... vals )
    {
        if ( isHR == null )
        {
            // If this flag is null, then there is no values.
            return false;
        }

        if ( attributeType == null )
        {
            if ( !isHR )
            {
                // Iterate through all the values, and quit if we 
                // don't find one in the values
                for ( byte[] val:vals )
                {
                    if ( !contains( new BinaryValue( val ) ) )
                    {
                        return false;
                    }
                }
            }
            else
            {
                // As the attribute type is String, we have to convert 
                // the values before checking for them in the values
                // Iterate through all the values, and quit if we 
                // don't find one in the values
                for ( byte[] val:vals )
                {
                    String stringVal = StringTools.utf8ToString( val );
    
                    if ( !contains( new StringValue( stringVal ) ) )
                    {
                        return false;
                    }
                }
            }
        }
        else
        {
            if ( !isHR )
            {
                // Iterate through all the values, and quit if we 
                // don't find one in the values
                for ( byte[] val:vals )
                {
                    BinaryValue value = new BinaryValue( attributeType, val );
                    
                    try
                    {
                        value.normalize();
                    }
                    catch ( LdapException ne )
                    {
                        return false;
                    }
                    
                    if ( !values.contains( value ) )
                    {
                        return false;
                    }
                }
                
                return true;
            }
            else
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * @see EntryAttribute#contains(Object...)
     */
    public boolean contains( Object... vals )
    {
        boolean isHR = true;
        boolean seen = false;
        
        // Iterate through all the values, and quit if we 
        // don't find one in the values
        for ( Object val:vals )
        {
            if ( ( val instanceof String ) ) 
            {
                if ( !seen )
                {
                    isHR = true;
                    seen = true;
                }

                if ( isHR )
                {
                    if ( !contains( (String)val ) )
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                if ( !seen )
                {
                    isHR = false;
                    seen = true;
                }

                if ( !isHR )
                {
                    if ( !contains( (byte[])val ) )
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
        }
        
        return true;
    }

    
    /**
     * <p>
     * Get the first value of this attribute. If there is none, 
     * null is returned.
     * </p>
     * <p>
     * Note : even if we are storing values into a Set, one can assume
     * the values are ordered following the insertion order.
     * </p>
     * <p> 
     * This method is meant to be used if the attribute hold only one value.
     * </p>
     * 
     *  @return The first value for this attribute.
     */
    public Value<?> get()
    {
        if ( values.isEmpty() )
        {
            return null;
        }
        
        return values.iterator().next();
    }


    /**
     * <p>
     * Get the nth value of this attribute. If there is none, 
     * null is returned.
     * </p>
     * <p>
     * Note : even if we are storing values into a Set, one can assume
     * the values are ordered following the insertion order.
     * </p>
     * <p> 
     * 
     * @param i the index  of the value to get
     *  @return The nth value for this attribute.
     */
    public Value<?> get( int i )
    {
        if ( values.size() < i )
        {
            return null;
        }
        else
        {
            int n = 0;
            
            for ( Value<?> value:values )
            {
                if ( n == i )
                {
                    return value;
                }
                
                n++;
            }
        }
        
        // fallback to 
        return null;
    }
    
    
    /**
     * Returns an iterator over all the attribute's values.
     * <p>
     * The effect on the returned enumeration of adding or removing values of
     * the attribute is not specified.
     * </p>
     * <p>
     * This method will throw any <code>LdapException</code> that occurs.
     * </p>
     *
     * @return an enumeration of all values of the attribute
     */
    public Iterator<Value<?>> getAll()
    {
        return iterator();
    }


    /**
     * Retrieves the number of values in this attribute.
     *
     * @return the number of values in this attribute, including any values
     * wrapping a null value if there is one
     */
    public int size()
    {
        return values.size();
    }


    /**
     * <p>
     * Removes all the  values that are equal to the given values.
     * </p>
     * <p>
     * Returns true if all the values are removed.
     * </p>
     * <p>
     * If the attribute type is HR and some value which are not String, we
     * will convert the values first (same thing for a non-HR attribute).
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    public boolean remove( Value<?>... vals )
    {
        if ( ( isHR == null ) || ( values.size() == 0 ) ) 
        {
            // Trying to remove a value from an empty list will fail
            return false;
        }
        
        boolean removed = true;
        
        if ( attributeType == null )
        {
            if ( isHR )
            {
                for ( Value<?> val:vals )
                {
                    if ( val instanceof StringValue )
                    {
                        removed &= values.remove( val );
                    }
                    else
                    {
                        // Convert the binary value to a string value
                        byte[] binaryVal = val.getBytes();
                        removed &= values.remove( new StringValue( StringTools.utf8ToString( binaryVal ) ) );
                    }
                }
            }
            else
            {
                for ( Value<?> val:vals )
                {
                    removed &= values.remove( val );
                }
            }
        }
        else
        {
            // Loop through all the values to remove. If one of
            // them is not present, the method will return false.
            // As the attribute may be HR or not, we have two separated treatments
            if ( isHR )
            {
                for ( Value<?> val:vals )
                {
                    if ( val instanceof StringValue )
                    {
                        StringValue stringValue = (StringValue)val;
                        
                        if ( stringValue.getAttributeType() == null )
                        {
                            stringValue.apply( attributeType );
                        }
                        
                        removed &= values.remove( stringValue );
                    }
                    else
                    {
                        removed = false;
                    }
                }
            }
            else
            {
                for ( Value<?> val:vals )
                {
                    if ( val instanceof BinaryValue )
                    {
                        BinaryValue binaryValue = (BinaryValue)val;
                        
                        if ( binaryValue.getAttributeType() == null )
                        {
                            binaryValue.apply( attributeType );
                        }
                        
                        removed &= values.remove( binaryValue );
                    }
                    else
                    {
                        removed = false;
                    }
                }
            }
        }
        
        return removed;
    }


    /**
     * <p>
     * Removes all the  values that are equal to the given values.
     * </p>
     * <p>
     * Returns true if all the values are removed.
     * </p>
     * <p>
     * If the attribute type is HR, then the values will be first converted
     * to String
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    public boolean remove( byte[]... vals )
    {
        if ( ( isHR == null ) || ( values.size() == 0 ) ) 
        {
            // Trying to remove a value from an empty list will fail
            return false;
        }
        
        boolean removed = true;
        
        if ( attributeType == null )
        {
            if ( !isHR )
            {
                // The attribute type is not HR, we can directly process the values
                for ( byte[] val:vals )
                {
                    BinaryValue value = new BinaryValue( val );
                    removed &= values.remove( value );
                }
            }
            else
            {
                // The attribute type is String, we have to convert the values
                // to String before removing them
                for ( byte[] val:vals )
                {
                    StringValue value = new StringValue( StringTools.utf8ToString( val ) );
                    removed &= values.remove( value );
                }
            }
        }
        else
        {
            if ( !isHR ) 
            {
                for ( byte[] val:vals )
                {
                    BinaryValue value = new BinaryValue( attributeType, val );
                    removed &= values.remove( value );
                }
            }
            else
            {
                removed = false;
            }
        }
        
        return removed;
    }


    /**
     * Removes all the  values that are equal to the given values.
     * <p>
     * Returns true if all the values are removed.
     * </p>
     * <p>
     * If the attribute type is not HR, then the values will be first converted
     * to byte[]
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    public boolean remove( String... vals )
    {
        if ( ( isHR == null ) || ( values.size() == 0 ) ) 
        {
            // Trying to remove a value from an empty list will fail
            return false;
        }
        
        boolean removed = true;
        
        if ( attributeType == null )
        {
            if ( isHR )
            {
                // The attribute type is HR, we can directly process the values
                for ( String val:vals )
                {
                    StringValue value = new StringValue( val );
                    removed &= values.remove( value );
                }
            }
            else
            {
                // The attribute type is binary, we have to convert the values
                // to byte[] before removing them
                for ( String val:vals )
                {
                    BinaryValue value = new BinaryValue( StringTools.getBytesUtf8( val ) );
                    removed &= values.remove( value );
                }
            }
        }
        else
        {
            if ( isHR )
            {
                for ( String val:vals )
                {
                    StringValue value = new StringValue( attributeType, val );
                    removed &= values.remove( value );
                }
            }
            else
            {
                removed = false;
            }
        }
        
        return removed;
    }


    /**
     * An iterator on top of the stored values.
     * 
     * @return an iterator over the stored values.
     */
    public Iterator<Value<?>> iterator()
    {
        return values.iterator();
    }
    
    
    /*
     * Puts some values to this attribute.
     * <p>
     * The new values will replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param val some values to be put which may be null
     * @return the number of added values, or 0 if none has been added
     *
    public int put( String... vals )
    {
        values.clear();
        return add( vals );
    }
    
    
    /**
     * Puts some values to this attribute.
     * <p>
     * The new values will replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param val some values to be put which may be null
     * @return the number of added values, or 0 if none has been added
     *
    public int put( byte[]... vals )
    {
        values.clear();
        return add( vals );
    }

    
    /**
     * Puts some values to this attribute.
     * <p>
     * The new values are replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param val some values to be put which may be null
     * @return the number of added values, or 0 if none has been added
     *
    public int put( Value<?>... vals )
    {
        values.clear();
        return add( vals );
    }
    
    
    /**
     * <p>
     * Puts a list of values into this attribute.
     * </p>
     * <p>
     * The new values will replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param vals the values to be put
     * @return the number of added values, or 0 if none has been added
     *
    public int put( List<Value<?>> vals )
    {
        values.clear();
        
        // Transform the List to an array
        Value<?>[] valArray = new Value<?>[vals.size()];
        return add( vals.toArray( valArray ) );
    }
    */
    


    /**
     * Get the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    public AttributeType getAttributeType()
    {
        return attributeType;
    }
    
    
    /**
     * <p>
     * Set the attribute type associated with this ServerAttribute.
     * </p>
     * <p>
     * The current attributeType will be replaced. It is the responsibility of
     * the caller to insure that the existing values are compatible with the new
     * AttributeType
     * </p>
     *
     * @param attributeType the attributeType associated with this entry attribute
     */
    public void setAttributeType( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }

        this.attributeType = attributeType;
        setUpId( null, attributeType );
        
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            isHR = true;
        }
        else
        {
            isHR = false;
        }
        
        // Compute the hashCode
        rehash();
    }
    
    
    /**
     * <p>
     * Check if the current attribute type is of the expected attributeType
     * </p>
     * <p>
     * This method won't tell if the current attribute is a descendant of 
     * the attributeType. For instance, the "CN" serverAttribute will return
     * false if we ask if it's an instance of "Name". 
     * </p> 
     *
     * @param attributeId The AttributeType ID to check
     * @return True if the current attribute is of the expected attributeType
     * @throws LdapInvalidAttributeValueException If there is no AttributeType
     */
    public boolean instanceOf( String attributeId ) throws LdapInvalidAttributeValueException
    {
        String trimmedId = StringTools.trim( attributeId );
        
        if ( StringTools.isEmpty( trimmedId ) )
        {
            return false;
        }
        
        String normId = StringTools.lowerCaseAscii( trimmedId );
        
        for ( String name:attributeType.getNames() )
        {
            if ( normId.equalsIgnoreCase( name ) )
            {
                return true;
            }
        }
        
        return normId.equalsIgnoreCase( attributeType.getOid() );
    }


    //-------------------------------------------------------------------------
    // Overloaded Object classes
    //-------------------------------------------------------------------------
    /**
     * A helper method to rehash the hashCode
     */
    private void rehash()
    {
        h = 37;
        
        if ( isHR != null )
        {
            h = h*17 + isHR.hashCode();
        }
        
        if ( id != null )
        {
            h = h*17 + id.hashCode();
        }
        
        /*
        // We have to sort the values if we wnt to correctly compare two Attributes
        if ( isHR )
        {
            SortedSet<String> sortedSet = new TreeSet<String>();

            for ( Value<?> value:values )
            {
                sortedSet.add( (String)value.getNormalizedValueReference() );
            }
            
            for ( String value:sortedSet )
            {
                h = h*17 + value.hashCode();
            }
        }
        else
        {
            SortedSet<byte[]> sortedSet = new TreeSet<byte[]>();
            
            for ( Value<?> value:values )
            {
                sortedSet.add( (byte[])value.getNormalizedValueReference() );
            }
            
            for ( byte[] value:sortedSet )
            {
                h = h*17 + ArrayUtils.hashCode( value );
            }
        }
        */
        
        if ( attributeType != null )
        {
            h = h*17 + attributeType.hashCode();
        }
    }

    
    /**
     * The hashCode is based on the id, the isHR flag and 
     * on the internal values.
     *  
     * @see Object#hashCode()
     * @return the instance's hashcode 
     */
    public int hashCode()
    {
        if ( h == 0 )
        {
            rehash();
        }
        
        return h;
    }
    
    
    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        
        if ( ! (obj instanceof EntryAttribute ) )
        {
            return false;
        }
        
        EntryAttribute other = (EntryAttribute)obj;
        
        if ( id == null )
        {
            if ( other.getId() != null )
            {
                return false;
            }
        }
        else
        {
            if ( other.getId() == null )
            {
                return false;
            }
            else
            {
                if ( attributeType != null )
                {
                    if ( !attributeType.equals( other.getAttributeType() ) )
                    {
                        return false;
                    }
                }
                else if ( !id.equals( other.getId() ) )
                {
                    return false;
                }
            }
        }
        
        if ( isHR() !=  other.isHR() )
        {
            return false;
        }
        
        if ( values.size() != other.size() )
        {
            return false;
        }
        
        for ( Value<?> val:values )
        {
            if ( ! other.contains( val ) )
            {
                return false;
            }
        }
        
        if ( attributeType == null )
        {
            return other.getAttributeType() == null;
        }
        
        return attributeType.equals( other.getAttributeType() );
    }
    
    
    /**
     * @see Cloneable#clone()
     */
    public EntryAttribute clone()
    {
        try
        {
            DefaultEntryAttribute attribute = (DefaultEntryAttribute)super.clone();
            attribute.setUpId( upId );
            
            attribute.values = new LinkedHashSet<Value<?>>( values.size() );
            
            for ( Value<?> value:values )
            {
                attribute.values.add( value.clone() );
            }
            
            return attribute;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }
    
    
    /**
     * @see Object#toString() 
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        if ( ( values != null ) && ( values.size() != 0 ) )
        {
            for ( Value<?> value:values )
            {
                sb.append( "    " ).append( upId ).append( ": " );
                
                if ( value.isNull() )
                {
                    sb.append( "''" );
                }
                else
                {
                    sb.append( value );
                }
                
                sb.append( '\n' );
            }
        }
        else
        {
            sb.append( "    " ).append( upId ).append( ": (null)\n" );
        }
        
        return sb.toString();
    }


    
    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     * <p>
     * 
     * This is the place where we serialize attributes, and all theirs
     * elements. 
     * 
     * The inner structure is the same as the client attribute, but we can't call
     * it as we won't be able to serialize the serverValues
     * 
     */
    public void serialize( ObjectOutput out ) throws IOException
    {
        // Write the UPId (the id will be deduced from the upID)
        UTFUtils.writeUTF( out, upId );
        
        // Write the HR flag, if not null
        if ( isHR != null )
        {
            out.writeBoolean( true );
            out.writeBoolean( isHR );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // Write the number of values
        out.writeInt( size() );
        
        if ( size() > 0 ) 
        {
            // Write each value
            for ( Value<?> value:values )
            {
                // Write the value, using the correct method
                if ( value instanceof StringValue )
                {
                    ((StringValue)value).serialize( out );
                }
                else
                {
                    ((BinaryValue)value).serialize( out );
                }
            }
        }
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void deserialize( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the ID and the UPId
        upId = UTFUtils.readUTF( in );
        
        // Compute the id
        setUpId( upId );
        
        // Read the HR flag, if not null
        if ( in.readBoolean() )
        {
            isHR = in.readBoolean();
        }

        // Read the number of values
        int nbValues = in.readInt();

        if ( nbValues > 0 )
        {
            for ( int i = 0; i < nbValues; i++ )
            {
                Value<?> value = null;
                
                if ( isHR )
                {
                    value  = new StringValue( attributeType );
                    ((StringValue)value).deserialize( in );
                }
                else
                {
                    value  = new BinaryValue( attributeType );
                    ((BinaryValue)value).deserialize( in );
                }
                
                try
                {
                    value.normalize();
                }
                catch ( LdapException ne )
                {
                    // Do nothing...
                }
                    
                values.add( value );
            }
        }
    }
    
    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     * <p>
     * 
     * This is the place where we serialize attributes, and all theirs
     * elements. 
     * 
     * The inner structure is :
     * 
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // Write the UPId (the id will be deduced from the upID)
        UTFUtils.writeUTF( out, upId );
        
        // Write the HR flag, if not null
        if ( isHR != null )
        {
            out.writeBoolean( true );
            out.writeBoolean( isHR );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // Write the number of values
        out.writeInt( size() );
        
        if ( size() > 0 ) 
        {
            // Write each value
            for ( Value<?> value:values )
            {
                // Write the value
                out.writeObject( value );
            }
        }
        
        out.flush();
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the ID and the UPId
        upId = UTFUtils.readUTF( in );
        
        // Compute the id
        setUpId( upId );
        
        // Read the HR flag, if not null
        if ( in.readBoolean() )
        {
            isHR = in.readBoolean();
        }

        // Read the number of values
        int nbValues = in.readInt();

        if ( nbValues > 0 )
        {
            for ( int i = 0; i < nbValues; i++ )
            {
                values.add( (Value<?>)in.readObject() );
            }
        }
    }
}
