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
package de.oth.keycloak.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
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
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper class with a lot of static helper functions ... good for
 * testing
 *
 * @author eiko
 */
public class KeycloakAccess {

    /**
     * a helper function that currently queries the realm. If it doesn't exist
     * it catches a exception ... all other ideas don't work :-/
     *
     * @param rr
     * @return
     */
    public static boolean doesRealmExist(RealmResource rr) {
        try {
            rr.clients().findAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static RealmResource getRealm(Keycloak keycloak, String realmName, boolean createNew) {
        RealmResource rRes = keycloak.realm(realmName);
        if (!KeycloakAccess.doesRealmExist(rRes)) { // TODO don't work ... need to be replaced by a checkFunction!
            if (!createNew) {
                return null;
            }
            log.info("create a new realm: " + realmName);
            RealmRepresentation rr = new RealmRepresentation();
            rr.setId(realmName);
            rr.setRealm(realmName);
            rr.setEnabled(Boolean.TRUE);
            keycloak.realms().create(rr);
            return keycloak.realm(realmName);
        }
        return rRes;
    }

    public static void addMissedRealmRoles(RealmResource rRes, List<String> roleList) {
        if (roleList == null || roleList.isEmpty()) {
            return;
        }
        RolesResource rolesResource = rRes.roles();
        List<RoleRepresentation> listRoleReps = rolesResource.list();
        for (String roleName : roleList) {
            boolean bFound = false;
            for (RoleRepresentation roleRep : listRoleReps) {
                if (roleName.equals(roleRep.getName())) {
                    bFound = true;
                    break;
                }
            }
            if (!bFound) {
                RoleRepresentation rr = new RoleRepresentation();
                rr.setName(roleName);
                rRes.roles().create(rr);
            }
        }
    }

    public static GroupResource getGroupFromRealm(RealmResource rRes, String groupName) {
        GroupsResource groupsRes = rRes.groups();
        List<GroupRepresentation> groupsList = groupsRes.groups();
        for (GroupRepresentation group : groupsList) {
            if (groupName.equals(group.getName())) {
                return rRes.groups().group(group.getId());
            }
        }
        return null;
    }

    public static GroupResource addGroupToRealm(RealmResource rRes, String groupName) {
        GroupRepresentation gr = new GroupRepresentation();
        gr.setName(groupName);
        Response response = rRes.groups().add(gr);
        String groupId = getCreatedId(response);
        return rRes.groups().group(groupId);
    }

    public static void addMissedGroupRealmRoles(RealmResource rRes, GroupResource groupResource, List<String> realmRoleList) {
        boolean changed = false;
        RoleScopeResource roleScopeResource = groupResource.roles().realmLevel();
        List<RoleRepresentation> rRepList = roleScopeResource.listAll();
        for (String realmRole : realmRoleList) {
            boolean bFound2 = false;
            for (RoleRepresentation rRep : rRepList) {
                if (realmRole.equals(rRep.getName())) {
                    bFound2 = true;
                    break;
                }
            }
            if (!bFound2) {
                changed = true;
                rRepList.add(rRes.roles().get(realmRole).toRepresentation());
            }
        }
        if (changed) {
            roleScopeResource.add(rRepList);
        }
    }

    public static void addMissedGroupClientRole(RealmResource rRes, GroupResource groupResource, String clientName, String roleName) {
        RoleScopeResource roleScopeResource = groupResource.roles().clientLevel(clientName);
        List<RoleRepresentation> rRepList = null;
        try {
            rRepList = roleScopeResource.listAll();
        }
        catch (javax.ws.rs.NotFoundException e) {
            rRepList = new ArrayList();
        }
        boolean bFound = false;
        for (RoleRepresentation rRep : rRepList) {
            if (roleName.equals(rRep.getName())) {
                bFound = true;
                break;
            }
        }
        if (!bFound) {
            List<ClientRepresentation> clientList = rRes.clients().findAll();
            for (ClientRepresentation client:clientList) {
                if (clientName.equals(client.getName())) {
                    ClientResource cRes = rRes.clients().get(client.getId());
                    RolesResource rolesRes = cRes.roles();                                
                    RoleResource roleRes = rolesRes.get(roleName);
                    List<RoleRepresentation> l2 = new ArrayList();
                    l2.add(roleRes.toRepresentation());                                                
                    RoleMappingResource roleMappingResource = groupResource.roles();
                    roleMappingResource.clientLevel(client.getId()).add(l2);
                    break;
                }
            }
            List<RoleRepresentation> realmRoles = rRes.roles().list();
            for (RoleRepresentation realmRole:realmRoles) {
                if (roleName.equals(realmRole.getName())) {
                    rRepList.add(rRes.roles().get(roleName).toRepresentation());
                    roleScopeResource.add(rRepList);
                    break;
                }
            }
        }
    }

    public static ClientResource getClientFromRealm(RealmResource rRes, String clientName) {
        ClientsResource clientsResource = rRes.clients();
        List<ClientRepresentation> clientList = clientsResource.findAll();
        ClientRepresentation cRep = null;
        for (ClientRepresentation aktCRep : clientList) {
            String aktName = aktCRep.getName();
            if (aktName != null && aktName.equals(clientName)) {
                cRep = aktCRep;
                return rRes.clients().get(cRep.getId());
            }
        }
        return null;
    }

    public static UserResource getUserFromRealm(RealmResource rRes, String firstName, String lastName, String login) {
        UsersResource usersRes = rRes.users();
        List<UserRepresentation> userList = usersRes.search(login, null, null);
        if (userList != null && (!userList.isEmpty())) {
            String userId = userList.get(0).getId();
            return rRes.users().get(userId);
        } else {
            return null;
        }
    }

    public static UserResource addUserToRealm(RealmResource rRes, String firstName, String lastName, String login) {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setLastName(lastName);
        userRep.setFirstName(firstName);
        // if Email is used then the value needs to be unique :-/ v.1.9.4.Final
//                userRep.setEmail(userConfig.getEmail());
        userRep.setEnabled(true);
        userRep.setUsername(login);
        Response response = rRes.users().create(userRep);
        String userId = getCreatedId(response);
        return rRes.users().get(userId);
    }

    public static void setGroupForUser(RealmResource rRes, UserResource userRes, String groupName) {
        List<GroupRepresentation> realmGroups = rRes.groups().groups();
        for (GroupRepresentation realmGroup : realmGroups) {
            if (groupName.equals(realmGroup.getName())) {
                rRes.users().get(userRes.toRepresentation().getId()).joinGroup(realmGroup.getId());
                break;
            }
        }
    }

    public static void setPasswordForUser(RealmResource rRes, UserResource userRes, String password) {
        CredentialRepresentation credRep = new CredentialRepresentation();
        credRep.setValue(password);
        credRep.setType(CredentialRepresentation.PASSWORD);
        credRep.setTemporary(Boolean.FALSE);
        rRes.users().get(userRes.toRepresentation().getId()).resetPassword(credRep);
    }

    public static ClientResource addClientToRealm(RealmResource rRes, String clientName,
            List<String> redirectUrls, List<String> clientRoles) {
        ClientRepresentation cRep = new ClientRepresentation();
        cRep.setEnabled(Boolean.TRUE);
        cRep.setClientId(clientName);
        cRep.setName(clientName);
        if (redirectUrls != null && (!redirectUrls.isEmpty())) {
            cRep.setRedirectUris(redirectUrls);
        }
        if (clientRoles != null && (!clientRoles.isEmpty())) {
            cRep.setDefaultRoles(list2array(clientRoles));
        }
        Response response = rRes.clients().create(cRep);
        String clientId = getCreatedId(response);
        return rRes.clients().get(clientId);
    }

    public static void addMissedRedirectUrls(RealmResource rRes, ClientResource clientResource, List<String> neededRedirectUriList) {
        if (neededRedirectUriList != null && (!neededRedirectUriList.isEmpty())) {
            ClientRepresentation cRep = clientResource.toRepresentation();
            List<String> redirectUriList = cRep.getRedirectUris();
            boolean redirectsChanged = false;
            for (String s : neededRedirectUriList) {
                if (!redirectUriList.contains(s)) {
                    redirectUriList.add(s);
                    redirectsChanged = true;
                }
            }
            if (redirectsChanged) {
                cRep.setRedirectUris(redirectUriList);
                rRes.clients().get(cRep.getId()).update(cRep);
            }
        }
    }

    public static void addMissedClientRoles(RealmResource rRes, ClientResource clientResource, List<String> clientRoles) {
        if (clientRoles != null && (!clientRoles.isEmpty())) {
            ClientRepresentation cRep = clientResource.toRepresentation();
            String[] defaultRoles = cRep.getDefaultRoles();
            for (String s : defaultRoles) {
                if (clientRoles.contains(s)) {
                    clientRoles.remove(s);
                }
            }
            if (!clientRoles.isEmpty()) {
                int l = defaultRoles.length + clientRoles.size();
                String[] appRoles = new String[l];
                int count = 0;
                for (int i = 0; i < defaultRoles.length; i++) {
                    appRoles[i] = defaultRoles[i];
                    count++;
                }
                for (String s : clientRoles) {
                    appRoles[count] = s;
                    count++;
                }
                cRep.setDefaultRoles(appRoles);
                rRes.clients().get(cRep.getId()).update(cRep);
            }
        }
    }

    public static String[] list2array(List<String> list) {
        int s = list.size();
        String[] ret = new String[s];
        for (int i = 0; i < s; i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }

    /**
     * copy from original keycloak source
     * keycloak/testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite/admin/ApiUtil.java
     *
     * @param response
     * @return
     */
    public static String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            Response.StatusType statusInfo = response.getStatusInfo();
            throw new RuntimeException("Create method returned status "
                    + statusInfo.getReasonPhrase() + " (Code: " + statusInfo.getStatusCode() + "); expected status: Created (201)");
        }
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static final Logger log = LoggerFactory.getLogger(KeycloakAccess.class);

}
