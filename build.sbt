import sbt.Keys._

/**
 * root build.sbt
 */

val akkaVer = "2.4.3"
val logbackVer = "1.1.3"
val scalaVer = "2.11.8"
val scalaParsersVer= "1.0.4"
val scalaTestVer = "2.2.4"
val dataStaxVer = "1.6.0-M2"
val cassandraVer = "3.0.1"

val spark = "1.6.1"
val sparkConnector = "1.6.0-M2"

autoCompilerPlugins := true

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
  "com.twitter"              %% "chill-akka"                 % "0.8.0"
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
  "com.softwaremill.reactivekafka"  %% "reactive-kafka-core"        % "0.8.2",
  "org.apache.spark"                %% "spark-streaming-kafka"      % spark
)

//not so common dependencies :)
val dataStaxConnector = "com.datastax.spark" %% "spark-cassandra-connector" % dataStaxVer
val cassandraDriver = "com.datastax.cassandra" % "cassandra-driver-core" % cassandraVer

lazy val commonSettings = Seq(
  organization := "de.nierbeck.floating.data",
  version := "0.1.0-SNAPSHOT",
  scalacOptions ++= compileOptions,
  parallelExecution in Test := true, 
  logBuffered in Test := false,
  libraryDependencies ++= commonDependencies
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "BusFloatingData",
    scalaVersion := scalaVer
  ).
  aggregate(common, ingest, akkaDigest, sparkDigest)

lazy val common = (project in file("commons")).
  settings(commonSettings: _*).
  settings(
    name := "commons",
    scalaVersion := scalaVer,
    libraryDependencies += "org.apache.kafka" %% "kafka" % "0.9.0.1",
    crossScalaVersions := Seq("2.10.5", scalaVer)
  )

lazy val ingest = (project in file("akka-ingest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-ingest",
    scalaVersion := scalaVer,
    libraryDependencies += cassandraDriver,
    libraryDependencies ++= akkaDependencies,
    crossScalaVersions := Seq("2.11.8")
).dependsOn(common)

lazy val akkaDigest = (project in file("akka-digest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-digest",
    scalaVersion := scalaVer,
    libraryDependencies += cassandraDriver,
    libraryDependencies ++= akkaDependencies,
    crossScalaVersions := Seq("2.11.8")
  ).dependsOn(common)

lazy val sparkDigest = (project in file("spark-digest")).
  settings(commonSettings: _*).
  settings(
    name := "spark-digest",
    libraryDependencies ++= sparkDependencies,
    scalaVersion := "2.10.5",
    crossScalaVersions := Seq("2.10.5")
  ).dependsOn(common)
