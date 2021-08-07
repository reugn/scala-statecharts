# scala-statecharts
[![Build](https://github.com/reugn/scala-statecharts/actions/workflows/build.yml/badge.svg)](https://github.com/reugn/scala-statecharts/actions/workflows/build.yml)

A statecharts library written in Scala.

## Implemented
* **FSM** - Finite-state machine.
    * One state is defined as the initial state. When a machine starts to execute, it automatically enters this state
    * Each state can define actions that occur when a machine enters or exits that state. Actions will typically have side effects
    * Each state can define events that trigger a transition
    * A transition defines how a machine would react to the event, by exiting one state and entering another state
    * A transition can define actions that occur when the transition happens. Actions will typically have side effects

* **UML** - Executable UML statechart.
    * Asynchronous statechart processing
    * Recovery capability (start from last successful transition)
    * No need to translate diagrams into code
    * No bugs introduced by hand translation of diagrams

## Getting started
The library is available for the JVM Runtime using Scala 2.12, 2.13.

Build from source:
```sh
sbt clean +package
```

## Examples
* [FSM](./src/test/scala/io/github/reugn/statecharts/FSMTest.scala)
* [UML](./src/test/scala/io/github/reugn/statecharts/UMLTest.scala)

## License
Licensed under the [Apache 2.0 License](./LICENSE).
