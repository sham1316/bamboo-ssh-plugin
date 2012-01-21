package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import junit.framework.TestCase;

import org.junit.Ignore;
import org.junit.Test;

import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.edwardawebb.atlassian.plugins.bamboo.sshplugin.SshTask;

public class NotTestSshTask extends TestCase
{
	@Test
	public void testTheTaskCanSshToLocalhostAsTestUser()
	{
		ConfigurationMap config = mock(ConfigurationMap.class);
		config.put("host", "localhost");
		config.put("username", "testuser");
		config.put("password", "password");
		config.put("inlineScript", "whoami");
		config.put("timeout", "15");
		TaskContext context =mock(TaskContext.class);
		when(context.getConfigurationMap()).thenReturn(config);
		
		SshTask task = new SshTask();
		
		
		try {
			task.execute(context);
		} catch (TaskException e) {
			e.printStackTrace();
			fail();			
		}

	}
}
