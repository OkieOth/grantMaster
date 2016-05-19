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
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;


/**
 *
 * @author eiko
 */
public class InitKeycloakServer {
    private static void addRealm(Keycloak keycloak,RealmConfig realmConf) {
        RealmsResource realmsResource = keycloak.realms();
        String realmName = realmConf.getName();
        if (realmName==null) {
            log.error("realm name is null");
        }
        RealmResource rRes = realmsResource.realm(realmName);
        if ( rRes == null) {
            RealmRepresentation rr = new RealmRepresentation();
            rr.setId(realmName);
            rr.setRealm(realmName);
            rr.setEnabled(Boolean.TRUE);
            keycloak.realms().create(rr);            
            rRes = realmsResource.realm(realmName);
            if (rRes==null) {
                log.error ("can't retrieve created realm: "+realmName);
                return;
            }
        }
        else
            log.info("realm '"+realmName+"' already exists");
        if (log.isInfoEnabled()) {
            log.info("realm '"+realmName+"' init realm roles ...");
        }
        addRealmRoles(keycloak,rRes,realmConf.getRealmRoles());
        if (log.isInfoEnabled()) {
            log.info("realm '"+realmName+"' init apps ...");
        }
        addApps(keycloak,rRes,realmConf.getApps());
        if (log.isInfoEnabled()) {
            log.info("realm '"+realmName+"' init users ...");
        }
        addUserGroups(keycloak,rRes,realmConf.getUserGroups());
        if (log.isInfoEnabled()) {
            log.info("realm '"+realmName+"' init users ...");
        }
        addUsers(keycloak,rRes,realmConf.getUsers());
    }
    
    private static void addRealmRoles(Keycloak keycloak,RealmResource rRes,List<String> roleList) {
        if (roleList==null || roleList.isEmpty()) {
            log.info("no realm roles found");
            return;
        }        
        // TODO
    }

    private static void addUserGroups(Keycloak keycloak,RealmResource rRes,List<UserGroupConfig> roleList) {
        if (roleList==null || roleList.isEmpty()) {
            log.info("no realm roles found");
            return;
        }        
        // TODO
    }
    
    private static String[] list2array(List<String> list) {
        int s = list.size();
        String[] ret = new String[s];
        for (int i=0;i<s;i++)
            ret[i] = list.get(i);
        return ret; 
    }

    private static void addApps(Keycloak keycloak,RealmResource rRes,List<AppConfig> appList) {
        if (appList==null || appList.isEmpty()) {
            log.info("no apps found");
            return;
        }
        for (AppConfig app:appList) {
            String name = app.getName();
            List<ClientRepresentation> clientList = rRes.clients().findByClientId(name);
            ClientRepresentation cRep = clientList!=null && (!clientList.isEmpty()) ? clientList.get(0) : null;
            if (cRep==null) {
                cRep = new ClientRepresentation();
                cRep.setEnabled(Boolean.TRUE);
                cRep.setClientId(name);
                cRep.setRedirectUris(app.getRedirectUrls());
                List<String> appRoles = app.getAppRoles();
                if (appRoles!=null && (!appRoles.isEmpty()))
                    cRep.setDefaultRoles(list2array(appRoles));
                rRes.clients().create(cRep);
                
            }
            else {
                if (log.isInfoEnabled()) {
                    log.info("client '"+name+"' already exists");
                }
                List<String> neededRedirectUriList = app.getRedirectUrls();
                boolean change = false;
                if (neededRedirectUriList!=null &&(!neededRedirectUriList.isEmpty())) {
                    List<String> redirectUriList = cRep.getRedirectUris();
                    boolean redirectsChanged=false;
                    for (String s:neededRedirectUriList) {
                        if (!redirectUriList.contains(s)) {
                            redirectUriList.add(s);
                            redirectsChanged = true;
                        }
                        else {
                            if (log.isInfoEnabled()) {
                                log.info("client '"+name+"' redirect uri already exists:"+s);
                            }
                        }
                    }
                    if (redirectsChanged) {
                        cRep.setRedirectUris(redirectUriList);
                        change = true;
                    }
                }
                List<String> appRoleList = app.getAppRoles();
                if (appRoleList!=null && (!appRoleList.isEmpty())) {
                    String[] defaultRoles = cRep.getDefaultRoles();
                    for (String s:defaultRoles) {
                        if (appRoleList.contains(s)) {
                            if (log.isInfoEnabled()) {
                                appRoleList.remove(s);
                                log.info("client '"+name+"' role already exists:"+s);
                            }                            
                        }
                    }
                    if (!appRoleList.isEmpty()) {
                        int l = defaultRoles.length + appRoleList.size();
                        String[] appRoles = new String[l];
                        int count=0;
                        for (int i=0;i<defaultRoles.length;i++)  {
                            appRoles[i] = defaultRoles[i];
                            count++;
                        }
                        for (String s:appRoleList) {
                            appRoles[count] = s;
                            count++;
                        }
                        cRep.setDefaultRoles(appRoles);
                        change = true;
                    }
                }
            }
        }
    }

    private static void addUsers(Keycloak keycloak,RealmResource rRes,List<UserConfig> userList) {
        if (userList==null || userList.isEmpty()) {
            log.info("no user found");
            return;
        }
        // TODO
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
                log.error("init file does not exist: "+initFile);
                System.exit(1);
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
                    addRealm(keycloak,realmConf);
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
        
    
    private static final Logger log = LoggerFactory.getLogger(InitKeycloakServer.class);
}
