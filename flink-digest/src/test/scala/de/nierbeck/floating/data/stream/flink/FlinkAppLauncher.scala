package de.nierbeck.floating.data.stream.flink

object FlinkAppLauncher {
  def main(args: Array[String]): Unit = {
    KafkaToCassandraFlinkApp.main(args)
  }
}
