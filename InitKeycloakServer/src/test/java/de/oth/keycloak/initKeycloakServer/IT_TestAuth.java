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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oth.keycloak.InitKeycloakServer;
import de.oth.keycloak.TestRun;
import de.oth.keycloak.impl.KeycloakAccess;
import static de.oth.keycloak.initKeycloakServer.IT_Test.CLIENTSTR;
import static de.oth.keycloak.initKeycloakServer.IT_Test.PWD;
import static de.oth.keycloak.initKeycloakServer.IT_Test.REALM;
import static de.oth.keycloak.initKeycloakServer.IT_Test.SERVER;
import static de.oth.keycloak.initKeycloakServer.IT_Test.USER;
import static de.oth.keycloak.initKeycloakServer.IT_Test.initKeycloak;
import de.oth.keycloak.json.RealmConfig;
import de.oth.keycloak.json.RealmsConfig;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;

/**
 *
 * @author eiko
 */
public class IT_TestAuth {

    private final static String GM_REALM = "it_grantMaster";
    private final static String APP_REALM = "it_keycloakTest";

    private static Keycloak keycloak;

    @AfterClass
    public static void tearDownClass() throws IOException {
        RealmResource rRes = KeycloakAccess.getRealm(keycloak, GM_REALM, false);
        if (rRes != null) {
            rRes.remove();
        }
        rRes = KeycloakAccess.getRealm(keycloak, APP_REALM, false);
        if (rRes != null) {
            rRes.remove();
        }
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        keycloak = initKeycloak(SERVER, REALM, USER, PWD, CLIENTSTR);
        String initFileStr = "integration_test_config.json";
        File initFile = new File(initFileStr);
        if (!initFile.isFile()) {
            URL url = TestRun.class.getClassLoader().getResource(initFileStr);
            if (url != null) {
                initFile = new File(url.getFile());
                if (!initFile.isFile()) {
                    fail("test config is no file: " + initFileStr);
                }
            } else {
                fail("can't load test config: " + initFileStr);
            }
        }

        RealmResource rRes = KeycloakAccess.getRealm(keycloak, GM_REALM, false);
        if (rRes != null) {
            rRes.remove();
        }
        rRes = KeycloakAccess.getRealm(keycloak, APP_REALM, false);
        if (rRes != null) {
            rRes.remove();
        }
        ObjectMapper mapper = new ObjectMapper();
        RealmsConfig realmsConfig = mapper.readValue(initFile, RealmsConfig.class);

        if (realmsConfig != null) {
            List<RealmConfig> realmList = realmsConfig.getRealms();
            if (realmList == null || realmList.isEmpty()) {
                fail("realmList is empty: " + initFileStr);
            }
            for (RealmConfig realmConf : realmList) {
                InitKeycloakServer.addRealm(keycloak, realmConf);
            }
        } else {
            fail("no realm config found in: " + initFileStr);
        }
    }
    
    @Test
    public void testDummy() {
        System.out.println("here comes later the access tests");
    }

}
