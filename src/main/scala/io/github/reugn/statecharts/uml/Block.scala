package io.github.reugn.statecharts.uml

import scala.concurrent.{ExecutionContext, Future}

/**
  * The trait that represents an abstract [[UML]] Block.
  *
  * @tparam T the type of the input/output objects.
  */
trait Block[T] extends (Future[T] => Future[T])

/**
  * A [[Block]] that represents a simple action [[UML]] unit that transforms
  * an input object using the transition function and returns the result.
  *
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
  * @param action the block's transition function.
  * @tparam T the type of the input/output objects.
  */
case class ActionBlock[T](action: Future[T] => Future[T]) extends Block[T] {
    @throws(classOf[UMLException])
    override def apply(data: Future[T]): Future[T] = action(data)
}

/**
  * A [[Block]] that represents a conditional [[UML]] unit which runs the condition
  * function first. And then, having the result, one of the specified Blocks
  * accordingly.
  *
  *             |
  *        true | false
  *      -------<>-------
  *      |              |
  *  ---------      ---------
  *  |t_block|      |f_block|
  *  ---------      ---------
  *
  * @param cond     the boolean condition function.
  * @param on_true  the Block to execute on a positive condition.
  * @param on_false the Block to execute on a negative condition.
  * @param ec       the implicit execution context.
  * @tparam T the type of the input/output objects.
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
