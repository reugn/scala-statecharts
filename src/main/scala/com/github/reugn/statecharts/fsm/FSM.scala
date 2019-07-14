package com.github.reugn.statecharts.fsm

import java.util.concurrent.{Callable, Executors, TimeUnit}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal

case class InvalidFSMTransition(msg: String, e: Throwable) extends Exception(msg, e)

/**
  * Finite-state machine
  *
  * @param currentState initial state
  * @param graph        state diagram
  * @tparam S State type
  * @tparam E Event type
  */
class FSM[S <: SideEffect, E] private[fsm](private var currentState: S,
                                           private val graph: Map[S, Map[E, S]]) {

    /**
      * Delayed transitions scheduler
      */
    private lazy val scheduler = Executors.newScheduledThreadPool(1)

    /**
      * Perform transition for event
      *
      * @param event actual transition event
      * @throws com.github.reugn.statecharts.fsm.InvalidFSMTransition on faulty transition
      * @return fsm state
      */
    @throws(classOf[InvalidFSMTransition])
    def transition(event: E): S = this.synchronized {
        currentState.onExit()
        try {
            currentState = graph(currentState)(event)
        } catch {
            case NonFatal(e) =>
                throw InvalidFSMTransition(s"Failed to transition from $currentState for $event", e)
        }
        currentState.onEnter()
        currentState
    }

    /**
      * Perform delayed transition for event
      *
      * @param event actual transition event
      * @param d     defer duration
      * @param ec    implicit execution context
      * @return fsm state
      */
    def delayedTransition(event: E, d: Duration)(implicit ec: ExecutionContext): Future[S] = {
        val (t: Long, tu: TimeUnit) = durationToPair(d)
        Future {
            scheduler.schedule((() => transition(event)): Callable[S], t, tu).get
        }
    }

    /**
      * Get current state
      *
      * @return current state
      */
    def state(): S = this.synchronized {
        currentState
    }
}

object FSM {

    /**
      * FSM constructor
      *
      * @param initial initial state
      * @param states  list of state objects with transition rules
      * @tparam S State type
      * @tparam E Event type
      * @return new FSM instance
      */
    def apply[S <: SideEffect, E](initial: S, states: State[S, E]*): FSM[S, E] = {
        val builder = new GraphBuilder[S, E].initialState(initial)
        states foreach builder.setState
        builder.build()
    }
}

/**
  * FSM state diagram builder
  *
  * @tparam S State type
  * @tparam E Event type
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

/**
  * State model
  *
  * @param state       actual state
  * @param transitions list of available transitions
  * @tparam S State type
  * @tparam E Event type
  */
case class State[S <: SideEffect, E](state: S, transitions: On[S, E]*) {
    val trans: mutable.Map[E, S] = mutable.Map()
    transitions.foreach(on => trans.put(on.event, on.dest))
}

/**
  * Transition rule model
  *
  * @param event actual event received
  * @param dest  destination state
  * @tparam S State type
  * @tparam E Event type
  */
case class On[+S <: SideEffect, +E](event: E, dest: S)

/**
  * Transitions side effects trait
  */
trait SideEffect {
    /**
      * On state enter hook
      * default empty
      */
    def onEnter(): Unit = {}

    /**
      * On state exit hook
      * default empty
      */
    def onExit(): Unit = {}
}
