/*
 *    Copyright 2016 Achim Nierbeck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import de.heikoseeberger.sbtheader.license.Apache2_0
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import sbt.Keys._
import sbt._
import sbtassembly.MergeStrategy
//import Docker.autoImport.exposedPorts

/**
 * root build.sbt
 */

//needed for crosscompilation ...
autoCompilerPlugins := true

fork in run := true

//local dependency for sbt itself
libraryDependencies += "org.apache.spark" %% "spark-core" % Version.spark % "provided"

//used for aether-deploy
overridePublishBothSettings
enablePlugins(SignedAetherPlugin)
overridePublishSignedSettings

lazy val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-Xcheckinit"
)

resolvers in ThisBuild += "Apache Snapshots" at "https://repository.apache.org/snapshots/"
resolvers in ThisBuild += Resolver.bintrayRepo("hseeberger", "maven")
resolvers in ThisBuild += Resolver.mavenLocal

// exclude Scala library from assembly
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

//noinspection ScalaStyle
lazy val commonDependencies = Seq(
  CommonDependencies.scala_test,
  CommonDependencies.joda_time,
  CommonDependencies.akka_chill,

  // Fast Java Serializer
  CommonDependencies.fast_serializer

).map(_. excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "com.github.jnr"),
  ExclusionRule(organization = "org.ow2.asm"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "io.netty", artifact = "netty-all")
))

lazy val kafkaDependencies = Seq(
  KafkaDependencies.kafka,
  KafkaDependencies.kafka_clients
).map(_. excludeAll(
  ExclusionRule(organization = "log4j")
))

//noinspection ScalaStyle
lazy val akkaDependencies = Seq(
  AkkaDependencies.scala_lang_modules,
  AkkaDependencies.akka_actor,
  AkkaDependencies.akka_slf4j,
  AkkaDependencies.akka_testkit,

  // these are to avoid sbt warnings about transitive dependency conflicts
  AkkaDependencies.akka_http,
  AkkaDependencies.akka_stream_kafka,
  AkkaDependencies.akka_http_json4s,
  AkkaDependencies.json4s_jackson,
  AkkaDependencies.cassandra_driver_core
).map(_. excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "io.netty", artifact = "netty-all")
))

//noinspection ScalaStyle
lazy val sparkDependencies = Seq(
  SparkDependencies.spark_cassandra_connector,
  SparkDependencies.spark_streaming_kafka,
  SparkDependencies.spark_core,
  SparkDependencies.spark_streaming,
  SparkDependencies.spark_catalyst,
  SparkDependencies.spark_sql,
  SparkDependencies.spark_mllib,
  SparkDependencies.scalanlp
).map(_.excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "org.ow2.asm", artifact = "asm-util"),
  ExclusionRule(organization = "com.github.jnr"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "org.spark-project"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "io.netty", artifact = "netty-all")
))

val flinkDependencies = Seq(
  (FlinkDependencies.flink_core)
    .exclude("com.esotericsoftware.kryo", "kryo"),
  FlinkDependencies.flink_scala,
  (FlinkDependencies.flink_streaming_java)
    .exclude("com.esotericsoftware.kryo", "kryo"),
  (FlinkDependencies.flink_streaming_scala)
    .exclude("com.esotericsoftware.kryo", "kryo")
    .exclude("io.netty", "netty-all"),
  FlinkDependencies.flink_connector_kafka,
  FlinkDependencies.flink_connector_cassandra,
  (FlinkDependencies.flink_client)
    .exclude("com.esotericsoftware.kryo", "kryo"),
  FlinkDependencies.metrics_core
).map(_.excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "org.spark-project"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "com.esotericsoftware.kryo", artifact = "kryo")
))

//noinspection ScalaStyle
lazy val logDependencies = Seq(
  LogDependencies.log4j12_api,
  LogDependencies.slf4j,
  LogDependencies.log4j_api,
  LogDependencies.log4j_core,
  LogDependencies.jcl,
  LogDependencies.jul
)

lazy val akkaHttpDependencies = Seq(
  AkkaDependencies.akka_http,
  "com.typesafe.akka"               %% "akka-http-testkit-experimental" % "2.4.2-RC3"
)

/*
lazy val vertxDependencies = Seq(
  VertxDependencies.vertx_lang_scala,
  VertxDependencies.vertx_web,
  VertxDependencies.vertx_web_client,
  //required to get rid of some warnings emitted by the scala-compile
  VertxDependencies.vertx_codegen,
  CommonDependencies.scalatest_embedded_cassandra,
  VertxDependencies.vertx_cassandra,
  CommonDependencies.scala_xml
)
*/

lazy val commonSettings = Seq(
  organization := "de.nierbeck.floating.data",
  scalacOptions ++= compileOptions,
  parallelExecution in Test := true,
  logBuffered in Test := false,
  libraryDependencies ++= commonDependencies,
  libraryDependencies ++= logDependencies,

  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
      <scm>
        <url>git@github.com:ANierbeck/BusFloatingData.git</url>
        <connection>scm:git:git@github.com:ANierbeck/BusFloatingData.git</connection>
      </scm>
      <developers>
        <developer>
          <id>ANierbeck</id>
          <name>Achim Nierbeck</name>
        </developer>
      </developers>
    ),

  licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")),

  homepage := Some(url("https://github.com/ANierbeck/BusFloatingData")),

  dockerRepository := Some("anierbeck"),

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val root = (project in file(".")).
  enablePlugins(GitVersioning).
  settings(commonSettings: _*).
  settings(
    name := "BusFloatingData",
    scalaVersion := Version.scalaVer
  ).
  aggregate(commons, ingest, sparkDigest, akkaServer, flinkDigest)

lazy val commons = (project in file("commons")).
  enablePlugins(AutomateHeaderPlugin).
  settings(commonSettings: _*).
  settings(
    name := "commons",
    scalaVersion := Version.scalaVer,
    libraryDependencies ++= kafkaDependencies,
    crossScalaVersions := Seq(Version.scalaVer, Version.scalaVertxVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  )

lazy val ingest = (project in file("akka-ingest")).
  enablePlugins(JavaAppPackaging, AutomateHeaderPlugin).
  settings(commonSettings: _*).
  settings(
    name := "akka-ingest",
    scalaVersion := Version.scalaVer,
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= kafkaDependencies,
    mainClass in (Compile,run) := Some("de.nierbeck.floating.data.stream.StreamToKafkaApp"),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)

lazy val sparkDigest = (project in file("spark-digest")).
  enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, UniversalPlugin).
  settings(commonSettings: _*).
  settings(
    name := "spark-digest",
    libraryDependencies ++= sparkDependencies,
    libraryDependencies ++= kafkaDependencies,
    scalaVersion := Version.scalaVer,
    mainClass in (run) := Some("de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp"),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "log4j", "spi", xs @_ * ) => MergeStrategy.first
      case PathList("org", "apache", "log4j", "xml", xs @_ * ) => MergeStrategy.first
      case PathList("org", "slf4j", "impl", xs @_ * ) => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("", "create_table.cql") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.last
      case PathList("org", "apache", "spark", xs @ _ *) => MergeStrategy.first
      case PathList("org", "apache", "commons", xs @_ *) => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
      case PathList("akka", xs @_ * ) => MergeStrategy.discard
      case PathList("scala", xs @_ * ) => MergeStrategy.discard
      case PathList("assets", xs @_ * ) => MergeStrategy.discard
      case PathList("darwin", xs @_ * ) => MergeStrategy.discard
      case PathList("jline", xs @_ * ) => MergeStrategy.discard
      case PathList("junit", xs @_ * ) => MergeStrategy.discard
      case PathList("linux", xs @_ * ) => MergeStrategy.discard
      case PathList("win32", xs @_ * ) => MergeStrategy.discard
      case PathList("webapps", xs @_ * ) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  ).dependsOn(commons)

lazy val akkaServer = (project in file("akka-server")).
  settings(commonSettings: _*).
  enablePlugins(JavaAppPackaging).
  enablePlugins(AutomateHeaderPlugin).
  settings(
    name := "akka-server",
    scalaVersion := Version.scalaVer,
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= akkaHttpDependencies,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies += "com.lambdaworks" %% "jacks" % "2.5.2",
    crossScalaVersions := Seq(Version.scalaVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)

lazy val flinkDigest = (project in file("flink-digest")).
  settings(commonSettings: _*).
  enablePlugins(JavaAppPackaging).
  enablePlugins(AutomateHeaderPlugin).
  settings(
    name := "flink-digest",
    fork in run := true,
    scalaVersion := Version.scalaVer,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies ++= flinkDependencies,
    mainClass in (run) := Some("de.nierbeck.floating.data.stream.flink.KafkaToCassandraFlinkApp"),
    crossScalaVersions := Seq(Version.scalaVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    ),
    assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      cp.filter(_.data.getName == "log4j-1.2.17.jar")
    },
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "log4j", "spi", xs @_ * ) => MergeStrategy.first
      case PathList("org", "apache", "log4j", "xml", xs @_ * ) => MergeStrategy.first
      case PathList("org", "slf4j", "impl", xs @_ * ) => MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("", "create_table.cql") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.last
      case PathList("com", "datastax", "driver", xs @ _ *) => MergeStrategy.first
      case PathList("com", "datastax", "driver", "mapping", xs @ _ *) => MergeStrategy.first
      case PathList("com", "datastax", "driver", "core", xs @ _ *) => MergeStrategy.first
      case PathList("org", "apache", "commons", xs @_ *) => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
      case PathList("com", "sun", xs @_ * ) => MergeStrategy.discard
      case PathList("akka", xs @_ * ) => MergeStrategy.discard
      case PathList("scala", xs @_ * ) => MergeStrategy.discard
      case PathList("assets", xs @_ * ) => MergeStrategy.discard
      case PathList("darwin", xs @_ * ) => MergeStrategy.discard
      case PathList("jline", xs @_ * ) => MergeStrategy.discard
      case PathList("junit", xs @_ * ) => MergeStrategy.discard
      case PathList("linux", xs @_ * ) => MergeStrategy.discard
      case PathList("win32", xs @_ * ) => MergeStrategy.discard
      case PathList("webapps", xs @_ * ) => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  ).dependsOn(commons)

/*
lazy val vertxIngest = (project in file("vertx-ingest")).
  settings(commonSettings: _*).
  enablePlugins(JavaAppPackaging, AutomateHeaderPlugin).
  settings(
    name := "vertx-ingest",
    scalaVersion := Version.scalaVertxVer,
    libraryDependencies ++= vertxDependencies,
    crossScalaVersions := Seq(Version.scalaVertxVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)
*/


//create project
//addCommandAlias("create", ";clean ;so commons/compile; so commons/test; so commons/publishLocal; ingest/test ;ingest/publishLocal; akkaServer/test ;akksServer/publishLocal; sparkDigest/test; sparkDigest/publishLocal; so vertxIngest/test; so vertxIngest/publishLocal")
addCommandAlias("create", ";clean ;test ;publishLocal")

//create deployment artefacts for DC/OS system
addCommandAlias("createIngestContainer", "ingest/docker:publishLocal")
addCommandAlias("createDigestUberJar", "sparkDigest/assembly")
addCommandAlias("createServerContainer", "akkaServer/docker:publishLocal")
addCommandAlias("createFlinkUberJar", "flinkDigest/assembly")

//localy run
addCommandAlias("runIngest", "ingest/run")
addCommandAlias("runServer", "akkaServer/run")

addCommandAlias("runFlink", "flinkDigest/run METRO-Vehicles localhost:9042 localhost:9092")

//localy run spark
addCommandAlias("submitKafkaCassandra", "sparkDigest/sparkSubmit --master local[2] --class de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp -- METRO-Vehicles localhost:9042 localhost:9092")
addCommandAlias("submitClusterSpark", "sparkDigest/sparkSubmit --master local[2] --class de.nierbeck.floating.data.stream.spark.CalcClusterSparkApp -- localhost:9042")

addCommandAlias("createAWS", ";clean ;test ;publishLocal ;sparkDigest/assembly ;flinkDigest/assembly ;publish-signed; ingest/docker:publish; akkaServer/docker:publish")
addCommandAlias("publishAll", ";sparkDigest/assembly ;flinkDigest/assembly ;publish-signed; ingest/docker:publish; akkaServer/docker:publish")
