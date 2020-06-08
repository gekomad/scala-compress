name := "scala-compress"
version := "0.0.1"
organization := "com.github.gekomad"
scalaVersion := "2.13.2"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.9", "2.13.2")

val options = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:postfixOps",
  "-feature",
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-explaintypes", // Explain type errors in more detail.
  //"-Xfatal-warnings",
  "-Ywarn-unused"
)

scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 13)) => options
  case _             => Seq()
})

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.20"
libraryDependencies += "org.lz4"            % "lz4-java"         % "1.7.1"
libraryDependencies += "org.tukaani"        % "xz"               % "1.8"
libraryDependencies += "com.github.luben"   % "zstd-jni"         % "1.4.5-4"

//test
libraryDependencies += "org.scalatest"      %% "scalatest"       % "3.3.0-SNAP2" % Test

//sonatype
publishTo := sonatypePublishTo.value
