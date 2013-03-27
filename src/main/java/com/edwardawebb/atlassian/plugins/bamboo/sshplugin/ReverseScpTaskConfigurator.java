package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ReverseScpTaskConfigurator extends BaseSshTaskConfigurator
{
    public ReverseScpTaskConfigurator(EncryptionService encryptionService)
    {
        super(encryptionService);
    }

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("localPath", params.getString("localPath"));
        config.put("remotePath", params.getString("remotePath"));
        config.put("remotePattern", params.getString("remotePattern"));
        return config;
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        context.put("localPath", taskDefinition.getConfiguration().get("localPath"));
        context.put("remotePath", taskDefinition.getConfiguration().get("remotePath"));
        context.put("remotePattern", taskDefinition.getConfiguration().get("remotePattern"));
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        context.put("localPath", taskDefinition.getConfiguration().get("localPath"));
        context.put("remotePath", taskDefinition.getConfiguration().get("remotePath"));
        context.put("remotePattern", taskDefinition.getConfiguration().get("remotePattern"));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        String localPath = params.getString("localPath");
        if (StringUtils.isEmpty(localPath))
        {
            errorCollection.addError("localPath", "You must specify a relative local path to save to");
        }

        String remotePath = params.getString("remotePath");
        if (StringUtils.isEmpty(remotePath))
        {
            errorCollection.addError("remotePath", "You must specify the remote path on the server to download");
        }
        /* String remotePattern = params.getString("remotePattern");
        if (StringUtils.isEmpty(remotePattern))
        {
            errorCollection.addError("remotePattern", "You must specify the remote pattern to match on the server");
        }*/
    }

}
