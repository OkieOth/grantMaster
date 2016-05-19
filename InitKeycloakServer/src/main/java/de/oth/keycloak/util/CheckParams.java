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
package de.oth.keycloak.util;

import java.io.PrintStream;
import java.util.Comparator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import de.oth.keycloak.InitKeycloakServer;

/**
 *
 * @author eiko
 */
public class CheckParams {
    public final static String CONF_HELP = "h";
    public final static String CONF_SERVER = "k";
    public final static String CONF_REALM = "r";
    public final static String CONF_USER = "u";
    public final static String CONF_PWD = "p";
    public final static String CONF_CLIENT = "c";
    public final static String CONF_SECRET = "s";
    public final static String CONF_INITFILE = "i";

    
    private String server;
    private String realm;
    private String user;
    private String pwd;
    private String client;
    private String secret;
    private String initFile;
    
    private CheckParams(CommandLine cmdline) {
        server = cmdline.getOptionValue(CONF_SERVER);
        realm = cmdline.hasOption(CONF_REALM) ? cmdline.getOptionValue(CONF_REALM) : "master";
        user = cmdline.getOptionValue(CONF_USER);
        pwd = cmdline.getOptionValue(CONF_PWD);
        client = cmdline.hasOption(CONF_CLIENT) ? cmdline.getOptionValue(CONF_CLIENT) : "admin-cli";
        secret = cmdline.hasOption(CONF_SECRET) ? cmdline.getOptionValue(CONF_SECRET) : null;
        initFile = cmdline.getOptionValue(CONF_INITFILE);
    }

    private static CommandLine checkCommandline(String[] args, PrintStream printStream) {
        Options options = getCommandLineOptions();
        CommandLineParser parser = new GnuParser();
        try {
            CommandLine ret = parser.parse(options, args);
            if (ret.getOptions().length == 0 || ret.hasOption(CONF_HELP)) {
                printCommandLineHelp(options);
                return null;
            }
            return ret;
        } catch (ParseException e) {
            printStream.println(e.getMessage());
            printCommandLineHelp(options);
            return null;
        }
    }

    public static CheckParams create(String[] args,PrintStream printStream,String callCall) {
        CommandLine cmdLine=checkCommandline(args, printStream);
        if (cmdLine == null) {
            return null;
        }
        CheckParams ret=new CheckParams(cmdLine);
        return ret;
    }

    private static Option createOption(String shortS, String longS, boolean args, String descr, boolean required) {
        Option o = new Option(shortS, longS, args, descr);
        o.setRequired(required);
        return o;
    }

    private static Options getCommandLineOptions() {
        Options options = new Options();
        options.addOption(createOption(CONF_HELP, "help", false, "Show help", false));
        options.addOption(createOption(CONF_USER, "user", true, "user used to login in keycloak server", true));
        options.addOption(createOption(CONF_PWD, "password", true, "password for keycloak user", true));
        options.addOption(createOption(CONF_SERVER, "keycloak", true, "URL to keycloak server", true));
        options.addOption(createOption(CONF_REALM, "realm", true, "Realm for the connection, if not given then 'master'", true));
        options.addOption(createOption(CONF_CLIENT, "client", true, "client used for the connection, if not given then 'admin-cli'", false));
        options.addOption(createOption(CONF_SECRET, "secret", true, "Client secret if 'confidential' connection used", false));
        options.addOption(createOption(CONF_INITFILE, "initfile", true, "init file to set up basic realms an stuff", true));
        return options;
    }

    public static void printCommandLineHelp() {
        printCommandLineHelp(getCommandLineOptions());
    }

    private static void printCommandLineHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setOptionComparator(new Comparator<Option>() {
            public int compare(Option option1, Option option2) {
                if (option1.isRequired() == option2.isRequired()) {
                    if (option1.getOpt() != null && option2.getOpt() != null) {
                        return option1.getOpt().compareTo(option2.getOpt());
                    } else if (option1.getLongOpt() != null && option2.getLongOpt() != null) {
                        return option1.getLongOpt().compareTo(option2.getLongOpt());
                    } else {
                        return -1;
                    }
                } else {
                    return (option1.isRequired() ? -1 : 1);
                }
            }
        });
        helpFormatter.setWidth(256);
        helpFormatter.printHelp("java " + InitKeycloakServer.class.getName(), options, true);
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getInitFile() {
        return initFile;
    }

    public void setInitFile(String initFile) {
        this.initFile = initFile;
    }
}