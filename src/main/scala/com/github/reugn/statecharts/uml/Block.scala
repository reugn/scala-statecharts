package com.github.reugn.statecharts.uml

import scala.concurrent.{ExecutionContext, Future}

/**
  * UML Block trait
  *
  * @tparam T input/output type
  */
trait Block[T] extends (Future[T] => Future[T])

/**
  * UML Action Block
  *             |
  *             | Future[T]
  *             |
  *         ----------
  *         | action |
  *         ----------
  *             |
  *             | Future[T]
  *             |
  *
  * @param action transition implementation
  * @tparam T input/output type
  */
case class ActionBlock[T](action: Future[T] => Future[T]) extends Block[T] {
    @throws(classOf[UMLException])
    override def apply(data: Future[T]): Future[T] = action(data)
}

/**
  * UML Condition Block
  *             |
  *        true | false
  *      -------<>-------
  *      |              |
  *  ---------      ---------
  *  |t_block|      |f_block|
  *  ---------      ---------
  *
  * @param cond     boolean condition
  * @param on_true  on true Block
  * @param on_false on false Block
  * @param ec       implicit execution context
  * @tparam T input/output type
  */
case class ConditionBlock[T](cond: Future[T] => Future[Boolean], on_true: Block[T], on_false: Block[T])
                            (implicit ec: ExecutionContext) extends Block[T] {
    @throws(classOf[UMLException])
    override def apply(data: Future[T]): Future[T] = {
        def exec(b: Block[T]): Future[T] = b match {
            case a@ActionBlock(_) =>
                a.apply(data)
            case cb@ConditionBlock(_, _, _) =>
                cb.apply(data)
        }

        cond(data) flatMap {
            case true => exec(on_true)
            case false => exec(on_false)
        }
    }
}
