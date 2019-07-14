# scala-statecharts
[ ![Download](https://api.bintray.com/packages/reug/maven/scala-statecharts/images/download.svg) ](https://bintray.com/reug/maven/scala-statecharts/_latestVersion)
[![Build Status](https://travis-ci.org/reugn/scala-statecharts.svg?branch=master)](https://travis-ci.org/reugn/scala-statecharts)

Scala statecharts collection

## Implemented
* **FSM** - Finite-state machine
    * One state is defined as the initial state. When a machine starts to execute, it automatically enters this state.
    * Each state can define actions that occur when a machine enters or exits that state. Actions will typically have side effects.
    * Each state can define events that trigger a transition.
    * A transition defines how a machine would react to the event, by exiting one state and entering another state.
    * A transition can define actions that occur when the transition happens. Actions will typically have side effects.

* **UML** - Executable UML statechart
    * Asynchronous statechart processing
    * Recovery capability (start from last successful transition)
    * No need to translate diagrams into code
    * No bugs introduced by hand translation of diagrams

## Getting started
Add bintray resolver:
```scala
resolvers += Resolver.bintrayRepo("reug", "maven")
```
Add scala-statecharts library as a dependency in your project:
```scala
libraryDependencies += "com.github.reugn" %% "scala-statecharts" % "<version>"
```

## Examples
* [FSM](./src/test/scala/com/github/reugn/statecharts/FSMTest.scala)
* [UML](./src/test/scala/com/github/reugn/statecharts/UMLTest.scala)

## License
Licensed under the [Apache 2.0 License](./LICENSE).
