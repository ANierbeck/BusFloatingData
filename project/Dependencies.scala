import sbt._

object Version {
  final val akkaVer        = "2.4.10"
  final val scalaVer       = "2.11.8"
  final val scalaVertxVer  = "2.12.2"
  final val scalaParsersVer= "1.0.4"
  final val scalaTestVer   = "3.0.1"
  final val cassandraVer   = "3.1.2"
  final val Log4j2         = "2.8.2"
  final val Slf4j          = "1.7.18"
  final val spark          = "2.1.1"
  final val sparkConnector = "2.0.1"
  final val circeVersion   = "0.4.1"
  final val kafkaVersion   = "0.10.1.1"
  final val flinkVersion   = "1.3.0"
  final val Vertx          = "3.4.2"
}

object CommonDependencies {
  val scala_test      = "org.scalatest"            %% "scalatest"                  % Version.scalaTestVer       % "test"
  val scala_xml       = "org.scala-lang.modules"   %% "scala-xml"                  % "1.0.6"
  val joda_time       = "joda-time"                %  "joda-time"                  % "2.9.3"
  val akka_chill      = "com.twitter"              %% "chill-akka"                 % "0.9.2"

  // Fast Java Serializer
  val fast_serializer = "de.ruedigermoeller"       %  "fst"                        % "2.45"

  val scalatest_embedded_cassandra = "io.tmio"     % "scalatest-embedded-cassandra" % "1.0.0"

}

object KafkaDependencies {
  val kafka           = "org.apache.kafka"                %% "kafka"                      % Version.kafkaVersion
  val kafka_clients   = "org.apache.kafka"                % "kafka-clients"               % Version.kafkaVersion
}

object AkkaDependencies {
  val scala_lang_modules = "org.scala-lang.modules"   %% "scala-parser-combinators"   % Version.scalaParsersVer
  val akka_actor         = "com.typesafe.akka"        %% "akka-actor"                 % Version.akkaVer
  val akka_slf4j         = "com.typesafe.akka"        %% "akka-slf4j"                 % Version.akkaVer
  val akka_testkit       = "com.typesafe.akka"        %% "akka-testkit"               % Version.akkaVer            % "test"

  // these are to avoid sbt warnings about transitive dependency conflicts
  val akka_http             = "com.typesafe.akka"               %  "akka-http-experimental_2.11"  % "2.0.1"
  val akka_stream_kafka     = "com.typesafe.akka"               %% "akka-stream-kafka"            % "0.12"
  val akka_http_json4s      = "de.heikoseeberger"               %% "akka-http-json4s"             % "1.6.0"
  val json4s_jackson        = "org.json4s"                      %% "json4s-jackson"               % "3.2.11"
  val cassandra_driver_core = "com.datastax.cassandra"          % "cassandra-driver-core"         % Version.cassandraVer
}

object SparkDependencies {
  val spark_cassandra_connector = "com.datastax.spark"              %% "spark-cassandra-connector"  % Version.sparkConnector
  val spark_streaming_kafka     = "org.apache.spark"                %% "spark-streaming-kafka-0-10" % Version.spark
  val spark_core                = "org.apache.spark"                %% "spark-core"                 % Version.spark           % "provided"
  val spark_streaming           = "org.apache.spark"                %% "spark-streaming"            % Version.spark           % "provided"
  val spark_catalyst            = "org.apache.spark"                %% "spark-catalyst"             % Version.spark           % "provided"
  val spark_sql                 = "org.apache.spark"                %% "spark-sql"                  % Version.spark           % "provided"
  val spark_mllib               = "org.apache.spark"                %% "spark-mllib"                % Version.spark           % "provided"
  val scalanlp                  = "org.scalanlp"                    %% "nak"                        % "1.3"
}

object FlinkDependencies {
  val flink_core                = "org.apache.flink"     %  "flink-core"                  % Version.flinkVersion % "provided"
  val flink_scala               = "org.apache.flink"     %% "flink-scala"                 % Version.flinkVersion % "provided"
  val flink_streaming_java      = "org.apache.flink"     %% "flink-streaming-java"        % Version.flinkVersion % "provided"
  val flink_streaming_scala     = "org.apache.flink"     %% "flink-streaming-scala"       % Version.flinkVersion % "provided"
  val flink_connector_kafka     = "org.apache.flink"     %% "flink-connector-kafka-0.10"  % Version.flinkVersion
  val flink_connector_cassandra = "org.apache.flink"     %% "flink-connector-cassandra"   % Version.flinkVersion
  val flink_client              = "org.apache.flink"     %% "flink-clients"               % Version.flinkVersion
  val metrics_core              = "com.codahale.metrics" %  "metrics-core"                % "3.0.2"
}

object LogDependencies {
  val slf4j       = "org.slf4j"                       % "slf4j-api"                           % Version.Slf4j
  val log4j12_api = "org.apache.logging.log4j"        % "log4j-1.2-api"                       % Version.Log4j2
  val log4j_slf4j = "org.apache.logging.log4j"        % "log4j-slf4j-impl"                    % Version.Log4j2
  val log4j_api   = "org.apache.logging.log4j"        % "log4j-api"                           % Version.Log4j2
  val log4j_core  = "org.apache.logging.log4j"        % "log4j-core"                          % Version.Log4j2
  val jcl         = "org.slf4j"                       % "jcl-over-slf4j"                      % Version.Slf4j
  val jul         = "org.slf4j"                       % "jul-to-slf4j"                        % Version.Slf4j
}

object VertxDependencies {
  val vertx_codegen                     = "io.vertx" %  "vertx-codegen"                           % Version.Vertx % "provided"
  val vertx_lang_scala                  = "io.vertx" %% "vertx-lang-scala"                        % Version.Vertx
  val vertx_hazelcast                   = "io.vertx" %  "vertx-hazelcast"                         % Version.Vertx
  val vertx_web                         = "io.vertx" %% "vertx-web-scala"                         % Version.Vertx

  val vertx_mqtt_server                 = "io.vertx" %% "vertx-mqtt-server-scala"                 % Version.Vertx
  val vertx_sql_common                  = "io.vertx" %% "vertx-sql-common-scala"                  % Version.Vertx
  val vertx_bridge_common               = "io.vertx" %% "vertx-bridge-common-scala"               % Version.Vertx
  val vertx_jdbc_client                 = "io.vertx" %% "vertx-jdbc-client-scala"                 % Version.Vertx
  val vertx_mongo_client                = "io.vertx" %% "vertx-mongo-client-scala"                % Version.Vertx
  val vertx_mongo_service               = "io.vertx" %% "vertx-mongo-service-scala"               % Version.Vertx
  val vertx_auth_common                 = "io.vertx" %% "vertx-auth-common-scala"                 % Version.Vertx
  val vertx_auth_shiro                  = "io.vertx" %% "vertx-auth-shiro-scala"                  % Version.Vertx
  val vertx_auth_htdigest               = "io.vertx" %% "vertx-auth-htdigest-scala"               % Version.Vertx
  val vertx_auth_oauth2                 = "io.vertx" %% "vertx-auth-oauth2-scala"                 % Version.Vertx
  val vertx_auth_mongo                  = "io.vertx" %% "vertx-auth-mongo-scala"                  % Version.Vertx
  val vertx_auth_jwt                    = "io.vertx" %% "vertx-auth-jwt-scala"                    % Version.Vertx
  val vertx_auth_jdbc                   = "io.vertx" %% "vertx-auth-jdbc-scala"                   % Version.Vertx
  val vertx_web_common                  = "io.vertx" %% "vertx-web-common-scala"                  % Version.Vertx
  val vertx_web_client                  = "io.vertx" %% "vertx-web-client-scala"                  % Version.Vertx
  val vertx_sockjs_service_proxy        = "io.vertx" %% "vertx-sockjs-service-proxy-scala"        % Version.Vertx
  val vertx_web_templ_freemarker        = "io.vertx" %% "vertx-web-templ-freemarker-scala"        % Version.Vertx
  val vertx_web_templ_handlebars        = "io.vertx" %% "vertx-web-templ-handlebars-scala"        % Version.Vertx
  val vertx_web_templ_jade              = "io.vertx" %% "vertx-web-templ-jade-scala"              % Version.Vertx
  val vertx_web_templ_mvel              = "io.vertx" %% "vertx-web-templ-mvel-scala"              % Version.Vertx
  val vertx_web_templ_pebble            = "io.vertx" %% "vertx-web-templ-pebble-scala"            % Version.Vertx
  val vertx_web_templ_thymeleaf         = "io.vertx" %% "vertx-web-templ-thymeleaf-scala"         % Version.Vertx
  val vertx_mysql_postgresql_client     = "io.vertx" %% "vertx-mysql-postgresql-client-scala"     % Version.Vertx
  val vertx_mail_client                 = "io.vertx" %% "vertx-mail-client-scala"                 % Version.Vertx
  val vertx_rabbitmq_client             = "io.vertx" %% "vertx-rabbitmq-client-scala"             % Version.Vertx
  val vertx_redis_client                = "io.vertx" %% "vertx-redis-client-scala"                % Version.Vertx
  val vertx_stomp                       = "io.vertx" %% "vertx-stomp-scala"                       % Version.Vertx
  val vertx_tcp_eventbus_bridge         = "io.vertx" %% "vertx-tcp-eventbus-bridge-scala"         % Version.Vertx
  val vertx_amqp_bridge                 = "io.vertx" %% "vertx-amqp-bridge-scala"                 % Version.Vertx
  val vertx_dropwizard_metrics          = "io.vertx" %% "vertx-dropwizard-metrics-scala"          % Version.Vertx
  val vertx_hawkular_metrics            = "io.vertx" %% "vertx-hawkular-metrics-scala"            % Version.Vertx
  val vertx_shell                       = "io.vertx" %% "vertx-shell-scala"                       % Version.Vertx
  val vertx_kafka_client                = "io.vertx" %% "vertx-kafka-client-scala"                % Version.Vertx
  val vertx_circuit_breaker             = "io.vertx" %% "vertx-circuit-breaker-scala"             % Version.Vertx
  val vertx_config                      = "io.vertx" %% "vertx-config-scala"                      % Version.Vertx
  val vertx_service_discovery           = "io.vertx" %% "vertx-service-discovery-scala"           % Version.Vertx
  val vertx_config_git                  = "io.vertx" %% "vertx-config-git-scala"                  % Version.Vertx
  val vertx_config_hocon                = "io.vertx" %% "vertx-config-hocon-scala"                % Version.Vertx
  val vertx_config_kubernetes_configmap = "io.vertx" %% "vertx-config-kubernetes-configmap-scala" % Version.Vertx
  val vertx_config_redis                = "io.vertx" %% "vertx-config-redis-scala"                % Version.Vertx
  val vertx_config_spring_config_server = "io.vertx" %% "vertx-config-spring-config-server-scala" % Version.Vertx
  val vertx_config_yaml                 = "io.vertx" %% "vertx-config-yaml-scala"                 % Version.Vertx
  val vertx_config_zookeeper            = "io.vertx" %% "vertx-config-zookeeper-scala"            % Version.Vertx

  val vertx_cassandra                   = "com.englishtown.vertx" % "vertx-cassandra"             % "3.2.0"
}