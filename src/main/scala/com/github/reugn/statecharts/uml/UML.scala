package com.github.reugn.statecharts.uml

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.control.NonFatal

case class UMLException(msg: String) extends Exception(msg)

case class UMLResult[T](content: Option[T], lastStep: Int, error: Option[Throwable] = None)

/**
  * UML statechart class
  * Models data flow as an UML Diagram
  *
  * @param i initial state index
  * @tparam T Block type
  */
class UML[T](private var i: Int = 0) extends Iterator[Block[T]] {
    self =>

    protected val listBuffer: ListBuffer[Block[T]] = new ListBuffer[Block[T]]()
    protected var list: List[Block[T]] = _

    protected var input: T = _

    /**
      * Append UML building block
      *
      * @param next block to add
      * @return self
      */
    def ~>(next: Block[T]): UML[T] = {
        listBuffer += next
        self
    }

    /**
      * Build UML instance
      *
      * @return self
      */
    def ~>| : UML[T] = {
        list = listBuffer.toList
        self
    }

    /** Tests whether this iterator can provide another element.
      *
      * @return `true` if a subsequent call to `next` will yield an element,
      *         `false` otherwise.
      * @note Reuse: $preservesIterator
      */
    override def hasNext: Boolean = {
        require(list != null)
        i < list.size
    }

    /** Produces the next element of this iterator.
      *
      * @return the next element of this iterator, if `hasNext` is `true`,
      *         undefined behavior otherwise.
      * @note Reuse: $preservesIterator
      */
    override def next(): Block[T] = {
        require(list != null)
        val n = list(i)
        i += 1
        n
    }

    type Id[IN] = IN

    trait PF[F[_]] {
        def apply[PT](t: PT): F[PT]
    }

    object identity extends PF[Id] {
        def apply[PT](t: PT): PT = t
    }

    /**
      * Set diagram head index
      *
      * @param ind index
      * @return self
      */
    def from(ind: Int): UML[T] = {
        i = ind
        self
    }

    def getIndex: Int = i - 1

    /**
      * Iterate over the UML diagram
      *
      * @param initial diagram input
      * @param >>      result transformer; defaults to UMLResult identity
      * @param ec      implicit execution context
      * @tparam R higher kind return value
      * @return Future of flow result
      */
    def iterate[R[_]](initial: T, >> : UMLResult[T] => R[T] = identity.apply[UMLResult[T]] _)
                     (implicit ec: ExecutionContext): Future[R[T]] = {
        if (list == null) ~>|
        require(list.nonEmpty)

        def recursiveIter(block: Block[T], in: T): Future[R[T]] = {
            input = in
            block.apply(Future.successful(in)) flatMap {
                res =>
                    if (hasNext) {
                        recursiveIter(next(), res)
                    } else {
                        Future.successful(>>(UMLResult[T](Some(res), getIndex)))
                    }
            }
        }

        recursiveIter(next(), initial) recover {
            case e@UMLException(_) =>
                >>(UMLResult[T](Some(input), getIndex, Some(e)))
            case NonFatal(e) =>
                >>(UMLResult[T](Some(input), getIndex, Some(e)))
        }
    }
}
