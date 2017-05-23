organization := "com.workday"

name := "prometheus-akka"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.12.2")

val akkaVersion = "2.4.18"
val aspectjweaverVersion = "1.8.10"
val prometheusVersion = "0.0.22"

checksums in update := Nil

resolvers += Resolver.bintrayRepo("kamon-io", "releases")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.22",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "io.kamon" %% "kamon-core" % "0.6.7",
  "io.prometheus" % "simpleclient" % prometheusVersion,
  "io.prometheus" % "simpleclient_common" % prometheusVersion,
  "com.typesafe" % "config" % "1.3.1",
  "org.aspectj" % "aspectjweaver" % aspectjweaverVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
)

enablePlugins(JavaAgent)
javaAgents += "org.aspectj" % "aspectjweaver" % aspectjweaverVersion % "test"

testOptions in Test += Tests.Argument("-oD")

parallelExecution in Test := false
logBuffered := false

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

parallelExecution in Test := false

homepage := Some(url("https://github.com/Workday/prometheus-akka"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

pomExtra := (
  <scm>
    <url>git@github.com:Workday/prometheus-akka.git</url>
    <connection>scm:git:git@github.com:Workday/prometheus-akka.git</connection>
  </scm>
)
