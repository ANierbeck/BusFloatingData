/*
 * Copyright 2016 Achim Nierbeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.nierbeck.floating.data

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import de.nierbeck.floating.data.domain.{BoundingBox, LatLon}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

package object server {
  val Traversable = scala.collection.immutable.Traversable
  type Traversable[+A] = scala.collection.immutable.Traversable[A]

  val Iterable = scala.collection.immutable.Iterable
  type Iterable[+A] = scala.collection.immutable.Iterable[A]

  val Seq = scala.collection.immutable.Seq
  type Seq[+A] = scala.collection.immutable.Seq[A]

  val IndexedSeq = scala.collection.immutable.IndexedSeq
  type IndexedSeq[+A] = scala.collection.immutable.IndexedSeq[A]

  def futureToFutureTry[T](f: Future[T])(implicit ec: ExecutionContext): Future[Try[T]] =
    f.map(Success(_)).recover { case exception: Exception => Failure(exception) }

  implicit class RichListenableFuture[T](lf: ListenableFuture[T]) {
    def toFuture: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t

        def onSuccess(result: T): Unit = p success result
      })
      p.future
    }
  }

  def toBoundingBox(bbox: String): BoundingBox = {
    val bboxCoords: Array[String] = bbox.split(",")
    val boundingBox: BoundingBox =
      new BoundingBox(LatLon(bboxCoords(0).toFloat, bboxCoords(1).toFloat), LatLon(bboxCoords(2).toFloat, bboxCoords(3).toFloat))
    boundingBox
  }

}
