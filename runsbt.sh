#!/bin/bash

if [ $# -lt 1 ];
then
  echo ">>>"
  echo ">>> You forget to specify the environment 'd', 'h' or 'p' ('dev', 'hml' or 'prd') ?"
  echo ">>>"
else
	case $1 in
	
		d|dev)
			echo -e "\n******************* NOTICE *******************"
			echo "   I will run in development"
			echo -e "**********************************************\n"
			export RSP_API_SERVER_ENV=dev
			source ./environment.sh
			./sbt
			;;
	
		h|hml)
			echo -e "\n******************* NOTICE *******************"
			echo "   I will run in homologation"
			echo -e "**********************************************\n"
			export RSP_API_SERVER_ENV=hml
			source ./environment.sh
			./sbt
			;;
	
		p|prd)
			echo -e "\n******************* NOTICE *******************"
			echo "   I will run in production"
			echo -e "**********************************************\n"
			export RSP_API_SERVER_ENV=prd
			source ./environment.sh
			./sbt
			;;
	
		*)
			echo -e "\n******************************** NOTICE ********************************"
			echo "   I will run in development, because I dont know the environment '$1'"
			echo -e "************************************************************************\n"
			export RSP_API_SERVER_ENV=dev
			source ./environment.sh
			./sbt
			;;
	esac
fi

