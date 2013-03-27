package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.security.EncryptionService;
import com.google.common.collect.ImmutableList;
import com.opensymphony.xwork.TextProvider;

public class SshTaskConfigurator extends AbstractTaskConfigurator
{
	//TODO use to pull i18n strings
    private transient TextProvider textProvider;

	private transient EncryptionService encryptionService;

    public static final String CREATE_MODE = "create";
    public static final String EDIT_MODE = "edit";
    public static final String MODE = "mode";

    private static final List<String> FIELDS_TO_COPY_ALWAYS = ImmutableList.of("host", "username","timeout","inlineScript");
    private static final List<String> FIELDS_TO_COPY_SECURE = ImmutableList.of("host", "username","timeout","inlineScript","password");
    


    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS_TO_COPY_ALWAYS);

        String commandChange = params.getString("change_command");
        if ("true".equals(commandChange))
        {
        	//The old password was discarded, bt command was show so it is posible they mad ehcanges
            final String password = params.getString("new_password");
            config.put("password", encryptionService.encrypt( password));
            final String script = params.getString("inlineScript");
            config.put("inlineScript", script);
        }
        else if (previousTaskDefinition != null)
        {
        		//pass through and they did not change poassword or command
            config.put("password", encryptionService.encrypt(previousTaskDefinition.getConfiguration().get("password")));
            config.put("inlineScript", previousTaskDefinition.getConfiguration().get("inlineScript"));
        }
        else
        {
        	//first time around, let them set each fresh
            final String password = params.getString("password");
            config.put("password", encryptionService.encrypt(password));
            final String script = params.getString("inlineScript");
            config.put("inlineScript", script);
            
        }

        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);

        context.put("host", "example.com");
        context.put("username", "release");
        context.put(MODE, CREATE_MODE);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY_SECURE);
        context.put(MODE, EDIT_MODE);
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY_ALWAYS);
        
        context.put("inlineScript", taskDefinition.getConfiguration().get("inlineScript").replaceAll("\n", "\r\n     "));
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

        String username = params.getString("username");
        if (StringUtils.isEmpty(username))
        {
            errorCollection.addError("username", "You must specify a username");
        }

        String localPath = params.getString("inlineScript");
        if (StringUtils.isEmpty(localPath))
        {
            errorCollection.addError("inlineScript", "You must specify a one or more commands, line seperated");
        }

        String remotePath = params.getString("timeout");
        if (StringUtils.isEmpty(remotePath))
        {
            errorCollection.addError("timeout", "Specify the number of seconds to wait before giving up");
        }
        
        String commandChange = params.getString("change_command");
        if ("true".equals(commandChange))
        {
        	 String password = params.getString("new_password");
        	 if(StringUtils.isEmpty(password)){
        		 errorCollection.addError("new_password", "When altering the commands, password re-entry is required"); 
        	 }
             String script = params.getString("inlineScript");
        	 if(StringUtils.isEmpty(script)){
        		 errorCollection.addError("inlineScript", "Wouldn't you rather I did _something_ ?"); 
        	 }
        }
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }

    public void setEncryptionService(EncryptionService encryptionService)
    {
        this.encryptionService = encryptionService;
    }
}
