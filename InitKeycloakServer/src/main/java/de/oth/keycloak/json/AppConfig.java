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
public class AppConfig {
    private String name;
    private List<String> redirectUrls;
    private List<String> appRoles;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRedirectUrls() {
        return redirectUrls;
    }

    public void setRedirectUrls(List<String> redirectUrls) {
        this.redirectUrls = redirectUrls;
    }

    public List<String> getAppRoles() {
        return appRoles;
    }

    public void setAppRoles(List<String> appRoles) {
        this.appRoles = appRoles;
    }  
}
