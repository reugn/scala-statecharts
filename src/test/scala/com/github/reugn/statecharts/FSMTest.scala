package com.github.reugn.statecharts

import com.github.reugn.statecharts.fsm._
import org.scalatest.{FlatSpec, Matchers}

class FSMTest extends FlatSpec with Matchers {

    behavior of "FSM"

    it should "transition FSM properly" in {
        sealed trait FooState extends SideEffect
        object Foo1 extends FooState
        object Foo2 extends FooState
        object Foo3 extends FooState

        sealed trait BarEvent
        object Bar1 extends BarEvent
        object Bar2 extends BarEvent
        object Bar3 extends BarEvent

        val fsm = FSM(
            Foo1,
            State(
                Foo1,
                On(Bar1, Foo2),
                On(Bar2, Foo3)
            ),
            State(
                Foo2,
                On(Bar1, Foo3),
                On(Bar2, Foo1)
            ),
            State(
                Foo3,
                On(Bar1, Foo1),
                On(Bar2, Foo2)
            )
        )

        fsm.state() shouldBe Foo1
        fsm.transition(Bar1) shouldBe Foo2
        fsm.transition(Bar1) shouldBe Foo3

        intercept[InvalidFSMTransition] {
            fsm.transition(Bar3)
        }
    }
}
