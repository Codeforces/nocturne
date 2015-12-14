Nocturne is a lightweight MVC web framework. It supports code hot-swap out of the box. I believe that it dramatically
speeds up development process. You can make a any change in template, controller or service code and just press F5 in
your browser. Moreover using together with http://code.google.com/p/jacuzzi you can change the datasource layer of
application: create or modify domain objects, DAOs and so on. In this case you can change any line of code and view the
result immediately without redeploy.

Currently the framework is in development, but it is in stable state and you can try it. Latest snapshot is 1.3.3-SNAPSHOT.

How to start? Just install nocturne-archetype running "mvn install" in the tools/nocturne-archetype directory. After it
you can create simple nocturne project typing

~~~~~
    mvn archetype:create -DarchetypeGroupId=org.nocturne.archetypes \
        -DarchetypeArtifactId=nocturne-archetype -DarchetypeVersion=1.3.3-SNAPSHOT \
        -DgroupId=?  -DartifactId=?
~~~~~

Use the following maven settings to try current nocturne build:

~~~~~
        <dependency>
            <groupId>org.nocturne</groupId>
            <artifactId>nocturne</artifactId>
            <version>1.3.3-SNAPSHOT</version>
        </dependency>
~~~~~
