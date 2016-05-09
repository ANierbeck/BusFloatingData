val akkaVer = "2.4.3"
val logbackVer = "1.1.3"
val scalaVer = "2.11.8"
val scalaParsersVer= "1.0.4"
val scalaTestVer = "2.2.4"
val sparkVer = "1.5.0-RC1"

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
  "com.datastax.spark" %% "spark-cassandra-connector" % sparkVer,
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0.1",
  "de.heikoseeberger" %% "akka-http-json4s" % "1.4.1",
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.11-M2",
  "com.twitter"                    %% "chill-akka"          % "0.8.0",
  "org.apache.kafka"               %% "kafka"               % "0.9.0.1"
)

lazy val fttas = project in file(".")

name := "Akka-Stream-Test"

version := "1.0.0"

scalaVersion := scalaVer
scalacOptions ++= compileOptions
unmanagedSourceDirectories in Compile := List((scalaSource in Compile).value)
unmanagedSourceDirectories in Test := List((scalaSource in Test).value)
EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
EclipseKeys.eclipseOutput := Some(".target")
EclipseKeys.withSource := true
parallelExecution in Test := false
logBuffered in Test := false
parallelExecution in ThisBuild := false
libraryDependencies ++= commonDependencies