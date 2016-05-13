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

lazy val compileOptions = Seq(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-Xcheckinit"
)

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

lazy val commonDependencies = Seq(
  "com.typesafe.akka"        %% "akka-actor"                 % akkaVer,
  "com.typesafe.akka"        %% "akka-slf4j"                 % akkaVer,
  "ch.qos.logback"           %  "logback-classic"            % logbackVer,
  "org.scala-lang.modules"   %% "scala-parser-combinators"   % scalaParsersVer,
  "com.typesafe.akka"        %% "akka-testkit"               % akkaVer            % "test",
  "org.scalatest"            %% "scalatest"                  % scalaTestVer       % "test",
  
  // these are to avoid sbt warnings about transitive dependency conflicts
  "org.scala-lang"           %  "scala-reflect"              % scalaVer,
  "org.scala-lang.modules"   %% "scala-xml"                  % "1.0.4",
  "com.typesafe.akka"        %  "akka-http-experimental_2.11" % "2.0.1",
  "de.heikoseeberger"        %% "akka-http-json4s"           % "1.4.1",
  "org.json4s"               %% "json4s-jackson"             % "3.2.11",
  "com.typesafe.akka"        %% "akka-stream-kafka"          % "0.11-M2",
  "com.twitter"              %% "chill-akka"                 % "0.8.0",
  "org.apache.kafka"         %% "kafka"                      % "0.9.0.1",
  "joda-time"                %  "joda-time"                  % "2.9.3"
)

//not so common dependencies :)
val dataStaxConnector = "com.datastax.spark" %% "spark-cassandra-connector" % dataStaxVer
val cassandraDriver = "com.datastax.cassandra" % "cassandra-driver-core" % cassandraVer

lazy val commonSettings = Seq(
  organization := "de.nierbeck.floating.data",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := scalaVer,
  scalacOptions ++= compileOptions,
  unmanagedSourceDirectories in Compile := List((scalaSource in Compile).value),
  unmanagedSourceDirectories in Test := List((scalaSource in Test).value),
  parallelExecution in Test := true, 
  logBuffered in Test := false,
  libraryDependencies ++= commonDependencies
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "BusFloatingData"
  ).
  aggregate(common, ingest, akkaDigest)

lazy val common = (project in file("commons")).
  settings(commonSettings: _*).
  settings(
    name := "commons"
  )

lazy val ingest = (project in file("akka-ingest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-ingest",
    libraryDependencies += cassandraDriver
).dependsOn(common)

lazy val akkaDigest = (project in file("akka-digest")).
  settings(commonSettings: _*).
  settings(
    name := "akka-digest",
    libraryDependencies += cassandraDriver
  ).dependsOn(common)

lazy val sparkDigest = (project in file("spark-digest")).
  settings(commonSettings: _*).
  settings(
    name := "spark-digest"
  ).dependsOn(common)
