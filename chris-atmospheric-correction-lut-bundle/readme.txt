This module provides the lookup table for the CHRIS/Proba atmospheric
correction. In order to deploy this module

1. Place the file 'chrisbox-ac-lut-formatted-1nm.img' into the proper
   resource directory

2. Run 'mvn deploy' from the module directory:

   mvn deploy

   Define an alternative repository, when the repository specified in
   the <distributuionManagement> is not available:

   -DaltDeploymentRepository=bc::default::file:///Volumes/fs1/pub/webservers/www.brockmann-consult.de/mvn/os

   and then 'trigger' synchronisation of the web server.