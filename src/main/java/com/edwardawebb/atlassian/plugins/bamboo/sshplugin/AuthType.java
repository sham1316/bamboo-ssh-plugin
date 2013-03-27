package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

public enum AuthType 
{
    PASSWORD("Password"),
    KEY("Key without passphrase"),
    KEY_WITH_PASSPHRASE("Key with passphrase");

    public static final String CONFIG_KEY = "authType";
    private final String display;

    AuthType(String display)
    {
        this.display = display;
    }

    public String getKey()
    {
        return name();
    }

    public String getDisplayName()
    {
        return display;
    }
}