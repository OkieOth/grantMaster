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
import de.oth.keycloak.json.AppRoleConfig;
import de.oth.keycloak.json.RealmConfig;
import de.oth.keycloak.json.RealmsConfig;
import de.oth.keycloak.json.UserConfig;
import de.oth.keycloak.json.UserGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.oth.keycloak.util.CheckParams;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
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
public class InitKeycloakServer {

    /**
     * a helper function that currently queries the realm. If it doesn't exist
     * it catches a exception ... all other ideas don't work :-/
     * @param rr
     * @return 
     */
    private static boolean doesRealmExist(RealmResource rr) {
        try {
            rr.clients().findAll();
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }
    

    /**
     * copy from original keycloak source 
     * keycloak/testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite/admin/ApiUtil.java
     * @param response
     * @return 
     */
    public static String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Status.CREATED)) {
            StatusType statusInfo = response.getStatusInfo();
            throw new RuntimeException("Create method returned status " +
                    statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201)");
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static void addRealm(Keycloak keycloak,RealmConfig realmConf) {
        String realmName = realmConf.getName();
        if (realmName==null) {
            log.error("realm name is null");
        }
        RealmResource rRes = keycloak.realm(realmName);
        if ( !doesRealmExist(rRes) ) { // TODO don't work ... need to be replaced by a checkFunction!
            RealmRepresentation rr = new RealmRepresentation();
            rr.setId(realmName);
            rr.setRealm(realmName);
            rr.setEnabled(Boolean.TRUE);
            keycloak.realms().create(rr);            
            rRes = keycloak.realm(realmName);
            if (rRes==null) {
                log.error ("can't retrieve created realm: "+realmName);
                return;
            }
        }
        else {
            log.info("realm '"+realmName+"' already exists");
        }
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
        RolesResource rolesResource = rRes.roles();
        List<RoleRepresentation> listRoleReps = rolesResource.list();
        for (String role:roleList) {
            boolean bFound = false;
            for (RoleRepresentation roleRep:listRoleReps) {
                if (role.equals(roleRep.getName())) {
                    bFound = true;
                    break;                    
                }
            }
            if (!bFound) {
                addRealmRole(rRes,role);
            }
        }
    }

    /**
     * TODO maybe a description make sense
     * @param rRes
     * @param roleName 
     */
    private static void addRealmRole(RealmResource rRes,String roleName) {        
        if (roleName==null) {
            return;
        }        
        RoleRepresentation rr = new RoleRepresentation();
        rr.setName(roleName);
        rRes.roles().create(rr);
    }

    private static void addUserGroups(Keycloak keycloak,RealmResource rRes,List<UserGroupConfig> userGroupList) {
        if (userGroupList==null || userGroupList.isEmpty()) {
            log.info("no realm user groups found");
            return;
        }        
        for (UserGroupConfig userGroup:userGroupList) {
            String name = userGroup.getName();
            boolean bFound = false;
            GroupsResource groupsResource = rRes.groups();
            List<GroupRepresentation> groupsList = groupsResource.groups();
            for (GroupRepresentation groupRep:groupsList) {
                if (name.equals(groupRep.getName())) {
                    String groupId = groupRep.getId();
                    // group found
                    // check for realm roles
                    List<String> realmRoleList = userGroup.getRealmRoles();
                    GroupResource groupResource = groupsResource.group(groupId);
                    for (String realmRole:realmRoleList) {
                        RoleScopeResource roleScopeResource = groupResource.roles().realmLevel();
                        List<RoleRepresentation> rRepList = roleScopeResource.listAll();
                        boolean bFound2=false;
                        for (RoleRepresentation rRep:rRepList) {
                            if (realmRole.equals(rRep.getName())) {
                                bFound2=true;
                                break;
                            }
                        }
                        if (!bFound2) {
                            rRepList.add(rRes.roles().get(realmRole).toRepresentation());
                            roleScopeResource.add(rRepList);
                        }
                    }
                    // check for client roles
                    List<AppRoleConfig> appRoleList = userGroup.getAppRoles();
                    ClientsResource clientsRes = rRes.clients();
                    List<ClientRepresentation> cRepList = clientsRes.findAll();
                    for (AppRoleConfig appRole:appRoleList) {
                        String appName = appRole.getApp();
                        String roleName = appRole.getRole();
                        for (ClientRepresentation cRep:cRepList) {
                            if (appName.equals(cRep.getName())) {                                
                                // test if there is already the role assigned to the group
                                String clientId = cRep.getId();
                                RoleScopeResource roleScopeResource = groupResource.roles().clientLevel(clientId);
                                List<RoleRepresentation> rRepList = roleScopeResource.listAll();
                                boolean bFound2=false;
                                for (RoleRepresentation rRep:rRepList) {
                                    if (roleName.equals(rRep.getName())) {
                                        bFound2=true;
                                        break;
                                    }
                                }
                                if (!bFound2) {
                                    ClientResource cRes = clientsRes.get(clientId);
                                    RolesResource rolesRes = cRes.roles();                                
                                    RoleResource roleRes = rolesRes.get(roleName);
                                    List<RoleRepresentation> l2 = new ArrayList();
                                    l2.add(roleRes.toRepresentation());                            
                                    RoleMappingResource roleMappingResource = rRes.groups().group(groupId).roles();
                                    roleMappingResource.clientLevel(cRep.getId()).add(l2);
                                }
                                break;
                            }
                        }
                    }
                    
                    bFound = true;
                    break;
                }                
            }
            if (!bFound) {
                // add a new user Group
                GroupRepresentation gr = new GroupRepresentation();
                gr.setName(name);
                Response response = rRes.groups().add(gr);         
                String groupId = getCreatedId(response);
                List<String> realmRoleList = userGroup.getRealmRoles();
                List<RoleRepresentation> l = new ArrayList();
                for (String role:realmRoleList) {
                    l.add(rRes.roles().get(role).toRepresentation());
                }
                if (!l.isEmpty()) {
                    RoleMappingResource roleMappingResource = rRes.groups().group(groupId).roles();
                    roleMappingResource.realmLevel().add(l);

                }
                
                List<AppRoleConfig> appRoleList = userGroup.getAppRoles();
                ClientsResource clientsRes = rRes.clients();
                List<ClientRepresentation> cRepList = clientsRes.findAll();
                for (AppRoleConfig appRole:appRoleList) {
                    // ClientRole ermitteln
                    String appName = appRole.getApp();
                    String roleName = appRole.getRole();
                    for (ClientRepresentation cRep:cRepList) {
                        if (appName.equals(cRep.getName())) {
                            ClientResource cRes = clientsRes.get(cRep.getId());
                            RolesResource rolesRes = cRes.roles();
                            RoleResource roleRes = rolesRes.get(roleName);
                            List<RoleRepresentation> l2 = new ArrayList();
                            l2.add(roleRes.toRepresentation());                            
                            RoleMappingResource roleMappingResource = rRes.groups().group(groupId).roles();
                            roleMappingResource.clientLevel(cRep.getId()).add(l2);
                            break;
                        }
                    }
                }
            }
        }
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
            
            ClientsResource clientsResource = rRes.clients();
            List<ClientRepresentation> clientList = clientsResource.findAll();
            ClientRepresentation cRep = null;
            for (ClientRepresentation aktCRep:clientList) {
                String aktName = aktCRep.getName();
                if (aktName != null && aktName.equals(name)) {
                    cRep = aktCRep;
                    break;
                }
            }
            if (cRep==null) {
                cRep = new ClientRepresentation();
                cRep.setEnabled(Boolean.TRUE);
                cRep.setClientId(name);
                cRep.setName(name);
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
                if (change) {
                    rRes.clients().get(cRep.getId()).update(cRep);
                }
            }
        }
    }

    private static void addUsers(Keycloak keycloak,RealmResource rRes,List<UserConfig> userList) {
        if (userList==null || userList.isEmpty()) {
            log.info("no user found");
            return;
        }
        UsersResource usersResource = rRes.users();
        List<UserRepresentation> aktUserList = usersResource.search(null,null,null);
        for (UserConfig userConfig:userList) {            
            String login = userConfig.getLogin();
            String groupName = userConfig.getUserGroup();
            boolean bFound = false;
            for (UserRepresentation userRep:aktUserList) {
                if (login.equals(userRep.getUsername())) {
                    bFound = true;
                    // check assigned user group
                    boolean groupFound=false;
                    List<String> aktGroups = userRep.getGroups();
                    if (aktGroups==null || (!aktGroups.contains(groupName))) {
                        List<GroupRepresentation> groupList = rRes.groups().groups();
                        for (GroupRepresentation group:groupList) {
                            if (group.getName().equals(groupName)) {
                                rRes.users().get(userRep.getId()).joinGroup(group.getId());
                                break;
                            }
                        }                        
                    }
                    break;
                }
            }
            if (!bFound) {
                UserRepresentation userRep = new UserRepresentation();
                userRep.setLastName(userConfig.getLastName());
                userRep.setFirstName(userConfig.getFirstName());
                // if Email is used then the value needs to be unique :-/ v.1.9.4.Final
//                userRep.setEmail(userConfig.getEmail());
                userRep.setEnabled(true);
                userRep.setUsername(userConfig.getLogin());
                Response response = rRes.users().create(userRep);
                String userId = getCreatedId(response);
                CredentialRepresentation credRep = new CredentialRepresentation();
                credRep.setValue(userConfig.getPassword());
                credRep.setType(CredentialRepresentation.PASSWORD);
                credRep.setTemporary(Boolean.FALSE);
                rRes.users().get(userId).resetPassword(credRep);
                List<GroupRepresentation> groupList = rRes.groups().groups();
                for (GroupRepresentation group:groupList) {
                    if (group.getName().equals(groupName)) {
                        rRes.users().get(userId).joinGroup(group.getId());
                        break;
                    }
                }
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
