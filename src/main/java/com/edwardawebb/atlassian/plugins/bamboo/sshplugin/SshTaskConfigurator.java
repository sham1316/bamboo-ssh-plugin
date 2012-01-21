package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.opensymphony.xwork.TextProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SshTaskConfigurator extends AbstractTaskConfigurator
{
    private TextProvider textProvider;

    public static final String CREATE_MODE = "create";
    public static final String EDIT_MODE = "edit";
    public static final String MODE = "mode";

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("host", params.getString("host"));
        config.put("username", params.getString("username"));
        config.put("inlineScript", params.getString("inlineScript"));
        config.put("timeout", params.getString("timeout"));

        String passwordChange = params.getString("change_password");
        if ("true".equals(passwordChange))
        {
            final String password = params.getString("new_password");
            config.put("password", password);
        }
        else if (previousTaskDefinition != null)
        {
            config.put("password", previousTaskDefinition.getConfiguration().get("password"));
        }
        else
        {
            final String password = params.getString("password");
            config.put("password", password);
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

        context.put("host", taskDefinition.getConfiguration().get("host"));
        context.put("username", taskDefinition.getConfiguration().get("username"));
        context.put("password", taskDefinition.getConfiguration().get("password"));
        context.put("timeout", taskDefinition.getConfiguration().get("timeout"));
        context.put("inlineScript", taskDefinition.getConfiguration().get("inlineScript"));
        context.put(MODE, EDIT_MODE);
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);

        context.put("host", taskDefinition.getConfiguration().get("host"));
        context.put("username", taskDefinition.getConfiguration().get("username"));
        //context.put("password", taskDefinition.getConfiguration().get("password"));
        context.put("timeout", taskDefinition.getConfiguration().get("timeout"));
        context.put("inlineScript", taskDefinition.getConfiguration().get("inlineScript"));
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
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }
}
