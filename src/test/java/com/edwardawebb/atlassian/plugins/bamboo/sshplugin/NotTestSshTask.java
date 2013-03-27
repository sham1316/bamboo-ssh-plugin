package com.edwardawebb.atlassian.plugins.bamboo.sshplugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskConfiguratorHelper;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskException;
import com.google.common.collect.ImmutableList;

public class NotTestSshTask extends TestCase
{
	
	private static final 
	ActionParametersMap ALL_PARAMS = mock(ActionParametersMap.class);
	
	//TODO config builder for testing
	private static final List<String> FIELDS_TO_COPY_ALWAYS = ImmutableList.of("host", "username","timeout","inlineScript");
    private static final List<String> FIELDS_TO_COPY_SECURE = ImmutableList.of("host", "username","timeout","inlineScript","password");
    
    
    public static void setup(){
    	ALL_PARAMS.put("host", "localhost");
    	ALL_PARAMS.put("username", "testuser");
    	ALL_PARAMS.put("password", "password");
    	ALL_PARAMS.put("inlineScript", "whoami");
    	ALL_PARAMS.put("timeout", "15");
    	ALL_PARAMS.put("new_password", "new_password");
    }
    
	public void testTheConfigValuesCanBeCreated(){
		SshTaskConfigurator config = new SshTaskConfigurator();
		TaskDefinition previousDef = null; //new config
		
		ActionParametersMap params = mock(ActionParametersMap.class);
		populateParams(params);
		params.put("change_command","");
		
		 Map<String, String> results = config.generateTaskConfigMap(params, previousDef);
		 for (String string : FIELDS_TO_COPY_SECURE) {
			assertTrue(results.get(string).equals(params.get(string)));
		}
		
	}

	public void testNonSecureConfigValuesCanBeUpdatedWithoutChangingSecureValues(){
		//given a full previous config
		SshTaskConfigurator config = new SshTaskConfigurator();
		TaskDefinition previousDef = mock(TaskDefinition.class);
		when(previousDef.getConfiguration()).thenReturn(createMap());
		
		//wehn the password nor command are passed
		ActionParametersMap params = mock(ActionParametersMap.class);
		populateParams(params);
		params.remove("password");
		params.remove("inlineScript");
		
		//they are still retained
		 Map<String, String> results = config.generateTaskConfigMap(params, previousDef);
		 for (String string : FIELDS_TO_COPY_SECURE) {
			assertTrue(results.get(string).equals(ALL_PARAMS.get(string)));
		}
		
	}
	
	
	public void testUpdatingSecureConfigValuesRequiresNewPassword(){
		//given a full previous config
		SshTaskConfigurator config = new SshTaskConfigurator();
		TaskDefinition previousDef = mock(TaskDefinition.class);
		when(previousDef.getConfiguration()).thenReturn(createMap());
		
		//wehn the comman was changed
		ActionParametersMap params = mock(ActionParametersMap.class);
		populateParams(params);
		params.put("change_command","true");
		params.put("password","");
		
		Map<String, String> results = config.generateTaskConfigMap(params, previousDef);
		assertTrue(results.get("password").equals(""));
		
	}

	private void populateParams(ActionParametersMap map){

		map.put("host", "localhost");
		map.put("username", "testuser");
		map.put("password", "password");
		map.put("inlineScript", "whoami");
		map.put("timeout", "15");
		map.put("new_password", "new_password");
	}

	private Map<String, String> createMap(){
		Map<String, String> map = new HashMap<String, String>();
		map.put("host", "localhost");
		map.put("username", "testuser");
		map.put("password", "password");
		map.put("inlineScript", "whoami");
		map.put("timeout", "15");
		return map;
	}
	
	
	public void dontTestTheTaskCanSshToLocalhostAsTestUser()
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
