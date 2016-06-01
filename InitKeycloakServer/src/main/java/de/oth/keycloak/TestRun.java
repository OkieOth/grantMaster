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
package de.oth.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.oth.keycloak.json.AppConfig;
import de.oth.keycloak.json.RealmConfig;
import de.oth.keycloak.json.RealmsConfig;
import de.oth.keycloak.json.UserConfig;
import de.oth.keycloak.json.UserGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.oth.keycloak.util.CheckParams;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;


/**
 *
 * @author eiko
 */
public class TestRun {    
    public static void main(String[] args) {
        try {
            String server = "http://localhost:8888/auth";
            String realm = "master";
            String user = "keycloak_admin";
            String pwd = "k6ycloakAdmin";
            String clientStr = "admin-cli";
            String secret = null;
            String initFileStr = "conf/test_init1.json";
            File initFile = new File(initFileStr);
            if (!initFile.isFile()) {
                URL url = TestRun.class.getClassLoader().getResource(initFileStr);
                if (url!=null) {
                    initFile = new File (url.getFile());
                    if (!initFile.isFile()) {
                        log.error("init file does not exist: "+initFile);
                        System.exit(1);                        
                    }
                }
                else {
                    log.error("init file does not exist: "+initFile);
                    System.exit(1);
                }
            }
            Keycloak keycloak = (secret==null) ? Keycloak.getInstance(server,realm,user,pwd,clientStr) :
                Keycloak.getInstance(server,realm,user,pwd,clientStr,secret);
    

            
            ObjectMapper mapper = new ObjectMapper();
            RealmsConfig realmsConfig = mapper.readValue(initFile, RealmsConfig.class);

            if (realmsConfig!=null) {
                List<RealmConfig> realmList = realmsConfig.getRealms();
                if (realmList==null || realmList.isEmpty()) {
                    log.error("no realms config found 1");
                    return;
                }
                for (RealmConfig realmConf:realmList) {
                    InitKeycloakServer.addRealm(keycloak,realmConf);
                }
            }
            else
                log.error("no realms config found 2");
        } catch (Exception e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
        
    
    private static final Logger log = LoggerFactory.getLogger(TestRun.class);
}
