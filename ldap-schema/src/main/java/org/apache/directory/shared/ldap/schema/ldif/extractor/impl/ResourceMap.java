/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.schema.ldif.extractor.impl;


import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Lists LDIF resources available from the classpath.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResourceMap
{
    /** the system property which can be used to load schema from a user specified
     *  resource like a absolute path to a directory or jar file.
     *  This is useful to start embedded DirectoryService in a servlet container environment
     *  
     *  usage: -Dschema.resource.location=/tmp/schema 
     *                OR
     *         -Dschema.resource.location=/tmp/shared-ldap-schema-0.9.18.jar
     *  */
    private static final String SCHEMA_RESOURCE_LOCATION = "schema.resource.location";
    
    private static final Logger LOG = LoggerFactory.getLogger( ResourceMap.class );
    
   /**
    * For all elements of java.class.path OR from the resource name set in the 
    * system property 'schema.resource.location' get a Map of resources
    * Pattern pattern = Pattern.compile(".*").  
    * The keys represent resource names and the boolean parameter indicates
    * whether or not the resource is in a Jar file.
    * 
    * @param pattern the pattern to match
    * @return the resources with markers - true if resource is in Jar
    */
    public static Map<String,Boolean> getResources( Pattern pattern )
    {
        HashMap<String,Boolean> retval = new HashMap<String,Boolean>();
    
        String schemaResourceLoc = System.getProperty( SCHEMA_RESOURCE_LOCATION, "" );
        
        if( schemaResourceLoc.trim().length() > 0 )
        {
            LOG.debug( "loading from the user provider schema resource {}", schemaResourceLoc );
            
            File file = new File( schemaResourceLoc );
            if( file.exists() )
            {
                getResources( retval, schemaResourceLoc, pattern );
            }
            else
            {
                LOG.error( "unable to load schema from the given resource value {}", schemaResourceLoc );
            }
        }
        else
        {
            String classPath = System.getProperty( "java.class.path", "." );
            String[] classPathElements = classPath.split( File.pathSeparator );
            
            for ( String element : classPathElements )
            {
                getResources( retval, element, pattern );
            }
        }
        
        return retval;
    }


    private static void getResources( HashMap<String,Boolean> map, 
        String element, Pattern pattern )
    {
        File file = new File( element );
        if ( !file.exists() )
        {
            // this may happen if the class path contains an element that doesn't exist
            return;
        }

        if ( file.isDirectory() )
        {
            getResourcesFromDirectory( map, file, pattern );
        }
        else
        {
            getResourcesFromJarFile( map, file, pattern );
        }
    }


    private static void getResourcesFromJarFile( HashMap<String,Boolean> map, 
        File file, Pattern pattern )
    {
        ZipFile zf;
        
        try
        {
            zf = new ZipFile( file );
        }
        catch ( ZipException e )
        {
            throw new Error( e );
        }
        catch ( IOException e )
        {
            throw new Error( e );
        }
        
        Enumeration<? extends ZipEntry> e = zf.entries();
        
        while ( e.hasMoreElements() )
        {
            ZipEntry ze = e.nextElement();
            String fileName = ze.getName();
            boolean accept = pattern.matcher( fileName ).matches();
        
            if ( accept )
            {
                map.put( fileName, Boolean.TRUE );
            }
        }
        try
        {
            zf.close();
        }
        catch ( IOException e1 )
        {
            throw new Error( e1 );
        }
    }


    private static void getResourcesFromDirectory( 
        HashMap<String,Boolean> map, File directory, Pattern pattern )
    {
        File[] fileList = directory.listFiles();
        
        for ( File file : fileList )
        {
            if ( file.isDirectory() )
            {
                getResourcesFromDirectory( map, file, pattern );
            }
            else
            {
                try
                {
                    String fileName = file.getCanonicalPath();
                    boolean accept = pattern.matcher( fileName ).matches();
        
                    if ( accept )
                    {
                        map.put( fileName, Boolean.FALSE );
                    }
                }
                catch ( IOException e )
                {
                    throw new Error( e );
                }
            }
        }
    }
}