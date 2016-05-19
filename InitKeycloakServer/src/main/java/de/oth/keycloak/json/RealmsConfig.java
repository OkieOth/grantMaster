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
public class RealmsConfig {
    private List<RealmConfig> realms;

    public List<RealmConfig> getRealms() {
        return realms;
    }

    public void setRealms(List<RealmConfig> realms) {
        this.realms = realms;
    }    
}
