package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.StringTokenizer;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalDestFile;
import net.schmizz.sshj.xfer.scp.SCPDownloadClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.google.common.collect.Sets;

/**
 * Uses remote file path and names to download those files that would match useing shell pattern matching.
 * Because all source files are remote we can;t leverage bamboo's FileVisitor for ant-style glob matching of files.
 * 
 */
public class ReverseScpTask implements TaskType
{
    private final EncryptionService encryptionService;

    public ReverseScpTask(EncryptionService encryptionService)
    {
        this.encryptionService = encryptionService;
    }

    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.create(taskContext);

        final BuildLogger buildLogger = taskContext.getBuildLogger();

        final ConfigurationMap config = taskContext.getConfigurationMap();

        final String host = config.get("host");
        final String port = config.get("port");
        int portNumber = BaseSshTaskConfigurator.DEFAULT_SSH_PORT_NUMBER;
        if (StringUtils.isNotBlank(port))
        {
            portNumber = NumberUtils.toInt(port, BaseSshTaskConfigurator.DEFAULT_SSH_PORT_NUMBER);
        }
        final String username = config.get("username");
        final String password = encryptionService.decrypt(config.get("password"));
        final String privateKey = encryptionService.decrypt(config.get("private_key"));
        final String passphrase = encryptionService.decrypt(config.get("passphrase"));
        
        final SSHClient ssh = new SSHClient();

        //Always validate
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
            ssh.connect(host, portNumber);
            if (AuthType.valueOf(config.get(AuthType.CONFIG_KEY)) == AuthType.PASSWORD)
            {
                ssh.authPassword(username, password);
            } 
            else
            {
                KeyProvider[] kp = { new SSHKeyProvider(privateKey, passphrase) };
                ssh.authPublickey(username, Arrays.asList(kp));
            }
        }
        catch (IOException e)
        {
            buildLogger.addErrorLogEntry("Failed to connect to host", e);
            return taskResultBuilder.failedWithError().build();
        }

        final Set<String> failedToDownload = Sets.newHashSet();
        transferFiles(ssh, taskContext, taskResultBuilder, failedToDownload, buildLogger);

        if (!failedToDownload.isEmpty())
        {
            buildLogger.addErrorLogEntry("Copy Failed. Some files were not downloaded successfully.");
            return taskResultBuilder.failedWithError().build();
        }

        return taskResultBuilder.success().build();
    }

    private void transferFiles(final SSHClient ssh, final TaskContext taskContext, final TaskResultBuilder taskResultBuilder, final Set<String> failedToDownload, final BuildLogger buildLogger)
    {
        final String localPath = taskContext.getConfigurationMap().get("localPath");
        final String remotePath = taskContext.getConfigurationMap().get("remotePath");
        final String remoteNames = taskContext.getConfigurationMap().get("remotePattern");

        
        final Set<String> remoteFilesCopied = Sets.newHashSet();
        final Set<String> localDirectoriesCreated = Sets.newHashSet();

        final String baseDirectory = taskContext.getWorkingDirectory().getAbsolutePath();

        final Set<String> names = Sets.newTreeSet(new Comparator<String>() {

            @Override
            //shouldnt need to override compare, use strings default behavior
            public int compare(final String s, final String s1)
            {
                return s1.compareTo(s);
            }
        });
        
        if(StringUtils.isEmpty(remoteNames)){
        	names.add(remotePath.trim());
        }else{
	        StringTokenizer tokenizer = new StringTokenizer(remoteNames,",");
	        while(tokenizer.hasMoreTokens()){
	        	names.add(remotePath + "/" + tokenizer.nextToken().trim());
	        }
        }

        try
        {
        	for(String remoteFileName : names){
        		transferFile(ssh,  baseDirectory, localPath, remoteFileName, localDirectoriesCreated, remoteFilesCopied, failedToDownload, buildLogger);
        	}
            
        }
        catch (InterruptedException e)
        {
            taskResultBuilder.failedWithError().build();
        }
    }

    private void transferFile(SSHClient ssh,  String baseDirectory, String localPath, String sourceFileName, Set<String> localDirectoriesCreated, Set<String> remoteFilesCopied, Set<String> failedToDownload, BuildLogger buildLogger) throws InterruptedException
    {
        try
        {
           
            if (remoteFilesCopied.contains(sourceFileName))
            {
                buildLogger.addBuildLogEntry("File '" + sourceFileName + "' already copied, skipping...");
                return;
            }

           
            String destFileName = baseDirectory + "/" + localPath ;
            LocalDestFile destinationFile = new FileSystemFile(destFileName);

            createLocalDirectoryIfNotExists(destFileName, localDirectoriesCreated, buildLogger);
            

            buildLogger.addBuildLogEntry("Downloading '" + sourceFileName + "'...");
            SCPFileTransfer transfer = ssh.newSCPFileTransfer();
            //transfer.download(sourceFileName,destFileName);           
            SCPDownloadClient client = transfer.newSCPDownloadClient();
            client.setRecursiveMode(true);
            
            client.copy(sourceFileName, destinationFile);
            buildLogger.addBuildLogEntry("'" + sourceFileName + "' was downloaded successfully.");
            remoteFilesCopied.add(sourceFileName);
        }
        catch (IOException e)
        {
            buildLogger.addErrorLogEntry("Failed to download file '" + sourceFileName + "'", e);
            failedToDownload.add(sourceFileName);
            throw new InterruptedException("Download failed");
        }
    }

    private void createLocalDirectoryIfNotExists( final String fullLocalFilePath,  final Set<String> localDirectoriesCreated, final BuildLogger buildLogger) throws InterruptedException
    {
    	File localPath = new File(fullLocalFilePath);
    	String absPath = localPath.getAbsolutePath();
    	if (localDirectoriesCreated.contains(absPath))
        {
            return;
        }
        for (String localDirectory : localDirectoriesCreated)
        {
            if (localDirectory.startsWith(absPath))
            {
                return;
            }
        }

        buildLogger.addBuildLogEntry("Creating local directory " + absPath);
		localPath.mkdirs();
		// result of operation may be not checked - subsequent copy would fail if directory has not been created
		localDirectoriesCreated.add(absPath);
        
    }
}