package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.security.EncryptionException;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.CommonTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;

public class SshTask implements CommonTaskType {
        private transient EncryptionService encryptionService;

        @NotNull
        @java.lang.Override
        public TaskResult execute(@NotNull final CommonTaskContext taskContext)
                        throws TaskException {
                TaskResultBuilder taskResultBuilder = TaskResultBuilder
                                .newBuilder(taskContext);

                boolean failure = false;
                final BuildLogger buildLogger = taskContext.getBuildLogger();

                final ConfigurationMap config = taskContext.getConfigurationMap();

                final String host = config.get("host");
                final String username = config.get("username");
                String decrypted;
                try{
                         decrypted =encryptionService.decrypt(config.get("password"));
                }catch(EncryptionException e){
                        buildLogger.addBuildLogEntry("Decryption of SSH password failed, will attempt to use value as it exists in DB.");
                        decrypted = config.get("password");
                }
                final String password = decrypted;

                final String inlineScript = config.get("inlineScript");
                final long timeout = config.getAsLong("timeout");

                final SSHClient ssh = new SSHClient();

                buildLogger.addBuildLogEntry("Attempting SSH connection");
        
                ssh.addHostKeyVerifier(new HostKeyVerifier() {
                        @Override
                        public boolean verify(final String s, final int i,
                                        final PublicKey publicKey) {
                                return true;
                        }
                });

                try {
                        ssh.connect(host);
                        ssh.authPassword(username, password);
                        buildLogger.addBuildLogEntry("Connected to " + host + " as " + username);
                } catch (IOException e) {
                        buildLogger.addErrorLogEntry("Failed to connect to host", e);
                        return taskResultBuilder.failedWithError().build();
                }
                
                
                try{
                        
					for (String commandLine : inlineScript.split("\n")){
						buildLogger.addBuildLogEntry("Exec: " + commandLine);
						final Session session = ssh.startSession();
						try{
								final Command cmd = session.exec(commandLine);
								
								BufferedReader in = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
								String line = null;
								while((line = in.readLine()) != null) {
										buildLogger.addBuildLogEntry(line);
								}
								cmd.join((int)timeout, TimeUnit.SECONDS);
								if ( cmd.getExitStatus() != 0 || null != cmd.getExitErrorMessage() ){
										buildLogger.addErrorLogEntry("SSH script failed with error code: "
														+ cmd.getExitStatus());
										String error = cmd.getExitErrorMessage();
										if ( null == error){
												error = IOUtils.readFully(cmd.getErrorStream()).toString();
										}
										buildLogger.addErrorLogEntry("Error Details (if any): "
														+ error);
										
										throw new SSHExecutionException("Failed to execute " + commandLine + ", return code: " + cmd.getExitStatus());
								}                                       
						} finally {
								session.close();
						}
						taskResultBuilder = taskResultBuilder.success();                        
						buildLogger.addBuildLogEntry("Successfully executed SSH commands");
					}
                        
                } catch (IOException e) {
                        taskResultBuilder = taskResultBuilder.failedWithError();
                        e.printStackTrace();
                }catch (SSHExecutionException e) {
                        taskResultBuilder = taskResultBuilder.failedWithError();                        
                }finally {
                        try {
                                ssh.disconnect();
								ssh.close();
                                buildLogger.addBuildLogEntry("Disconnected from server");
                        } catch (IOException e) {
                                taskResultBuilder = taskResultBuilder.failedWithError();
                                e.printStackTrace();
                        }
                }
                
                return taskResultBuilder.build();
        }

        
        public void setEncryptionService(EncryptionService encryptionService)
        {
            this.encryptionService = encryptionService;
        }
        
}