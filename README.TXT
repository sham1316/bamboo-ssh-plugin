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
* Changing the executed command requires re-entry of password




Want to contribute or modify? see the wiki, or try


Download and install atlassian's latest SDK

Checkout this code
$ git clone git@bitbucket.org:eddiewebb/bamboo-ssh-plugin.git

And then change to the directory that you downloaded and..

* atlas-run   -- installs this plugin into Bamboo and starts it on http://localhost:6990/bamboo
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running Bamboo instance
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

https://developer.atlassian.com/display/DOCS/Developing+with+the+Atlassian+Plugin+SDK

A big thanks to the bamboo scp plugin, https://bitbucket.org/i386/bamboo-scp-plugin
and it's authors for inspiring the task.
 
 
 
