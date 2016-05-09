package de.nierbeck.test.akka

import com.datastax.driver.core.{ResultSet, ResultSetFuture}
import com.google.common.util.concurrent.{Futures, FutureCallback}

import scala.concurrent.Future
import scala.concurrent.Promise

package object stream {

  import scala.language.implicitConversions
  import scala.language.postfixOps

  type Closeable = { def close(): Unit }

  def withResource[A <: Closeable, B](a: A)(f: A => B): B =
    try f(a) finally a.close()


  implicit def resultSetFutureToScala(f: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback(f,
      new FutureCallback[ResultSet] {
        def onSuccess(r: ResultSet) = p success r
        def onFailure(t: Throwable) = p failure t
      })
    p.future
  }

}
