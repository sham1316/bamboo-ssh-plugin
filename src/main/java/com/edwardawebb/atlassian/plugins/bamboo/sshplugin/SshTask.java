package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import java.io.IOException;
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
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.variable.CustomVariableContext;

public class SshTask implements TaskType {
	private transient EncryptionService encryptionService;

	@NotNull
	@java.lang.Override
	public TaskResult execute(@NotNull final TaskContext taskContext)
			throws TaskException {
		TaskResultBuilder taskResultBuilder = TaskResultBuilder
				.create(taskContext);

		boolean failure = false;
		final BuildLogger buildLogger = taskContext.getBuildLogger();

		final ConfigurationMap config = taskContext.getConfigurationMap();

		final String host = config.get("host");
		final String username = config.get("username");
		String decrypted;
		try{
			 decrypted =encryptionService.decrypt(config.get("password"));
		}catch(EncryptionException e){
			buildLogger.addBuildLogEntry("Decryption of SSH password failed, will attempt to use value as it exists in DB, and save encrypted version for subsequent use");
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
					buildLogger.addBuildLogEntry("      " + IOUtils.readFully(cmd.getInputStream()).toString());
					cmd.join((int)timeout, TimeUnit.SECONDS);
					if ( cmd.getExitStatus() != 0 || null != cmd.getExitErrorMessage() ){
						buildLogger.addErrorLogEntry("SSH script failed with error code: "
								+ cmd.getExitStatus());
						buildLogger.addErrorLogEntry("Message: "
										+ cmd.getExitErrorMessage());
						failure = true;
						break;
					}					
				} finally {
					session.close();
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				ssh.disconnect();
				ssh.close();
			} catch (IOException e) {
				taskResultBuilder = taskResultBuilder.failedWithError();
				e.printStackTrace();
			}
		}
		if(!failure){
			taskResultBuilder = taskResultBuilder.success();			
			buildLogger
					.addBuildLogEntry("Successfully executed SSH commands");
		}else{
			taskResultBuilder = taskResultBuilder.failedWithError();
		}
		return taskResultBuilder.build();
	}

	
	public void setEncryptionService(EncryptionService encryptionService)
	{
	    this.encryptionService = encryptionService;
	}
	
}


//This should work!
//for (String commandLine : inlineScript.split("\n")){
//	buildLogger.addBuildLogEntry("Exec: " + commandLine);
//	final Command cmd = session.exec(commandLine);
//	String output = CommandReader.getNextLineFrom(cmd.getInputStream());
//	if(cmd.getExitStatus() != 0){
//		buildLogger.addErrorLogEntry("Command Failed: " + commandLine);
//		for (String string : IOUtils
//				.readFully(cmd.getErrorStream()).toString()
//				.split("\n")) {
//			buildLogger.addErrorLogEntry("\t" + string);								
//		}
//		break;
//	}
//	
//	buildLogger.addBuildLogEntry(output);
//}

