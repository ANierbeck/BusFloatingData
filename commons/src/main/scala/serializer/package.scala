/**
 * Created by anierbeck on 13.05.16.
 */
package object serializer {

  import scala.language.{implicitConversions, postfixOps}

  type Closeable = { def close(): Unit }

  def withResource[A <: Closeable, B](a: A)(f: A => B): B =
    try f(a) finally a.close()
}
