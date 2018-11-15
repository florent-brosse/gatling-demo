# CQL gatling demo

## To clone the repo and remove git 

```
git clone --depth=1 --branch=master https://github.com/florent-brosse/gatling-demo gatling-myproject
rm -rf !$/.git
```

## To launch the performance test

`./mvnw -Dperf.totalRequestPerSecond=20 -Dperf.rampupDuration=1 -Dperf.testDurationSec=3 exec:java -Dexec.mainClass="io.gatling.app.Gatling" -Dexec.args="-s GatlingSimulation"`

or compile the uber jar

`./mvnw clean install`

and run

`java -cp target/cql-gatling-1.0-SNAPSHOT-jar-with-dependencies.jar -Dperf.totalRequestPerSecond=20 -Dperf.rampupDuration=10 -Dperf.testDurationSec=30 io.gatling.app.Gatling -s GatlingSimulation`

or 

`java -Dperf.totalRequestPerSecond=20 -Dperf.rampupDuration=1 -Dperf.testDurationSec=3 -jar target/cql-gatling-1.0-SNAPSHOT-jar-with-dependencies.jar -s GatlingSimulation`

To launch it in IntelliJ:

![Screenshot](IntelliJ.png?raw=true "screenshot")
