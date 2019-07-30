# docker-wait-for-oracle

This is an implementation of https://docs.docker.com/compose/startup-order/ for Oracle.

It tries to connect to a user account and execute SELECT 1 FROM DUAL.

 
## Usage :

```
usage: cmd
    --password <arg>      jdbc password
    --retryPeriod <arg>   period to retry in ms
    --timeout <arg>       timeout in ms. 0 = infinite retry
    --url <arg>           jdbc url
    --username <arg>      jdbc username
```


The wait-for-it script and wait-for-oracle-1.0-SNAPSHOT-executable.jar have been copied within the Dockerfile


## Build

There is no public distribution of oracle jdbc driver, so, in order to build this project, you need to find, download & install the oracle jdbc driver in your local repoistory :

```
mvn install:install-file -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.2.0 -Dpackaging=jar -Dfile=ojdbc6.jar -DgeneratePom=true
```

## Usage in Dockerfile and/or Docker compose
```dockerfile
COPY target/wait-for-oracle-1.0-SNAPSHOT-executable.jar $CATALINA_HOME
```

Exemple of usage into docker-compose that mix wait for it on ActiveMq, port 61616 and oracle onto 1521 :

> the wait-for-it script must be copied first.

```yaml
  ile1:
    build: web-server
    environment:
      - JPDA_ADDRESS=8000
      - JPDA_TRANSPORT=dt_socket
    ports:
     - 8080:8080
    depends_on:
      - database
      - amq
    command: >
      sh -c "
        java -jar wait-for-oracle-1.0-SNAPSHOT-executable.jar --url jdbc:oracle:thin:@database:1521/DB11G --username schema_user --password schema_pwd &&
        ./wait-for-it.sh -t 0 amq:61616 &&
        catalina.sh jpda run"
```