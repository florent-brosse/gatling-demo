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


For this insert query:

`val insertStatement = session.prepare(s"INSERT INTO $ks.test (id, data) VALUES (:id, :data)")`

These feeders will not create tombstone
```
Iterator.continually({
    Map("id" -> getRandomStr(2)) //skip data will not create tombstone
})
// or
val feederWithTombstone = csv("file.csv").random.convert{case (_, string :String) => if(string.isEmpty) null else string}
val feederWithoutTombstone = feederWithTombstone.copy(
    records = feederWithTombstone.records.map(row => row.filter { case (key, value) => value != null})
  )
//or
val feeder = csv("file.csv")
val feederWithoutTombstone = feeder.copy(
    records = feeder.records.map(row => row.filter { case (key, value :String) => !value.isEmpty})
)
```

This feeder will do create tombstone
```
Iterator.continually({
    Map("id" -> getRandomStr(2),
    "data" -> null) //null will create tombstone
})
// or
val feederWithTombstone = csv("file.csv").random.convert{case (_, string :String) => if(string.isEmpty) null else string}
```