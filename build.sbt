import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys._
import de.heikoseeberger.sbtheader.{AutomateHeaderPlugin, HeaderPlugin}
import sbt.Keys._

AutomateHeaderPlugin.automateFor(Compile, Test)
HeaderPlugin.settingsFor(Compile, Test)

/**
 * root build.sbt
 */

val akkaVer        = "2.4.3"
val logbackVer     = "1.1.3"
val scalaVer       = "2.11.8"
val scalaParsersVer= "1.0.4"
val scalaTestVer   = "2.2.4"
val cassandraVer   = "3.0.1"
val Slf4j          = "1.7.18"
val spark          = "1.6.0"
val sparkConnector = "1.5.0"

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

lazy val commonDependencies = Seq(
  "ch.qos.logback"           %  "logback-classic"            % logbackVer,
  "org.scalatest"            %% "scalatest"                  % scalaTestVer       % "test",
  "joda-time"                %  "joda-time"                  % "2.9.3",
  "com.twitter"              %% "chill-akka"                 % "0.8.0",

  // datastax cassandra driver
  "com.datastax.cassandra"   % "cassandra-driver-core"       % cassandraVer
)

lazy val akkaDependencies = Seq(
  "org.scala-lang.modules"   %% "scala-parser-combinators"   % scalaParsersVer,
  "com.typesafe.akka"        %% "akka-actor"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-slf4j"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-testkit"               % akkaVer            % "test",

  // these are to avoid sbt warnings about transitive dependency conflicts
  "com.typesafe.akka"        %  "akka-http-experimental_2.11" % "2.0.1",
  "de.heikoseeberger"        %% "akka-http-json4s"           % "1.4.1",
  "org.json4s"               %% "json4s-jackson"             % "3.2.11",
  "com.typesafe.akka"        %% "akka-stream-kafka"          % "0.11-M2",
  "org.apache.kafka"         %% "kafka"                      % "0.9.0.1",
  "joda-time"                %  "joda-time"                  % "2.9.3"
)

lazy val sparkDependencies = Seq(
  "com.datastax.spark"              %% "spark-cassandra-connector"  % sparkConnector,
  "org.apache.spark"                %% "spark-streaming-kafka"      % spark,
  "org.apache.spark"                %% "spark-core"                 % spark,
  "org.apache.spark"                %% "spark-streaming"            % spark,
  "org.apache.spark"                %% "spark-streaming-kafka"      % spark,
  "org.apache.spark"                %% "spark-catalyst"             % spark,
  "org.apache.spark"                %% "spark-sql"                  % spark
)

lazy val logDependencies = Seq(
  "org.slf4j"                       % "slf4j-api"                   % Slf4j,
  "org.slf4j"                       % "jcl-over-slf4j"              % Slf4j,
  "org.slf4j"                       % "jul-to-slf4j"                % Slf4j
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
  settings(commonSettings: _*).
  settings(
    name := "BusFloatingData",
    scalaVersion := scalaVer
  ).
  aggregate(commons, ingest, akkaDigest, sparkDigest)

lazy val commons = (project in file("commons")).
  settings(commonSettings: _*).
  settings(
    name := "commons",
    scalaVersion := scalaVer,
    libraryDependencies += "org.apache.kafka" %% "kafka" % "0.9.0.1" % "provided",
    crossScalaVersions := Seq("2.10.5", scalaVer)
  )

lazy val ingest = (project in file("akka-ingest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-ingest",
    scalaVersion := scalaVer,
    libraryDependencies ++= akkaDependencies,
    crossScalaVersions := Seq("2.11.8"),
    mainClass in (Compile,run) := Some("de.nierbeck.floating.data.stream.StreamToKafkaApp")
  ).dependsOn(commons)

lazy val akkaDigest = (project in file("akka-digest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-digest",
    scalaVersion := scalaVer,
    libraryDependencies ++= akkaDependencies,
    crossScalaVersions := Seq("2.11.8")
  ).dependsOn(commons)

lazy val sparkDigest = (project in file("spark-digest")).
  settings(commonSettings: _*).
  settings(
    name := "spark-digest",
    libraryDependencies ++= sparkDependencies,
    libraryDependencies += "org.apache.kafka" %% "kafka" % "0.8.2.2",
    scalaVersion := "2.10.5",
    crossScalaVersions := Seq("2.10.5")
  ).dependsOn(commons)
