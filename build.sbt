import AssemblyKeys._ // put this at the top of the file
assemblySettings

name := "proxy-scala"

scalaVersion := "2.11.0"

organization := "com.dikei"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.2",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "org.apache.httpcomponents" % "httpcore" % "4.3.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime"
)

fork in run := true

mainClass in (Compile, run) := Some("com.dikei.Main")