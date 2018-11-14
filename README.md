# CQL gatling demo

## Launch the performance test
./mvnw clean install

java -cp target/cql-gatling-1.0-SNAPSHOT-jar-with-dependencies.jar -Dperf.totalRequestPerSecond=20 -Dperf.rampupDuration=10 -Dperf.testDurationSec=30 io.gatling.app.Gatling -s GatlingSimulation
