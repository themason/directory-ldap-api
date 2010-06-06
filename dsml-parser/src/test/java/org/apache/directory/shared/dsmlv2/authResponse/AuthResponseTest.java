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

package org.apache.directory.shared.dsmlv2.authResponse;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.directory.shared.dsmlv2.AbstractResponseTest;
import org.apache.directory.shared.dsmlv2.Dsmlv2ResponseParser;
import org.apache.directory.shared.ldap.codec.LdapResultCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;


/**
 * Tests for the Auth Response parsing
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthResponseTest extends AbstractResponseTest
{

    /**
     * Test parsing of a Response with the (optional) requestID attribute
     */
    @Test
    public void testResponseWithRequestId()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                AuthResponseTest.class.getResource( "response_with_requestID_attribute.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 456, bindResponse.getMessageId() );
    }


    /**
     * Test parsing of a Response with the (optional) requestID attribute equals 0
     */
    @Test
    public void testResponseWithRequestIdEquals0()
    {
        testParsingFail( AuthResponseTest.class, "response_with_requestID_equals_0.xml" );
    }


    /**
     * Test parsing of a response with a (optional) Control element
     */
    @Test
    public void testResponseWith1Control()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_1_control.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 1, bindResponse.getControls().size() );

        Control control = bindResponse.getCurrentControl();

        assertTrue( control.isCritical() );

        assertEquals( "1.2.840.113556.1.4.643", control.getOid() );

        assertEquals( "Some text", StringTools.utf8ToString( ( byte[] ) control.getValue() ) );
    }


    /**
     * Test parsing of a response with a (optional) Control element with empty value
     */
    @Test
    public void testResponseWith1ControlEmptyValue()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_1_control_empty_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();
        Control control = bindResponse.getCurrentControl();

        assertEquals( 1, bindResponse.getControls().size() );
        assertTrue( control.isCritical() );
        assertEquals( "1.2.840.113556.1.4.643", control.getOid() );
        assertFalse( control.hasValue() );
    }


    /**
     * Test parsing of a response with 2 (optional) Control elements
     */
    @Test
    public void testResponseWith2Controls()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser
                .setInput( AuthResponseTest.class.getResource( "response_with_2_controls.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 2, bindResponse.getControls().size() );

        Control control = bindResponse.getCurrentControl();

        assertFalse( control.isCritical() );

        assertEquals( "1.2.840.113556.1.4.789", control.getOid() );

        assertEquals( "Some other text", StringTools.utf8ToString( ( byte[] ) control.getValue() ) );
    }


    /**
     * Test parsing of a response with 3 (optional) Control elements without value
     */
    @Test
    public void testResponseWith3ControlsWithoutValue()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_3_controls_without_value.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        assertEquals( 3, bindResponse.getControls().size() );

        Control control = bindResponse.getCurrentControl();

        assertTrue( control.isCritical() );

        assertEquals( "1.2.840.113556.1.4.456", control.getOid() );

        assertFalse( control.hasValue() );
    }


    /**
     * Test parsing of a response without Result Code element
     */
    @Test
    public void testResponseWithoutResultCode()
    {
        testParsingFail( AuthResponseTest.class, "response_without_result_code.xml" );
    }


    /**
     * Test parsing of a response with Result Code element but a not integer value
     */
    @Test
    public void testResponseWithResultCodeNotInteger()
    {
        testParsingFail( AuthResponseTest.class, "response_with_result_code_not_integer.xml" );
    }


    /**
     * Test parsing of a response with Result Code 
     */
    @Test
    public void testResponseWithResultCode()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_result_code.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        assertEquals( ResultCodeEnum.PROTOCOL_ERROR, ldapResult.getResultCode() );
    }


    /**
     * Test parsing of a response with Error Message
     */
    @Test
    public void testResponseWithErrorMessage()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_error_message.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        assertEquals( "Unrecognized extended operation EXTENSION_OID: 1.2.6.1.4.1.18060.1.1.1.100.2", ldapResult
            .getErrorMessage() );
    }


    /**
     * Test parsing of a response with Empty Error Message
     */
    @Test
    public void testResponseWithEmptyErrorMessage()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                AuthResponseTest.class.getResource( "response_with_empty_error_message.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        assertNull( ldapResult.getErrorMessage() );
    }


    /**
     * Test parsing of a response with a Referral
     */
    @Test
    public void testResponseWith1Referral()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser
                .setInput( AuthResponseTest.class.getResource( "response_with_1_referral.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        List<org.apache.directory.shared.ldap.util.LdapURL> referrals = ldapResult.getReferrals();

        assertEquals( 1, referrals.size() );

        Object referral = referrals.get( 0 );

        try
        {
            assertEquals( new LdapURL( "ldap://www.apache.org/" ).toString(), referral.toString() );
        }
        catch ( LdapURLEncodingException e )
        {
            fail();
        }
    }


    /**
     * Test parsing of a response with an empty Referral
     */
    @Test
    public void testResponseWith1EmptyReferral()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_1_empty_referral.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        List<org.apache.directory.shared.ldap.util.LdapURL> referrals = ldapResult.getReferrals();

        assertEquals( 0, referrals.size() );
    }


    /**
     * Test parsing of a response with 2 Referral elements
     */
    @Test
    public void testResponseWith2Referrals()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_2_referrals.xml" ).openStream(),
                "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        List<org.apache.directory.shared.ldap.util.LdapURL> referrals = ldapResult.getReferrals();

        assertEquals( 2, referrals.size() );

        Object referral = referrals.get( 0 );

        try
        {
            assertEquals( new LdapURL( "ldap://www.apache.org/" ).toString(), referral.toString() );
        }
        catch ( LdapURLEncodingException e )
        {
            fail();
        }

        Object referral2 = referrals.get( 1 );

        try
        {
            assertEquals( new LdapURL( "ldap://www.apple.com/" ).toString(), referral2.toString() );
        }
        catch ( LdapURLEncodingException e )
        {
            fail();
        }
    }


    /**
     * Test parsing of a response with a Referral and an Error Message
     */
    @Test
    public void testResponseWith1ReferralAndAnErrorMessage()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput( AuthResponseTest.class.getResource( "response_with_1_referral_and_error_message.xml" )
                .openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        List<org.apache.directory.shared.ldap.util.LdapURL> referrals = ldapResult.getReferrals();

        assertEquals( 1, referrals.size() );

        Object referral = referrals.get( 0 );

        try
        {
            assertEquals( new LdapURL( "ldap://www.apache.org/" ).toString(), referral.toString() );
        }
        catch ( LdapURLEncodingException e )
        {
            fail();
        }
    }


    /**
     * Test parsing of a response with MatchedDN attribute
     */
    @Test
    public void testResponseWithMatchedDNAttribute()
    {
        Dsmlv2ResponseParser parser = null;
        try
        {
            parser = new Dsmlv2ResponseParser();

            parser.setInput(
                AuthResponseTest.class.getResource( "response_with_matchedDN_attribute.xml" ).openStream(), "UTF-8" );

            parser.parse();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        BindResponseCodec bindResponse = ( BindResponseCodec ) parser.getBatchResponse().getCurrentResponse();

        LdapResultCodec ldapResult = bindResponse.getLdapResult();

        assertEquals( "cn=Bob Rush,ou=Dev,dc=Example,dc=COM", ldapResult.getMatchedDN() );
    }


    /**
     * Test parsing of a response with wrong matched DN
     */
    @Test
    public void testResponseWithWrongMatchedDN()
    {
        testParsingFail( AuthResponseTest.class, "response_with_wrong_matchedDN_attribute.xml" );
    }


    /**
     * Test parsing of a response with wrong Descr attribute
     */
    @Test
    public void testResponseWithWrongDescr()
    {
        testParsingFail( AuthResponseTest.class, "response_with_wrong_matchedDN_attribute.xml" );
    }
}
