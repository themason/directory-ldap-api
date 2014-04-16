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
package org.apache.directory.api.ldap.extras.controls.ad;


import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.States;


/**
 * ASN.1 grammar constants of AdDirSync Control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum AdDirSyncStatesEnum implements States
{

    /** The END_STATE */
    END_STATE,

    /***/
    START_STATE,

    /** sequence start state */
    AD_DIR_SYNC_SEQUENCE_STATE,

    /** parentFirst value state */
    PARENT_FIRST_STATE,

    /** maxReturnLength value state */
    MAX_RETURN_LENGTH_STATE,

    /** cookie value state */
    COOKIE_STATE,

    /** terminal state */
    LAST_AD_DIR_SYNC_STATE;

    /**
     * Get the grammar name
     * 
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "AD_DIR_SYNC_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<AdDirSyncContainer> grammar )
    {
        if ( grammar instanceof AdDirSyncGrammar )
        {
            return "AD_DIR_SYNC_GRAMMAR";
        }

        return "UNKNOWN GRAMMAR";
    }


    /**
     * Get the string representing the state
     * 
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == END_STATE.ordinal() ) ? "AD_DIR_SYNC_GRAMMAR" : this.name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == END_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public AdDirSyncStatesEnum getStartState()
    {
        return START_STATE;
    }
}
