package io.github.reugn.statecharts

import io.github.reugn.statecharts.uml.{ActionBlock, ConditionBlock, UML}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future
import scala.language.postfixOps

class UMLTest extends AsyncFlatSpec with Matchers {

    behavior of "UML"

    it should "process UML flow properly" in {
        val block1 = ActionBlock[String] {
            _.map {
                in =>
                    "block1" + in
            }

        }
        val block2 = ActionBlock[String] {
            _.map {
                in =>
                    in.toUpperCase
            }

        }
        val conditional = ConditionBlock[String](
            (f: Future[String]) => f.map(e => e.length % 2 == 0),
            ActionBlock[String] {
                _.map {
                    in =>
                        in + "[even]"
                }
            },
            ActionBlock[String] {
                _.map {
                    in =>
                        in + "[odd]"
                }
            }
        )

        val initialIndex = 0 // can be started from any state in the diagram
        val initialInput = "Foo" // the input that can be saved for further processing

        val uml = new UML[String] ~> block1 ~> block2 ~> conditional
        val res = uml.from(initialIndex).iterate(initialInput)

        res map { r => r.content.get shouldBe "BLOCK1FOO[odd]" }
    }
}
