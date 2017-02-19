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
import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPlugin}
import sbt.Keys._
import sbt._
import sbtassembly.MergeStrategy
import sbtrelease.ReleaseStateTransformations._

/**
 * root build.sbt
 */

val akkaVer        = "2.4.10"
val scalaVer       = "2.11.8"
val scalaParsersVer= "1.0.4"
val scalaTestVer   = "2.2.6"
val cassandraVer   = "3.1.2"
val Log4j2         = "2.5"
val Slf4j          = "1.7.18"
val spark          = "2.0.0"
val sparkConnector = "2.0.0-M3"
val circeVersion   = "0.4.1"
val kafkaVersion   = "0.10.0.1"
val flinkVersion   = "1.2.0"

//needed for crosscompilation ...
autoCompilerPlugins := true

fork in run := true

//local dependency for sbt itself
libraryDependencies += "org.apache.spark" %% "spark-core" % spark % "provided"

//used for aether-deploy
overridePublishBothSettings
enablePlugins(SignedAetherPlugin)
overridePublishSignedSettings

lazy val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-Xcheckinit"
)

resolvers in ThisBuild += "Apache Snapshots" at "https://repository.apache.org/snapshots/"
resolvers in ThisBuild += Resolver.bintrayRepo("hseeberger", "maven")
resolvers in ThisBuild += Resolver.mavenLocal

//noinspection ScalaStyle
lazy val commonDependencies = Seq(
  "org.scalatest"            %% "scalatest"                  % scalaTestVer       % "test",
  "joda-time"                %  "joda-time"                  % "2.9.3",
  "com.twitter"              %% "chill-akka"                 % "0.8.0",
  "com.datastax.cassandra"   % "cassandra-driver-core"       % cassandraVer       intransitive(),
  "com.datastax.cassandra"   % "cassandra-driver-mapping"    % cassandraVer,

  // Fast Java Serializer
  "de.ruedigermoeller"       %  "fst"                        % "2.45"

).map(_. excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "com.google.guava", artifact = "guava")
))

lazy val kafkaDependencies = Seq(
  "org.apache.kafka"                %% "kafka"                      % kafkaVersion,
  "org.apache.kafka"                % "kafka-clients"                % kafkaVersion
)

//noinspection ScalaStyle
lazy val akkaDependencies = Seq(
  "org.scala-lang.modules"   %% "scala-parser-combinators"   % scalaParsersVer,
  "com.typesafe.akka"        %% "akka-actor"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-slf4j"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-testkit"               % akkaVer            % "test",

  // these are to avoid sbt warnings about transitive dependency conflicts
  "com.typesafe.akka"               %  "akka-http-experimental_2.11" % "2.0.1",
  "com.typesafe.akka"               %% "akka-stream-kafka"          % "0.12",
  "joda-time"                       %  "joda-time"                  % "2.9.3",
  "de.heikoseeberger"               %% "akka-http-json4s"           % "1.6.0",
  "org.json4s"                      %% "json4s-jackson"             % "3.2.11"
).map(_. excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "javax.jms")
))

//noinspection ScalaStyle
lazy val sparkDependencies = Seq(
  "com.datastax.spark"              %% "spark-cassandra-connector"  % sparkConnector,
  "org.apache.spark"                %% "spark-streaming-kafka-0-10"      % spark,
  "org.apache.spark"                %% "spark-core"                 % spark           % "provided",
  "org.apache.spark"                %% "spark-streaming"            % spark           % "provided",
  "org.apache.spark"                %% "spark-catalyst"             % spark           % "provided",
  "org.apache.spark"                %% "spark-sql"                  % spark           % "provided",
  "org.apache.spark"                %% "spark-mllib"                % spark           % "provided",
  "org.scalanlp"                    %%  "nak"                       % "1.3"
).map(_.excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "org.spark-project"),
  ExclusionRule(organization = "javax.jms")
))

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion           % "provided",
  "org.apache.flink" %% "flink-streaming-java" % flinkVersion           % "provided",
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion           % "provided",
  "org.apache.flink" %% "flink-connector-kafka-0.10" % flinkVersion,
  "org.apache.flink" %% "flink-connector-cassandra" % flinkVersion,
  "org.apache.flink" %% "flink-clients" % flinkVersion           % "provided"
).map(_.excludeAll(
  ExclusionRule(organization = "org.slf4j", artifact = "slf4j-log4j12"),
  ExclusionRule(organization = "com.sun.jdmk"),
  ExclusionRule(organization = "com.sun.jmx"),
  ExclusionRule(organization = "log4j"),
  ExclusionRule(organization = "org.spark-project"),
  ExclusionRule(organization = "javax.jms"),
  ExclusionRule(organization = "com.google.guava", artifact = "guava")
))

//noinspection ScalaStyle
lazy val logDependencies = Seq(
  "org.slf4j"                       % "slf4j-api"                           % Slf4j,
  "org.apache.logging.log4j"        % "log4j-1.2-api"                       % Log4j2,
  "org.apache.logging.log4j"        % "log4j-slf4j-impl"                    % Log4j2,
  "org.apache.logging.log4j"        % "log4j-api"                           % Log4j2,
  "org.apache.logging.log4j"        % "log4j-core"                          % Log4j2,
  "org.slf4j"                       % "jcl-over-slf4j"                      % Slf4j,
  "org.slf4j"                       % "jul-to-slf4j"                        % Slf4j
)

lazy val akkaHttpDependencies = Seq(
  "com.typesafe.akka"               %% "akka-http-experimental"     % akkaVer,
  "com.typesafe.akka"               %% "akka-http-testkit-experimental" % "2.4.2-RC3"
)

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
    scalaVersion := scalaVer
  ).
  aggregate(commons, ingest, sparkDigest, akkaServer, flinkDigest)

lazy val commons = (project in file("commons")).
  enablePlugins(AutomateHeaderPlugin).
  settings(commonSettings: _*).
  settings(
    name := "commons",
    scalaVersion := scalaVer,
    libraryDependencies ++= kafkaDependencies,
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
    scalaVersion := scalaVer,
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
    scalaVersion := scalaVer,
    mainClass in (run) := Some("de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp"),
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
      case PathList("org", "apache", "spark", xs @ _ *) => MergeStrategy.first
      case PathList("org", "apache", "commons", xs @_ *) => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
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
    scalaVersion := scalaVer,
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= akkaHttpDependencies,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVer % "test",
    libraryDependencies += "com.lambdaworks" %% "jacks" % "2.5.2",
    crossScalaVersions := Seq(scalaVer),
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
    scalaVersion := scalaVer,
    libraryDependencies ++= kafkaDependencies,
    libraryDependencies ++= flinkDependencies,
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVer % "test",
    crossScalaVersions := Seq(scalaVer),
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

//create project
addCommandAlias("create", ";clean ;test;publishLocal")

//create deployment artefacts for DC/OS system
addCommandAlias("createIngestContainer", "ingest/docker:publishLocal")
addCommandAlias("createDigestUberJar", "sparkDigest/assembly")
addCommandAlias("createServerContainer", "akkaServer/docker:publishLocal")
addCommandAlias("createFlinkUberJar", "flinkDigest/assembly")

//localy run
addCommandAlias("runIngest", "ingest/run")
addCommandAlias("runServer", "akkaServer/run")

//localy run spark
addCommandAlias("submitKafkaCassandra", "sparkDigest/sparkSubmit --master local[2] --class de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkApp -- METRO-Vehicles localhost 9042 localhost 9092")
addCommandAlias("submitClusterSpark", "sparkDigest/sparkSubmit --master local[2] --class de.nierbeck.floating.data.stream.spark.CalcClusterSparkApp -- localhost 9042 localhost 9092")

addCommandAlias("createAWS", ";clean ;test ;publishLocal ;ingest/docker:publishLocal ;sparkDigest/assembly ;akkaServer/docker:publishLocal")
addCommandAlias("publishAll", ";sparkDigest/assembly ;publish-signed; ingest/docker:publish; akkaServer/docker:publish")
