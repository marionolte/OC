#!/bin/bash
# v1.00 22.06.2016 Opitz Consulting GmbH

 

if [ $# -ne 1 ] ; then echo "usage: $0 <test/stage>";exit; fi

 

ENV=$1

MODE=$2

 

##########

# main

##########

clear

ROOT=/opt/oracle/config-shared/bin

echo "-------------[ SYSTEMSTATUS: $ENV (`uname -n`) ]---------------"

echo

 

checkUrl(){

  wget --spider -q $1

  case $? in

    0)echo -ne "\033[32mOK\033[37m";;

    *)echo -ne "\033[31mERR\033[37m";;

  esac

}

 

case $ENV in

test) echo "* nodeManager(`$ROOT/nodeManager.sh soa_test status | awk '{print $2}'`) "

 

      echo "_____________________________"

      srv=frsoatest1soaadmin; echo; echo -n "* $srv(`$ROOT/adminServer.sh soa_test status | awk '{print $2}'`): CONSOLE=`checkUrl \"$srv:7001/console\"`, EM=`checkUrl \"$srv:7001/em\"`"

      srv=frsoatest1soa01; echo; echo -n "* $srv(`$ROOT/managedServer.sh soa_test soa_server1 fru3324e status | awk '{print $2}'`): SOACOMPOSER=`checkUrl \"$srv:8001/soa/composer\"`"

      srv=frsoatest1soa02; echo; echo -n "* $srv(`$ROOT/managedServer.sh soa_test soa_server2 fru3324e status | awk '{print $2}'`): SOACOMPOSER=`checkUrl \"$srv:8001/soa/composer\"`"

      echo;echo "* soa_test.ess_server1(`$ROOT/managedServer.sh soa_test ess_server1 fru3324e status | awk '{print $2}'`)"

      echo "* soa_test.wsm_server1(`$ROOT/managedServer.sh soa_test wsm_server1 fru3324e status | awk '{print $2}'`)"

 

      echo "_____________________________"

      srv=frsoatest1bamadmin; echo; echo -n "* $srv(`$ROOT/adminServer.sh bam_test status | awk '{print $2}'`): CONSOLE=`checkUrl \"$srv:7001/console\"`, EM=`checkUrl \"$srv:7001/em\"`"

     srv=frsoatest1bam01; echo; echo -n "* $srv(`$ROOT/managedServer.sh bam_test bam_server1 fru3324e status | awk '{print $2}'`): BAMCOMPOSER=`checkUrl \"$srv:9001/bam/composer\"`"

      srv=frsoatest1bam02; echo; echo -n "* $srv(`$ROOT/managedServer.sh bam_test bam_server2 fru3324e status | awk '{print $2}'`): BAMCOMPOSER=`checkUrl \"$srv:9001/bam/composer\"`"

 

      echo;echo "_____________________________"

      srv=frsoatest1osbadmin; echo; echo -n "* $srv(`$ROOT/adminServer.sh osb_test status | awk '{print $2}'`): CONSOLE=`checkUrl \"$srv:7001/console\"`, EM=`checkUrl \"$srv:7001/em\"`, SERVICEBUS=`checkUrl \"$srv:7001/servicebus\"`"

      srv=frsoatest1osb01; echo; echo -n "* $srv(`$ROOT/managedServer.sh soa_test osb_server1 fru3324e status | awk '{print $2}'`)"

      srv=frsoatest1osb02; echo; echo -n "* $srv(`$ROOT/managedServer.sh soa_test osb_server2 fru3324e status | awk '{print $2}'`)"

 

      echo;echo "_____________________________"

      srv=`uname -n`; echo; echo -n "* ohs_test/component(`$ROOT/componentOhs.sh ohs_test status | awk '{print $2}'`): OHS=`checkUrl \"$srv:7777\"`"

      echo

;;

esac
