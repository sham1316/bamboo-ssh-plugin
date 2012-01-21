#################################################			
#  												#
#     Bamboo SSH Plugin for password support	#
#												#
#################################################

Author: Eddie Webb
Date : Jan 21, 2012
Contact: eddie@edwardawebb.com
License: GNU General Public License v3
	

OpenSSH does not support supplying passwords 
to the SSH command by any other means than prompting user
(or keys obviously).  Some programs like Expect can be used to
remedy this, but it not readily found in enterprise environments.




* Passwords are masked in logs
* Passwords never seen in command history or "ps aux"
* Passwords (will someday be) stored securely


Example output
20-Jan-2012 21:17:49	Starting task 'host419.example.com' of type 'com.edwardawebb.bamboo-ssh-plugin:test'
20-Jan-2012 21:17:50	Attempting SSH command(s):echo "who are you?"whoamiecho where are you?hostname
20-Jan-2012 21:17:52	Connected
20-Jan-2012 21:17:52	who are you?test_userwhere are you?host419
20-Jan-2012 21:17:52	Successfully executed SSH commands
20-Jan-2012 21:17:52	Finished task 'host419.example.com'


A big thanks to the bamboo scp plugin, https://bitbucket.org/i386/bamboo-scp-plugin
and it's authors for inspiring the task.











You have successfully created a plugin using the Bamboo plugin archetype!

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into Bamboo and starts it on http://localhost:6990/bamboo
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running Bamboo instance
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Developing+with+the+Atlassian+Plugin+SDK
