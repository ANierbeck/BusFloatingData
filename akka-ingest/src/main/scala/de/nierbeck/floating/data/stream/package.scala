package de.nierbeck.floating.data.stream

package object stream {

  import scala.language.{ implicitConversions, postfixOps }

  type Closeable = { def close(): Unit }

  def withResource[A <: Closeable, B](a: A)(f: A => B): B =
    try f(a) finally a.close()

}
