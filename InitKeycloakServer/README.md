This program set up a keycloak server for work with grantMaster project.

It creates two realms (grantmaster, exampleapp) ...

==Requirements==
* Java > 1.7
* gradle > 2.12
* existing keycloak server, testet with v1.9.4.Final

currently used keycloak-admin-client 1.9.4.Final


==How to call==
```bash
cd InitKeycloakServer
gradle clean buildRelease
build/release/InitForGrantMaster.sh -k http://localhost:8888/auth/ -u keycloak_admin -p k6ycloakAdmin -r master
```
