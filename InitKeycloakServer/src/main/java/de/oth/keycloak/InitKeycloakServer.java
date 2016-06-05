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
import de.oth.keycloak.impl.KeycloakAccess;
import de.oth.keycloak.json.AppConfig;
import de.oth.keycloak.json.AppRoleConfig;
import de.oth.keycloak.json.RealmConfig;
import de.oth.keycloak.json.RealmsConfig;
import de.oth.keycloak.json.UserConfig;
import de.oth.keycloak.json.UserGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.oth.keycloak.util.CheckParams;
import java.io.File;
import java.net.URL;
import java.util.List;
import javax.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 *
 * @author eiko
 */
public class InitKeycloakServer {

    public static void addRealm(Keycloak keycloak, RealmConfig realmConf) {
        String realmName = realmConf.getName();
        if (realmName == null) {
            log.error("realm name is null");
        }
        if (log.isInfoEnabled()) {
            log.info("realm '" + realmName + "' init realm roles ...");
        }
        RealmResource rRes = KeycloakAccess.getRealm(keycloak, realmName, true);
        KeycloakAccess.addMissedRealmRoles(rRes, realmConf.getRealmRoles());
        addApps(rRes, realmConf.getApps());
        addUserGroups(rRes, realmConf.getUserGroups());
        addUsers(rRes, realmConf.getUsers());
    }

    private static void addUserGroups(RealmResource rRes, List<UserGroupConfig> userGroupList) {
        if (userGroupList == null || userGroupList.isEmpty()) {
            return;
        }
        GroupsResource groupsResource = rRes.groups();
        for (UserGroupConfig userGroup : userGroupList) {
            String name = userGroup.getName();
            GroupResource groupRes = KeycloakAccess.getGroupFromRealm(rRes, name);
            if (groupRes == null) {
                groupRes = KeycloakAccess.addGroupToRealm(rRes, name);
            }
            List<String> groupRealmRoles = userGroup.getRealmRoles();
            if (groupRealmRoles != null) {
                KeycloakAccess.addMissedGroupRealmRoles(rRes, groupRes, groupRealmRoles);
            }
            List<AppRoleConfig> groupAppRoles = userGroup.getAppRoles();
            if (groupAppRoles != null) {
                for (AppRoleConfig appRole : groupAppRoles) {
                    String appName = appRole.getApp();
                    String roleName = appRole.getRole();
                    KeycloakAccess.addMissedGroupClientRole(rRes, groupRes, appName, roleName);
                }
            }
        }
    }

    private static void addApps(RealmResource rRes, List<AppConfig> appList) {
        if (appList == null || appList.isEmpty()) {
            log.info("no apps found");
            return;
        }
        for (AppConfig app : appList) {
            String name = app.getName();
            ClientResource cRes = KeycloakAccess.getClientFromRealm(rRes, name);
            if (cRes == null) {
                cRes = KeycloakAccess.addClientToRealm(rRes, name, app.getRedirectUrls(), app.getAppRoles());
                KeycloakAccess.addMissedRedirectUrls(rRes, cRes, app.getRedirectUrls());
                KeycloakAccess.addMissedClientRoles(rRes, cRes, app.getAppRoles());
            }
        }
    }

    private static void addUsers(RealmResource rRes, List<UserConfig> userList) {
        if (userList == null || userList.isEmpty()) {
            return;
        }
        for (UserConfig userConfig : userList) {
            String login = userConfig.getLogin();
            String firstName = userConfig.getFirstName();
            String lastName = userConfig.getLastName();
            UserResource userRes = KeycloakAccess.getUserFromRealm(rRes, firstName, lastName, login);
            boolean bAdd = false;
            if (userRes == null) {
                userRes = KeycloakAccess.addUserToRealm(rRes, firstName, lastName, login);
                bAdd = true;
            }
            String groupName = userConfig.getUserGroup();
            KeycloakAccess.setGroupForUser(rRes, userRes, userConfig.getUserGroup());
            if (bAdd) {
                KeycloakAccess.setPasswordForUser(rRes, userRes, userConfig.getPassword());
            }
        }
    }

    public static void main(String[] args) {
        CheckParams checkParams = CheckParams.create(args, System.out, InitKeycloakServer.class.getName());
        if (checkParams == null) {
            System.exit(1);
        }
        try {
            String server = checkParams.getServer();
            String realm = checkParams.getRealm();
            String user = checkParams.getUser();
            String pwd = checkParams.getPwd();
            String clientStr = checkParams.getClient();
            String secret = checkParams.getSecret();
            String initFileStr = checkParams.getInitFile();
            File initFile = new File(initFileStr);
            if (!initFile.isFile()) {
                URL url = InitKeycloakServer.class.getClassLoader().getResource(initFileStr);
                if (url != null) {
                    initFile = new File(url.getFile());
                    if (!initFile.isFile()) {
                        log.error("init file does not exist: " + initFile);
                        System.exit(1);
                    }
                } else {
                    log.error("init file does not exist: " + initFile);
                    System.exit(1);
                }
            }
            Keycloak keycloak = (secret == null) ? Keycloak.getInstance(server, realm, user, pwd, clientStr)
                    : Keycloak.getInstance(server, realm, user, pwd, clientStr, secret);

            ObjectMapper mapper = new ObjectMapper();
            RealmsConfig realmsConfig = mapper.readValue(initFile, RealmsConfig.class);

            if (realmsConfig != null) {
                List<RealmConfig> realmList = realmsConfig.getRealms();
                if (realmList == null || realmList.isEmpty()) {
                    log.error("no realms config found 1");
                    return;
                }
                for (RealmConfig realmConf : realmList) {
                    addRealm(keycloak, realmConf);
                }
            } else {
                log.error("no realms config found 2");
            }
        } catch (Exception e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(InitKeycloakServer.class);
}
