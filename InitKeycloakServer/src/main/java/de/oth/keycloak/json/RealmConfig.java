/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.oth.keycloak.json;

import java.util.List;

/**
 *
 * @author eiko
 */
public class RealmConfig {
    private String name;
    private List<String> realmRoles;
    private List<AppConfig> apps;
    private List<UserGroupConfig> userGroups;
    private List<UserConfig> users;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public List<AppConfig> getApps() {
        return apps;
    }

    public void setApps(List<AppConfig> apps) {
        this.apps = apps;
    }

    public List<UserConfig> getUsers() {
        return users;
    }

    public void setUsers(List<UserConfig> users) {
        this.users = users;
    }

    public List<UserGroupConfig> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<UserGroupConfig> userGroups) {
        this.userGroups = userGroups;
    }

    
}
