package de.nierbeck.test.akka

package object stream {

  type Closeable = { def close(): Unit }

  def withResource[A <: Closeable, B](a: A)(f: A => B): B =
    try f(a) finally a.close()
}
