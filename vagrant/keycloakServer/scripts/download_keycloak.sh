#!/bin/bash

scriptPos=${0%/*}

function checkAndGgfDownload() {
	srcFile="$1"
	downloadUrl="$2"
	destDir="$3"
	destFile="$destDir/$srcFile"

	if ! [ -d "$destDir" ]; then
		mkdir "$destDir"
	fi

	if ! [ -f "$destFile" ]; then
		if ! curl "$downloadUrl" -o "$destFile" 2> /dev/null
		then 
			echo -en "\033[1;31mFehler beim Download von $destFile \033[0m\n"
		else	
			echo -en "\033[1;32m $destFile :) \033[0m\n"
		fi 
	else
		echo -en "\033[1;32m $destFile :) \033[0m\n"
	fi
}

s1=`curl http://keycloak.jboss.org/downloads | grep -P "keycloak-\d+\.\d+\.\d+\.Final\.tar\.gz"`

KC_FINAL_URL=${s1##*ref=}
KC_FINAL_URL=${KC_FINAL_URL%%\"*}
#echo $KC_FINAL_URL
KC_VERSION=${KC_FINAL_URL##*-}
KC_VERSION=${KC_VERSION%.F*}
#echo $KC_VERSION
echo $KC_FINAL_URL

KC_ADAPTER_BASE_URL=${KC_FINAL_URL%/*}
KC_ADAPTER_BASE_URL="$KC_ADAPTER_BASE_URL/adapters/keycloak-oidc"


checkAndGgfDownload "keycloak-${KC_VERSION}.Final.tar.gz" "$KC_FINAL_URL" "/opt/downloads"

# Tomcat7 Adapter
fileName="keycloak-tomcat7-adapter-dist-${KC_VERSION}.Final.tar.gz"
url="$KC_ADAPTER_BASE_URL/$fileName"
destDir="/opt/downloads/oid"
checkAndGgfDownload "$fileName" "$url" "$destDir"

# Tomcat8 Adapter
fileName="keycloak-tomcat8-adapter-dist-${KC_VERSION}.Final.tar.gz"
url="$KC_ADAPTER_BASE_URL/$fileName"
destDir="/opt/downloads/oid"
checkAndGgfDownload "$fileName" "$url" "$destDir"


