#!/bin/bash

#
#  This script will publish an Atlassian plugin (.jar) to the marketplace after releasing to configured maven repo.
#

function configureGitInfo {
	# make git happy
	git config  user.email "pipes@bitbucket.com"
	git config  user.name "BBPipelines"
}

function findReleaseVersion {
	# pull release version in pom file
	releaseVersion=$(mvn help:evaluate --batch-mode  -Dexpression=project.version 2>/dev/null | grep -E '^[0-9.]*$')
	case "$releaseVersion" in
		[0-9.]*)
			printf "\n\n-------------------------------------\n\tUsing release version: $releaseVersion\n------------------------------\n\n"
			;;
		*)
			echo  "Error, no version found. Be sure to commit final (non-snapshot) version as part of merge to shipit! Otherwise check logs for maven failure"
			exit 2
	esac

	#Optionally you can override version from a variable or another file and update pom and release
	#mvn --batch-mode versions:set -DnewVersion=${releaseVersion}
}


#deploy to configured repository and tagg with version
function deployAndTag {
	mvn --batch-mode deploy scm:tag
}

# with artifact available at URL this publishes the details to markeplce
function publishToMarketplace {
	#public available URL to find our plugin (NOTE - marketplace provides means to upload via API first if you dont have a public repo)
	PUBLISHEDURL="http://maven.edwardawebb.com/repository/releases/com/edwardawebb/bamboo-ssh-plugin/${releaseVersion}/bamboo-ssh-plugin-${releaseVersion}.jar"
	versionToBuildNumber
	#setup and replace values in json template
	TODAY=`date +%Y-%m-%d`
	RLSSUMMARY=`git log -1 --pretty=%B`
	sed -e "s/\${VERSION}/${releaseVersion}/g" \
	    -e "s/\${TODAY}/${TODAY}/g" \
	    -e "s/\${SUMMARY}/${RLSSUMMARY}/g" \
	    -e "s/\${BUILD}/${buildNumber}/g" \
	    -e "s#\${URL}#${PUBLISHEDURL}#g" \
	    pipelines/marketplacePost.json > target/marketplacePost.json
	#publish the released artifact to atlassian marketplace
	httpCode=$(curl --basic -u ${MKTUSER}:${MKTPASSWD} \
	-H "Content-Type: application/json" \
	--data-binary @target/marketplacePost.json \
	-w "%{http_code}" \
	-o "target/mktError.json" \
	https://marketplace.atlassian.com/rest/2/addons/${MKTADDON}/versions)

	echo "publish resulted in $httpCode"
	if [ $httpCode -ne 201 ];then
		echo "Error publishing" >&2
		touch target/mktError.json
		cat target/mktError.json >&2
		exit 1
	fi
    printf  "\n-----------------------\nPublished!  SHipit complete\n---------------------------\n"

}


# convert app version into marketplace buildnumber (bitbucket only offers commit hash as unique identifier)
# GIven version like 2, 2.2, 2.2.2, etc, return a 9 digit number (200000000,200200000,200200200)
# WHen using 4 places and the fourth is treated in the last 3 digits.
# (ie. 1.2.3.4 would be 100200304, while 1.2.333.444 would be 100200777 and might break order)
function versionToBuildNumber {
	digits=( $( IFS='.'; for digit in $releaseVersion; do echo "$digit"; done) )
	let buildNumber=0
	while [ ${#digits[@]} -gt 0 ];do
		let index=${#digits[@]}-1
		value=${digits[$index]}
		case "${#digits[@]}" in
			"3")
				padding=3
				;;
			"2")
				padding=6
				;;
			"1")
				padding=9
				;;
			*)
		esac
		revValue=`echo $value | rev`
		placeValue=`printf %0${padding}d $revValue | rev`
		let buildNumber=buildNumber+placeValue
		echo "Place ${#digits[@]} with value ${digits[$index]} makes it $buildNumber"
		unset -v digits[$index]
	done
	echo "Marketplace BuildNumber: $buildNumber"
}



configureGitInfo
findReleaseVersion
versionToBuildNumber
deployAndTag
publishToMarketplace
