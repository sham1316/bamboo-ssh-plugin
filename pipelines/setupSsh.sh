#!/bin/bash


#
# Setup machine to trust remote maven repo server using key in BB environment variables
#
cat /etc/ssh/ssh_config
echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config
cat /etc/ssh/ssh_config
mkdir -p ~/.ssh
echo -e $PRIVATEKEY > ~/.ssh/id_rsa
chmod 0600 ~/.ssh/id_rsa
eval "$(ssh-agent)"
ssh-add ~/.ssh/id_rsa
ssh eawmvn@maven.edwardawebb.com echo test 1>/dev/null 2>&1 && echo SSH_OK || echo SSH_NOK