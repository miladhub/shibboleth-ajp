Sample Shibboleth integration via AJP
===

This sample servlet-based Calculator shows how to configure a Shibboleth SP for SAML
SSO, using [environment variables](https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2065335257/AttributeAccess#AttributeAccess-ServerVariables) instead of HTTP headers to pass information to the
app. This is the recommended solution for security reasons, see
<https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2065335062/Apache>, parameters
`ShibUseEnvironment` and `ShibUseHeaders` for more information.

The AJP protocol in conjunction with the Apache HTTPD module [mod_proxy_ajp](https://httpd.apache.org/docs/2.4/mod/mod_proxy_ajp.html)
is the recommended way to send environment variables to a Java app, see
<https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2067400159/JavaHowTo>. Here we use Apache 2.4, where the `mod_proxy_ajp` is installed by default.

Status
===

The AJP integration is currently working on JBoss EAP 7.3.0, but it does *not* work
on neither Wildfly 24.0.1 nor JBoss EAP 7.4.0.

How it works
===

Shibboleth reads attributes from the SAML claims via `attribute-map.xml`, for example:

```xml
<Attribute name="uid" nameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:basic" id="uid" />
```

These attributes are then placed on Apache "environment variables" via the `ShibUseEnvironment`
directive, which is `On` by default, see
<https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2065335062/Apache>.

These variables are prefixed by the "AJP_" string (configured in Shibboleth's `shibboleth2.xml`) so that
the `mod_proxy_ajp` Apache module can put them as `HttpServletRequest` attributes (rather than headers),
without the prefix (see <https://httpd.apache.org/docs/2.4/mod/mod_proxy_ajp.html#env>).

At this point, the servlet can read the `uid` attribute from the incoming request:

```java
request.getAttribute("uid")
```

In this sample project, the JavaScript code displays the `uid` on the page:

```javascript
document.getElementById("uid").innerHTML = json.uid;
```

Using WildFly
===

[Download WildFly 24.0.1 Final](https://download.jboss.org/wildfly/24.0.1.Final/wildfly-24.0.1.Final.zip)
and extract it to `~/wildfly-24.0.1.Final/`.

Issue the following command from a CLI tab:

```bash
export JBOSS_HOME=~/wildfly-24.0.1.Final && $JBOSS_HOME/bin/standalone.sh
```

Issue the following command from another tab:

```bash
export JBOSS_HOME=~/wildfly-24.0.1.Final && $JBOSS_HOME/bin/jboss-cli.sh
[disconnected /] connect
[standalone@localhost:9990 /] /socket-binding-group=standard-sockets/socket-binding=ajp:add(port=8009)
{
    "outcome" => "failed",
    "failure-description" => "WFLYCTL0212: Duplicate resource [
    (\"socket-binding-group\" => \"standard-sockets\"),
    (\"socket-binding\" => \"ajp\")
]",
    "rolled-back" => true
}

[standalone@localhost:9990 /] /subsystem=undertow/server=default-server/ajp-listener=myListener:add(socket-binding=ajp, scheme=http, enabled=true)
{"outcome" => "success"}
```

Build and deploy the servlet:

```bash
export JBOSS_HOME=~/wildfly-24.0.1.Final && mvn clean install && cp target/mycalcwebapp.war $JBOSS_HOME/standalone/deployments
```

Using JBoss EAP
===

[Download JBoss EAP 7.3.0](https://developers.redhat.com/content-gateway/file/jboss-eap-7.3.0.zip)
and extract it to `~/jboss-eap-7.3/`.

Issue the following command from a CLI tab:

```bash
export JBOSS_HOME=~/jboss-eap-7.3 && $JBOSS_HOME/bin/standalone.sh
```

Issue the following command from another tab:

```bash
export JBOSS_HOME=~/jboss-eap-7.3 && $JBOSS_HOME/bin/jboss-cli.sh
[disconnected /] connect
[standalone@localhost:9990 /] /socket-binding-group=standard-sockets/socket-binding=ajp:add(port=8009)
{
    "outcome" => "failed",
    "failure-description" => "WFLYCTL0212: Duplicate resource [
    (\"socket-binding-group\" => \"standard-sockets\"),
    (\"socket-binding\" => \"ajp\")
]",
    "rolled-back" => true
}

[standalone@localhost:9990 /] /subsystem=undertow/server=default-server/ajp-listener=myListener:add(socket-binding=ajp, scheme=http, enabled=true)
{"outcome" => "success"}
```

Build and deploy the servlet:

```bash
export JBOSS_HOME=~/jboss-eap-7.3 && mvn clean install && cp target/mycalcwebapp.war $JBOSS_HOME/standalone/deployments
```

## Creating the server private key and SSL certificate

Create the private key `localhost.key` and the self-signed SSL certificate `localhost.crt` using
[mkcert](https://github.com/FiloSottile/mkcert):

```bash
mkcert --install
mkcert localhost
openssl x509 -outform der -in localhost.pem -out localhost.crt
openssl pkey -in localhost-key.pem -out localhost.key
```

# Installing the SAML IdP and SP

```bash
docker rm -f test-idp
docker run --name=test-idp \
    -p 28080:8080 \
    -p 28443:8443 \
    -e SIMPLESAMLPHP_SP_ENTITY_ID=https://sp.example.org/shibboleth \
    -e SIMPLESAMLPHP_SP_ASSERTION_CONSUMER_SERVICE=https://localhost/Shibboleth.sso/SAML2/POST \
    -e SIMPLESAMLPHP_SP_SINGLE_LOGOUT_SERVICE=https://localhost/Shibboleth.sso/Logout \
    -d kristophjunge/test-saml-idp
docker cp authsources.php test-idp:/var/www/simplesamlphp/config/authsources.php
docker cp localhost.pem test-idp:/etc/ssl/cert/cert.crt
docker cp localhost.key test-idp:/etc/ssl/private/private.key
docker restart test-idp

docker rm -f shib-sp
docker run -dit --name shib-sp -p 80:80 -p 443:443 unicon/shibboleth-sp:3.0.4
curl https://localhost:28443/simplesaml/saml2/idp/metadata.php -o idp-metadata.xml -k
docker cp idp-metadata.xml shib-sp:/etc/shibboleth/
docker cp shibboleth2.xml shib-sp:/etc/shibboleth/
docker cp attribute-map.xml shib-sp:/etc/shibboleth/
docker cp localhost.crt shib-sp:/etc/pki/tls/certs/
docker cp localhost.key shib-sp:/etc/pki/tls/private/
docker cp app.conf shib-sp:/etc/httpd/conf.d/
docker restart shib-sp
```

Accessing the app
===

Open up <https://localhost/mycalcwebapp/>. If it works, the current user id is shown at the bottom of the page.

References
===

* <https://devcenter.heroku.com/articles/ssl-certificate-self>
* <https://hub.docker.com/_/httpd>
* <https://docs.docker.com/docker-for-mac/networking/> (`host.docker.internal`)
* <https://docs.wildfly.org/17/Admin_Guide.html>
* <https://access.redhat.com/solutions/3974141>
* <https://stackoverflow.com/questions/14424142/how-can-i-read-apache-httpd-env-variables-from-a-java-application-running-in-tom>
* <https://httpd.apache.org/docs/current/env.html>
* <https://httpd.apache.org/docs/current/mod/mod_headers.html>
* <https://httpd.apache.org/docs/2.4/mod/mod_proxy_ajp.html#env>
* <https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2067400159/JavaHowTo>
* <https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2063695997/ApplicationDefaults>
* <https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2065335257/AttributeAccess#AttributeAccess-CustomSPVariables>
* <https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2065335062/Apache>
* <https://shibboleth.atlassian.net/wiki/spaces/SP3/pages/2065334444/AssertionAttributeExtractor>