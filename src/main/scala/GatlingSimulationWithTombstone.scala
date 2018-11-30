
import java.util

import com.datastax.driver.core._
import com.datastax.driver.core.policies._
import com.datastax.driver.dse.DseCluster
import com.datastax.gatling.plugin.DsePredef._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation

import scala.concurrent.duration.DurationInt


class GatlingSimulationWithTombstone extends Simulation {
  println("LOADING")

  def getProperty(value: String, default: String = null): String = {
    System.getProperty(value) match {
      case null => default
      case c => c
    }
  }

  //-Dperf.totalRequestPerSecond=20000
  //-Dperf.rampupDuration=60
  //-Dperf.testDurationSec=100
  //-Dperf.pkNumber=1000000
  //-Dperf.valueNumber=1000000
  //-Dperf.consistency=QUORUM
  //-Dperf.loadBalancingPolicy=RoundRobinPolicy or DCAwareRoundRobinPolicy
  //-Dperf.port=9142
  //-Dperf.withSSL=9142
  //-Dperf.keyspace=test
  //-Dperf.username=
  //-Dperf.password=
  //-Dperf.contactPoint=
  //-Dperf.maxRequestPerConnection=10000
  //-Dperf.connectionPerHost=16

  val consistency = ConsistencyLevel.valueOf(getProperty("perf.consistency", "QUORUM"))
  val contactPoint = getProperty("perf.contactPoint", "localhost")
  val port = getProperty("perf.port", "9042").toInt
  val username = getProperty("perf.username")
  val password = getProperty("perf.password")
  val connectionPerHost = getProperty("perf.connectionPerHost", "16").toInt
  val maxRequestPerConnection = getProperty("perf.maxRequestPerConnection", "10000").toInt
  val withSSL = getProperty("perf.withSSL", "false").toBoolean
  val roundRobinPolicy = getProperty("perf.loadBalancingPolicy", "RoundRobinPolicy") match {
    case "RoundRobinPolicy" => new TokenAwarePolicy(new RoundRobinPolicy())
    case "DCAwareRoundRobinPolicy" => new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().build())
  }
  val retryPolicy = getProperty("perf.retryPolicy", "Downgrading") match {
    case "Downgrading" => DowngradingConsistencyRetryPolicy.INSTANCE
    case "Default" => DefaultRetryPolicy.INSTANCE
  }
  val totalRequestPerSecond = getProperty("perf.totalRequestPerSecond", "30000").toInt
  val rampupDuration = getProperty("perf.rampupDuration", "30").toInt
  val testDurationSec = getProperty("perf.testDurationSec", "100000").toInt

  println(s"opening connection to $contactPoint:$port - SSL=$withSSL, with CL=$consistency with $connectionPerHost per host / $maxRequestPerConnection")


  val speculativeExecutionPolicy = new ConstantSpeculativeExecutionPolicy(500, 1)

  val builder = DseCluster.builder()
    .addContactPoints(contactPoint)
    .withQueryOptions(new QueryOptions().setConsistencyLevel(consistency).setDefaultIdempotence(true))
    .withRetryPolicy(retryPolicy)
    .withLoadBalancingPolicy(roundRobinPolicy)
    .withSpeculativeExecutionPolicy(speculativeExecutionPolicy)
    .withPoolingOptions(new PoolingOptions()
      .setConnectionsPerHost(HostDistance.LOCAL, connectionPerHost, connectionPerHost)
      .setMaxRequestsPerConnection(HostDistance.LOCAL, maxRequestPerConnection)
      .setConnectionsPerHost(HostDistance.REMOTE, connectionPerHost, connectionPerHost)
      .setMaxRequestsPerConnection(HostDistance.REMOTE, maxRequestPerConnection))
    .withPort(port)


  if (withSSL) {
    //  add in bin/gatling.sh:
    // -Djavax.net.ssl.trustStore=/path/to/client.truststore
    //  -Djavax.net.ssl.trustStorePassword=password123
    //  # If you're using client authentication:
    //  -Djavax.net.ssl.keyStore=/path/to/client.keystore
    //  -Djavax.net.ssl.keyStorePassword=password123
    builder.withSSL()
  }
  if (username != null) {
    builder.withCredentials(username, password)
  }
  val cluster = builder.build()

  val ks = getProperty("perf.keyspace", "test")

  val session = cluster.connect()

  session.execute(s"""CREATE KEYSPACE IF NOT EXISTS $ks WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 }""")

  session.execute(
    s"""CREATE TABLE IF NOT EXISTS $ks.test  (
    id text PRIMARY KEY,
    data text
  ) """)


  val insertStatement = session.prepare(s"""INSERT INTO $ks.test (id, data) VALUES (:id, :data)  """)

  val random = new util.Random
  val subset: Array[Char] = "0123456789abcdefghijklmnopqrstuvwxyzAZERTYUIOPMLKJHGFDSQWXCVBN".toCharArray

  def getRandomStr(length: Int): String = {
    val buf = new Array[Char](length)
    for (i <- 0 to buf.length - 1) {
      val index = random.nextInt(subset.length)
      buf(i) = subset(index)
    }
    new String(buf)
  }

  val feederWithTombstone = csv("file.csv").random.convert{case (_, string :String) => if(string.isEmpty) null else string}


  val sc_1 = scenario("test").repeat(1) {
    feed(Iterator.continually({
      Map("id" -> getRandomStr(2),
        "data" -> null) //null will create tombstone
    })).exec(cql("test insert").executeStatement(insertStatement)
      .withSessionParams()
    )
  }

  println(s"start test with $totalRequestPerSecond totalRequestPerSecond, rampupDuration = $rampupDuration, testDurationSec = $testDurationSec ")

  setUp(
    sc_1.inject(rampUsersPerSec(1) to totalRequestPerSecond during (rampupDuration seconds), constantUsersPerSec(totalRequestPerSecond) during (testDurationSec seconds))
  ).protocols(dseProtocolBuilder.session(session))
}
