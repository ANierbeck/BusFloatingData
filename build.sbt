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

/**
 * root build.sbt
 */

val akkaVer        = "2.4.3"
val logbackVer     = "1.1.3"
val scalaVer       = "2.11.8"
val scalaParsersVer= "1.0.4"
val scalaTestVer   = "2.2.6"
val cassandraVer   = "3.0.1"
val Slf4j          = "1.7.18"
val spark          = "1.6.0"
val sparkConnector = "1.5.0"
val circeVersion   = "0.4.1"

//needed for crosscompilation ...
autoCompilerPlugins := true

fork in run := true

lazy val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-Xcheckinit"
)

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

//noinspection ScalaStyle
lazy val commonDependencies = Seq(
  "ch.qos.logback"           %  "logback-classic"            % logbackVer,
  "org.scalatest"            %% "scalatest"                  % scalaTestVer       % "test",
  "joda-time"                %  "joda-time"                  % "2.9.3",
  "com.twitter"              %% "chill-akka"                 % "0.8.0",

  // datastax cassandra driver
  "com.datastax.cassandra"   % "cassandra-driver-core"       % cassandraVer,
  "de.ruedigermoeller"       %  "fst"                        % "2.45"
)

//noinspection ScalaStyle
lazy val akkaDependencies = Seq(
  "org.scala-lang.modules"   %% "scala-parser-combinators"   % scalaParsersVer,
  "com.typesafe.akka"        %% "akka-actor"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-slf4j"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-testkit"               % akkaVer            % "test",

  // these are to avoid sbt warnings about transitive dependency conflicts
  "com.typesafe.akka"               %  "akka-http-experimental_2.11" % "2.0.1",
  "com.typesafe.akka"               %% "akka-stream-kafka"          % "0.11-M2",
  "org.apache.kafka"                %% "kafka"                      % "0.9.0.1",
  "joda-time"                       %  "joda-time"                  % "2.9.3",
  "de.heikoseeberger"               %% "akka-http-json4s"           % "1.6.0",
  "org.json4s"                      %% "json4s-jackson"             % "3.2.11"
)

//noinspection ScalaStyle
lazy val sparkDependencies = Seq(
  "com.datastax.spark"              %% "spark-cassandra-connector"  % sparkConnector,
  "org.apache.spark"                %% "spark-streaming-kafka"      % spark,
  "org.apache.spark"                %% "spark-core"                 % spark,
  "org.apache.spark"                %% "spark-streaming"            % spark,
  "org.apache.spark"                %% "spark-streaming-kafka"      % spark,
  "org.apache.spark"                %% "spark-catalyst"             % spark,
  "org.apache.spark"                %% "spark-sql"                  % spark,
  "org.apache.spark"                %% "spark-mllib"                % spark,
  "org.scalanlp"                    %%  "nak"                       % "1.3"
)

//noinspection ScalaStyle
lazy val logDependencies = Seq(
  "org.slf4j"                       % "slf4j-api"                   % Slf4j,
  "org.slf4j"                       % "jcl-over-slf4j"              % Slf4j,
  "org.slf4j"                       % "jul-to-slf4j"                % Slf4j
)

lazy val akkaHttpDependencies = Seq(
  "com.typesafe.akka"               %% "akka-http-experimental"     % akkaVer,
  "com.typesafe.akka"               %% "akka-http-testkit-experimental" % "2.4.2-RC3"
)

lazy val commonSettings = Seq(
  organization := "de.nierbeck.floating.data",
  version := "0.1.0-SNAPSHOT",
  scalacOptions ++= compileOptions,
  parallelExecution in Test := true,
  logBuffered in Test := false,
  libraryDependencies ++= commonDependencies,
  libraryDependencies ++= logDependencies
)

lazy val root = (project in file(".")).
  enablePlugins(GitVersioning).
  settings(commonSettings: _*).
  settings(
    name := "BusFloatingData",
    scalaVersion := scalaVer
  ).
  aggregate(commons, ingest, akkaDigest, sparkDigest, akkaServer)

lazy val commons = (project in file("commons")).
  enablePlugins(AutomateHeaderPlugin).
    settings(commonSettings: _*).
  settings(
    name := "commons",
    scalaVersion := scalaVer,
    libraryDependencies += "org.apache.kafka" %% "kafka" % "0.9.0.1" % "provided",
    crossScalaVersions := Seq("2.10.5", scalaVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  )

lazy val ingest = (project in file("akka-ingest")).
  enablePlugins(JavaAppPackaging).
  enablePlugins(AutomateHeaderPlugin).
  settings(commonSettings: _*).
  settings(
    name := "akka-ingest",
    scalaVersion := scalaVer,
    libraryDependencies ++= akkaDependencies,
    crossScalaVersions := Seq(scalaVer),
    mainClass in (Compile,run) := Some("de.nierbeck.floating.data.stream.StreamToKafkaApp"),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)

lazy val akkaDigest = (project in file("akka-digest")).
  enablePlugins(JavaAppPackaging).
  enablePlugins(AutomateHeaderPlugin).
  settings(commonSettings: _*).
  settings(
    name := "akka-digest",
    scalaVersion := scalaVer,
    libraryDependencies ++= akkaDependencies,
    crossScalaVersions := Seq(scalaVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)

lazy val sparkDigest = (project in file("spark-digest")).
  enablePlugins(AutomateHeaderPlugin).
  settings(commonSettings: _*).
  settings(
    name := "spark-digest",
    libraryDependencies ++= sparkDependencies,
    libraryDependencies += "org.apache.kafka" %% "kafka" % "0.8.2.2",
    scalaVersion := "2.10.5",
    crossScalaVersions := Seq("2.10.5"),
    mainClass in (run) := Some("de.nierbeck.floating.data.stream.spark.KafkaToCassandraSparkAppgit "),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)

lazy val akkaServer = (project in file("akka-server")).
  enablePlugins(JavaAppPackaging).
  enablePlugins(AutomateHeaderPlugin).
  settings(
    name := "akka-server",
    scalaVersion := scalaVer,
    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= akkaHttpDependencies,
    libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVer % "test",
    libraryDependencies += "com.lambdaworks" %% "jacks" % "2.5.2",
    crossScalaVersions := Seq(scalaVer),
    headers := Map(
      "scala" -> Apache2_0("2016", "Achim Nierbeck"),
      "conf" -> Apache2_0("2016", "Achim Nierbeck", "#")
    )
  ).dependsOn(commons)

addCommandAlias("runIngest", "so ingest/run")
addCommandAlias("runSpark", "so sparkDigest/run")
addCommandAlias("runServer", "so akkaServer/run")
