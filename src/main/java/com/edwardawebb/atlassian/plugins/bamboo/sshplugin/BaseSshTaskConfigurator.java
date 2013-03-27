package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

public class BaseSshTaskConfigurator extends AbstractTaskConfigurator
{
    private static final Logger log = Logger.getLogger(BaseSshTaskConfigurator.class);

    private static final EnumSet<AuthType> SUPPORTED_AUTH_TYPES = EnumSet.of(AuthType.PASSWORD, AuthType.KEY, AuthType.KEY_WITH_PASSPHRASE);
    public static final int DEFAULT_SSH_PORT_NUMBER = 22;

    private final EncryptionService encryptionService;

    public BaseSshTaskConfigurator(EncryptionService encryptionService)
    {
        this.encryptionService = encryptionService;
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("host", params.getString("host"));
        config.put("username", params.getString("username"));
        config.put("authType", AuthType.valueOf(params.getString("authType")).toString());

        String port = params.getString("port");
        if (StringUtils.isNotEmpty(port))
        {
            config.put("port", Integer.toString(NumberUtils.toInt(port, DEFAULT_SSH_PORT_NUMBER)));
        }

        String passwordChange = params.getString("change_password");
        if ("true".equals(passwordChange))
        {
            final String password = params.getString("password");
            config.put("password", encryptionService.encrypt(password));
        }
        else if (previousTaskDefinition != null)
        {
            config.put("password", previousTaskDefinition.getConfiguration().get("password"));
        }
        else
        {
            final String password = params.getString("password");
            config.put("password", encryptionService.encrypt(password));
        }

        String keyChange = params.getString("change_key");
        if ("true".equals(keyChange))
        {
            String key = readPrivateKey(params);
            if (key != null)
            {
                config.put("private_key", encryptionService.encrypt(key));
            }
        }
        else if (previousTaskDefinition != null)
        {
            config.put("private_key",  previousTaskDefinition.getConfiguration().get("private_key"));
        }
        else
        {
            final String privateKey = params.getString("private_key");
            config.put("private_key", privateKey);
        }

        String passphraseChange = params.getString("change_passphrase");
        if ("true".equals(passphraseChange))
        {
            final String passphrase = params.getString("passphrase");
            config.put("passphrase", encryptionService.encrypt(passphrase));
        }
        else if (previousTaskDefinition != null)
        {
            config.put("passphrase", previousTaskDefinition.getConfiguration().get("passphrase"));
        }
        else
        {
            final String passphrase = params.getString("passphrase");
            config.put("passphrase", encryptionService.encrypt(passphrase));
        }

        return config;
    }

    private String readPrivateKey(final ActionParametersMap params)
    {
        final File private_key_file = params.getFiles().get("private_key");
        if (private_key_file != null)
        {
            final String key;
            try
            {
                key = FileUtils.readFileToString((File) private_key_file);
                return key;
            }
            catch (IOException e)
            {
                log.error("Cannot read uploaded ssh key file", e);
            }

        }
        return null;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        context.put("authenticationTypes", SUPPORTED_AUTH_TYPES);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);

        context.put("host", taskDefinition.getConfiguration().get("host"));
        final String port = taskDefinition.getConfiguration().get("port");
        if (port != null)
        {
            context.put("port", NumberUtils.toInt(port, DEFAULT_SSH_PORT_NUMBER));
        }
        context.put("username", taskDefinition.getConfiguration().get("username"));
        context.put("password", taskDefinition.getConfiguration().get("password"));
        if (StringUtils.isNotBlank(taskDefinition.getConfiguration().get("private_key")))
        {
            context.put("private_key_defined", "true");
        }
        context.put("passphrase", taskDefinition.getConfiguration().get("passphrase"));
        context.put("localPath", taskDefinition.getConfiguration().get("localPath"));
        context.put("remotePath", taskDefinition.getConfiguration().get("remotePath"));
        context.put("authType", taskDefinition.getConfiguration().get("authType"));
        context.put("authenticationTypes", SUPPORTED_AUTH_TYPES);
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);

        context.put("host", taskDefinition.getConfiguration().get("host"));
        context.put("username", taskDefinition.getConfiguration().get("username"));
        context.put("authType", taskDefinition.getConfiguration().get("authType"));
        context.put("localPath", taskDefinition.getConfiguration().get("localPath"));
        context.put("remotePath", taskDefinition.getConfiguration().get("remotePath"));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        String host = params.getString("host");
        if (StringUtils.isEmpty(host))
        {
            errorCollection.addError("host", "You must specify a host to connect to");
        }

        String port = params.getString("port");
        if (StringUtils.isNotEmpty(port))
        {
            final int portNumber = NumberUtils.toInt(port, -1);
            if (portNumber < 0)
            {
                errorCollection.addError("port", "Port number must be a positive number");
            }
        }
        String username = params.getString("username");
        if (StringUtils.isEmpty(username))
        {
            errorCollection.addError("username", "You must specify a username");
        }

        AuthType authType = AuthType.valueOf(params.getString("authType"));
        switch (authType)
        {
            case PASSWORD:
                if ("true".equals(params.getString("change_password")))
                {
                    String password = params.getString("password");
                    if (StringUtils.isEmpty(password))
                    {
                        errorCollection.addError("password", "You must specify password");
                    }
                }
                break;
            case KEY:
                if ("true".equals(params.getString("change_key")))
                {
                    SSHKeyProvider.validatePrivateKey(readPrivateKey(params), null, errorCollection);
                }
                break;
            case KEY_WITH_PASSPHRASE:
                if ("true".equals(params.getString("change_key")))
                {
                    SSHKeyProvider.validatePrivateKey(readPrivateKey(params), params.getString("passphrase"), errorCollection);
                }
                if ("true".equals(params.getString("change_passphrase")))
                {
                    String passphrase = params.getString("passphrase");
                    if (StringUtils.isEmpty(passphrase))
                    {
                        errorCollection.addError("passphrase", "You must specify private key passphrase");
                    }
                }
                break;
        }
    }

}
