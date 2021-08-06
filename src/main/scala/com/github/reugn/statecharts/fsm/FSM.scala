package com.github.reugn.statecharts.fsm

import java.util.concurrent.{Callable, Executors, TimeUnit}
import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal

case class InvalidFSMTransition(msg: String, e: Throwable) extends Exception(msg, e)

/**
  * An object that represents a Finite-state machine.
  *
  * @param currentState the initial state of the FSM.
  * @param graph        the state diagram [[Map]] of the FSM.
  * @tparam S the type of the FSM state.
  * @tparam E the type of the FSM event.
  */
class FSM[S <: SideEffect, E] private[fsm](private var currentState: S,
                                           private val graph: Map[S, Map[E, S]]) {

    /**
      * The delayed transitions scheduler.
      */
    private lazy val scheduler = Executors.newScheduledThreadPool(1)

    /**
      * Performs a transition for the given event.
      *
      * @param event the actual transition event.
      * @return the FSM state after the transition.
      */
    @throws[InvalidFSMTransition]("on faulty transition.")
    def transition(event: E): S = this.synchronized {
        currentState.onExit()
        try {
            currentState = graph(currentState)(event)
        } catch {
            case NonFatal(e) =>
                throw InvalidFSMTransition(s"Failed to transition from $currentState on $event", e)
        }
        currentState.onEnter()
        currentState
    }

    /**
      * Performs a delayed transition for the given event.
      *
      * @param event the actual transition event.
      * @param delay the delay [[Duration]].
      * @param ec    the implicit execution context.
      * @return the state after the transition.
      */
    def delayedTransition(event: E, delay: Duration)(implicit ec: ExecutionContext): Future[S] = {
        val (t: Long, tu: TimeUnit) = durationToPair(delay)
        Future {
            scheduler.schedule((() => transition(event)): Callable[S], t, tu).get
        }
    }

    /**
      * Returns the current state.
      *
      * @return the current FSM state.
      */
    def state(): S = this.synchronized {
        currentState
    }
}

object FSM {

    /**
      * Creates an [[FSM]] given the initial state and the list of the [[State]] objects.
      *
      * @param initial the initial state object of the FSM.
      * @param states  the list of the [[State]] objects with transition rules.
      * @tparam S the type of the FSM state.
      * @tparam E the type of the FSM event.
      * @return a new FSM instance.
      */
    def apply[S <: SideEffect, E](initial: S, states: State[S, E]*): FSM[S, E] = {
        val builder = new GraphBuilder[S, E].initialState(initial)
        states foreach builder.setState
        builder.build()
    }

    /**
      * A mutable builder for an [[FSM]].
      *
      * @tparam S the type of the FSM state.
      * @tparam E the type of the FSM event.
      */
    private[fsm] class GraphBuilder[S <: SideEffect, E] {
        self =>

        private var initial: S = _
        private val graph: mutable.Map[S, Map[E, S]] = mutable.Map()

        def initialState(state: S): GraphBuilder[S, E] = {
            initial = state
            self
        }

        def setState(state: State[S, E]): GraphBuilder[S, E] = {
            graph.put(state.state, state.trans.toMap)
            self
        }

        def build(): FSM[S, E] = {
            new FSM(initial, graph.toMap)
        }
    }
}

/**
  * The [[FSM]] state model. Contains a state object and a list
  * of available transitions.
  *
  * @param state       the actual FSM state.
  * @param transitions the list of available transitions.
  * @tparam S the type of the FSM state.
  * @tparam E the type of the FSM event.
  */
case class State[S <: SideEffect, E](state: S, transitions: On[S, E]*) {
    val trans: mutable.Map[E, S] = mutable.Map()
    transitions.foreach(on => trans.put(on.event, on.dest))
}

/**
  * The [[FSM]] transition rule model. Contains an event and a resulting
  * destination state object.
  *
  * @param event the actual event that has been received.
  * @param dest  the destination state for the given even.
  * @tparam S the type of the FSM state.
  * @tparam E the type of the FSM event.
  */
case class On[+S <: SideEffect, +E](event: E, dest: S)

/**
  * The supertype for the [[FSM]] state to handle transition side effects.
  */
trait SideEffect {
    /**
      * A method that is called in [[FSM.transition]] before returning
      * the state.
      * Defaults to no-action.
      */
    def onEnter(): Unit = {}

    /**
      * A method that is called in [[FSM.transition]] before transitioning
      * to a new state.
      * Defaults to no-action.
      */
    def onExit(): Unit = {}
}
