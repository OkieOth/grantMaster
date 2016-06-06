/*
Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
See the NOTICE file distributed with this work for additional information regarding copyright ownership.  
The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
specific language governing permissions and limitations under the License.
 */
package de.oth.keycloak.initKeycloakServer;

import de.oth.keycloak.impl.KeycloakAccess;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;

/**
 *
 * @author eiko
 */
public class IT_Test {
    private final static String server = "http://localhost:8888/auth";
    private final static String realm = "master";
    private final static String user = "keycloak_admin";
    private final static String pwd = "k6ycloakAdmin";
    private final static String clientStr = "admin-cli";
    private static Keycloak keycloak;
    
    
    public IT_Test() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        keycloak = Keycloak.getInstance(server,realm,user,pwd,clientStr);
        // poll and wait for keycloak server is started
        RealmResource rRes = null;
        int count = 0;
        try {
            while(rRes==null && count < 100) {
                rRes = KeycloakAccess.getRealm(keycloak, realm,false);
                count++;
                Thread.sleep(1000);
            }
            System.out.println("wait count: "+count);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        
    }
    
    @Before
    public void setUp() {
    }


    @Test
    public void testGetNonExistingRealm() {
        String testRealm = "it_test1";
        RealmResource rRes = KeycloakAccess.getRealm(keycloak, testRealm,false);
        assertNull(rRes);
    }

    @Test
    public void testGetNonExistingRealmAndCreate() {
        String testRealm = "it_test2";
        RealmResource rRes = KeycloakAccess.getRealm(keycloak, testRealm,false);
        assertNull(rRes);
        try {
            rRes = KeycloakAccess.getRealm(keycloak, testRealm,true);
            assertNotNull(rRes);
        }
        finally {
            if (rRes!=null) {
                rRes.remove();            
                rRes = KeycloakAccess.getRealm(keycloak, testRealm,false);
                assertNull(rRes);
            }
        }
    }

    @Test
    public void testRealmRoles() {
        String testRealm = "it_test3";
        RealmResource rRes = KeycloakAccess.getRealm(keycloak, testRealm,true);
        try {
            assertNotNull(rRes);
            KeycloakAccess.addMissedRealmRoles(rRes, null);
            List<String> aktRealmRoles = KeycloakAccess.getRealmRoleNames(rRes);
            assertEquals(0,aktRealmRoles.size());
            List<String> realmRoles = new ArrayList();
            KeycloakAccess.addMissedRealmRoles(rRes, realmRoles);
            aktRealmRoles = KeycloakAccess.getRealmRoleNames(rRes);
            assertEquals(0,aktRealmRoles.size());
            
            realmRoles.add("testRole1");
            KeycloakAccess.addMissedRealmRoles(rRes, realmRoles);
            aktRealmRoles = KeycloakAccess.getRealmRoleNames(rRes);
            compareStringLists(realmRoles,aktRealmRoles);
            // TODO
        }
        finally {
            if (rRes!=null) {
                rRes.remove();            
                rRes = KeycloakAccess.getRealm(keycloak, testRealm,false);
                assertNull(rRes);
            }
        }
    }

    public void compareStringLists(List<String> list1,List<String> list2) {
        assertNotNull(list1);
        assertNotNull(list2);
        assertEquals(list1.size(),list2.size());
        for (String s:list1) {
            assertTrue(list2.contains(s));
        }
    }

}
