#!/bin/sh

if [ -z $JBOSS_HOME  ]; then
  echo "You need to set JBOSS_HOME env variable."
  exit 1
fi

cp -rp `dirname $0`/ear/target/jboss-script-service-ear $JBOSS_HOME/standalone/deployments/jboss-script-service.ear
touch $JBOSS_HOME/standalone/deployments/jboss-script-service.ear.dodeploy
