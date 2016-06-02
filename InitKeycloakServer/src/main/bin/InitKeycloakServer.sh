#!/bin/sh

scriptPos=${0%/*}

if [ -z "$TESTADMINCON_HOME" -o ! -d "$TESTADMINCON_HOME" ] ; then
  ## resolve links - $0 may be a link to the home
  PRG="$0"
  progname=`basename "$0"`

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
    else
    PRG=`dirname "$PRG"`"/$link"
    fi
  done

  TESTADMINCON_HOME=`dirname "$PRG"`

  # make it fully qualified
  TESTADMINCON_HOME=`cd "$TESTADMINCON_HOME" > /dev/null && pwd`
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      JAVACMD="$JAVA_HOME/jre/sh/java"
    elif [ -x "$JAVA_HOME/jre/bin/java" ] ; then
      JAVACMD="$JAVA_HOME/jre/bin/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java 2> /dev/null `
    if [ -z "$JAVACMD" ] ; then
        JAVACMD=java
    fi
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

args=
for arg in "$@";
do
  args="$args \"$arg\""
done

exec_command="exec \"$JAVACMD\" -cp \"$TESTADMINCON_HOME/lib/*\" \"-Dlogback.configurationFile=$TESTADMINCON_HOME/conf/logback.xml\" de.oth.keycloak.InitKeycloakServer"
eval $exec_command $args
