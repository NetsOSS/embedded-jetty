Embedded Jetty
=====

Embeddded-jetty is a project for starting fully-embedded jetty servers with no XML (no web.xml).

The project encapsulates Nets standard technologies, but has no mandatory dependencies
that can introduce problems for other projects wishing to integrate with different versions.


Dependency Requirements
-----
Using "EmbeddedJettyBuilder" requires a Jetty8 dependency.

The "EmbeddedSpringBuilder" class requires that your project includes a spring version.

The "EmbeddedSpringWsBuilder" class requires that your project includes a spring-ws version.

The "EmbeddedWicketBuilder" class requires that your project includes a wicket version.

Sample code
------
For sample usage, look at https://vm-psource-4/embedded-jetty/embedded-jetty-sample


Governance/commit rights
=======
embedded-jetty is run as an internal open source project at Nets. It uses the "jenkins" governance
model, which means anyone can get commit access, just by asking. Project administators are Steingrim Dovland,
Bjørn Bjerkeli, Pål Stian Bjølseth and Kristian Rosenvold.

Deploying versions is currently done by Kristian, and he will do that upon request: "Version numbers are cheap"

Forking
------
If you want to hack around with this code for you own Nets project, you may also fork it on
gitorious. We'd be really happy for merge requests (pull requests).

Please note that if you fork, it is customary to make 1 single commit that changes the groupid/artifactIid of the fork,
to make it easy to rebase/merge your fork with the original.


