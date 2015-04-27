jboss-script-service
====================

Do you remember bsh deployer service? Now we are JSR-223, javax.script!

Usage
-----

Deploy ear/target/jboss-script-service.ear to $JBOSS_HOME/standalone/deployments directory and boot JBoss AS 7 / WildFly.

You'll see $JBOSS_HOME/standalone/scripts directory created and you can deploy any scripts to this directory.

Example
-------

hello.js:

    java.lang.System.out.println("Hello JavaScript!")

hello.rb:

    puts "Hello JRuby!"

How to add other language support
---------------------------------

In ear/pom.xml, add dependency and declare jarModule in ear plugin. There is already JRuby defined as a working example.

TODO
----

* Allow "service" style scripts that support lifecycle methods and expose as JMX MBean
* Make some useful container objects like MBeanServer available to scripts automatically
