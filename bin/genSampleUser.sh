#!/bin/bash
prog="$0"
count=100
basedn=""
domain=""
users="ou=Users"

function usage() {
    echo "$prog -d <domain> [-b <basedn>] [-c <count>] [-u <users ou>]"
    exit 1
}

while [ "$1" != "" ]; do
     case "$1" in
	"-c" ) count=$2;    shift;;
	"-b" ) basedn="$2"; shift;;
        "-u" ) users="$2";  shift;;
	"-d" ) domain="$2"; shift; basedn=`echo "$domain" | awk 'BEGIN{FS=".";d=""}{ for(i=NF;i>0; i--){ if ( d != "" ){d=","d}; d="dc="$i d }}END{print d}'` ;;
     esac
     shift;
done

if [[ "${domain}" == "" ]]; then
        echo "ERROR: no domain provided"
        usage
fi
if [[ "${basedn}" == "" ]]; then
	echo "ERROR: no basedn provided"
	usage
fi

la=1;
while [ $la -le $count ]
do
 	echo "dn: ou=user$la,${users},${basedn}"
	echo "objectclass: top"
	echo "objectclass: person"
	echo "objectclass: inetOrgPerson"
	echo "uid: user$la"
	echo "sn: User$la"
	echo "givenname: Name$la"
	echo "cn: User$la Name$la"
	echo "mail: user$la.name${la}@${domain}"
	echo "userPassword: user$la"
	echo	

	la=$(( $la + 1 )) 
done 
