name := """synergy-base"""

version := "1.0"

scalaVersion := "2.11.7"

lazy val akkaVersion = "2.4.0"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaStreamV = "2.0-M2"
  Seq(
    "com.typesafe" % "config" % "1.3.0",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

    "com.typesafe.play" %% "play-json" % "2.5.0-M1",

    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-M2",
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "1.0-M2",
    "com.typesafe.akka" % "akka-http-testkit-experimental_2.11" % "1.0-M2",

//    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M2",
//    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M2",
//    "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M2",

    /*
    "com.typesafe.akka" %% "akka-stream-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"            % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"       % akkaStreamV,*/
    // "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamV,
    // "com.typesafe.akka" %% "akka-http-testkit-experimental"    % akkaStreamV,

    "org.scalatest" %% "scalatest" % "2.2.5" % Test,
    "junit" % "junit" % "4.12" % Test,
    "com.novocode" % "junit-interface" % "0.11" % Test,
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion % Test


    // Or add: and spacebase jars (manually) and the following:
    // Run your tests!
    //"org.springframework" % "spring-aop" % "3.2.15.RELEASE",
    //"org.springframework" % "spring-expression" % "3.2.15.RELEASE",
    //"co.paralleluniverse" % "galaxy" % "1.3",
    //"co.paralleluniverse" % "quasar-core" % "0.5.0"
    //"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.4.1"
  )
}

enablePlugins(JavaServerAppPackaging)
mainClass in Compile := Some("com.coredump.synergybase.SynergyBaseApp")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

fork in run := true
