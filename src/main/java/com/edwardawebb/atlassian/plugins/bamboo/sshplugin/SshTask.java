package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import java.io.IOException;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.*;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;

public class SshTask implements TaskType
{
    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
    	 TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);

        final BuildLogger buildLogger = taskContext.getBuildLogger();

        final ConfigurationMap config = taskContext.getConfigurationMap();

        final String host = config.get("host");
        final String username = config.get("username");
        final String password = config.get("password");
        final String inlineScript = config.get("inlineScript");
        final long timeout = config.getAsLong("timeout");
        
        final SSHClient ssh = new SSHClient();

    
        buildLogger.addBuildLogEntry("Attempting SSH command(s):"+ inlineScript);
      //Always verify
        ssh.addHostKeyVerifier(new HostKeyVerifier()
        {
            @Override
            public boolean verify(final String s, final int i, final PublicKey publicKey)
            {
                return true;
            }
        });

        try
        {
            ssh.connect(host);
            ssh.authPassword(username, password);
        }
        catch (IOException e)
        {
            buildLogger.addErrorLogEntry("Failed to connect to host", e);
            return taskResultBuilder.failedWithError().build();
        }
        try
        {
        	
            try {
                final Session session = ssh.startSession();
                try {
                    final Command cmd = session.exec(inlineScript);

                    
                    buildLogger.addBuildLogEntry("Connected");
                    buildLogger.addErrorLogEntry(IOUtils.readFully(cmd.getInputStream()).toString());
                    //while(cmd.getExitStatus() == null){
                        cmd.join((int)timeout, TimeUnit.SECONDS);
                    //}
                    int result = cmd.getExitStatus();
                    if(result != 0){
                    	buildLogger.addErrorLogEntry("SSH script failed with error code: " + result);
                    	taskResultBuilder = taskResultBuilder.failedWithError();
                    }  else {
                        buildLogger.addBuildLogEntry("Successfully executed SSH commands");
                    	taskResultBuilder = taskResultBuilder.success();
                    }
                } finally {
                    session.close();
                }
            } finally {
                ssh.disconnect();
            }
        }
        catch (IOException e) {
			e.printStackTrace();
		}



        return taskResultBuilder.build();
    }
}