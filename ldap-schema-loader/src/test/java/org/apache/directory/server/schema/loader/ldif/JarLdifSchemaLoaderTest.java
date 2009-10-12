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
package org.apache.directory.server.schema.loader.ldif;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;
import org.junit.Test;


/**
 * Tests the LdifSchemaLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class JarLdifSchemaLoaderTest
{
    @Test
    public void testJarLdifSchemaLoader() throws Exception
    {
        Registries registries = new Registries();
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();
        loader.loadWithDependencies( loader.getSchema( "system" ), registries, true );
        
        assertTrue( registries.getAttributeTypeRegistry().contains( "cn" ) );
        assertFalse( registries.getAttributeTypeRegistry().contains( "m-aux" ) );
        
        loader.loadWithDependencies( loader.getSchema( "apachemeta" ), registries, true );

        assertTrue( registries.getAttributeTypeRegistry().contains( "m-aux" ) );
    }
}
