name := "scala-compress"
version := "1.0.1"
organization := "com.github.gekomad"
scalaVersion := "3.1.1"

crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.9", "2.13.2", "3.1.1")

val options = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:postfixOps",
  "-feature",
  "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code",              // Warn when dead code is identified.
  "-Ywarn-extra-implicit",         // Warn when more than one implicit parameter section is defined.
  "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",       // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",        // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-explaintypes",                 // Explain type errors in more detail.
  "-Xfatal-warnings",
  "-Ywarn-unused"
)

scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 13)) => options
  case _             => Seq()
})

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.21"
libraryDependencies += "org.lz4"            % "lz4-java"         % "1.8.0"
libraryDependencies += "org.tukaani"        % "xz"               % "1.9"
libraryDependencies += "com.github.luben"   % "zstd-jni"         % "1.5.2-1"

//test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.11" % Test

// sonatype
publishTo := sonatypePublishTo.value

//// microsite
//enablePlugins(GhpagesPlugin)
//enablePlugins(MicrositesPlugin)
//micrositeBaseUrl := "/scala-compress"
//git.remoteRepo := "https://github.com/gekomad/scala-compress.git"
//micrositeGithubOwner := "gekomad"
//micrositeGithubRepo := "scala-compress.git"
//micrositeGitterChannel := false
//micrositeShareOnSocial := false
//micrositeGithubLinks := false
//micrositeTheme := "pattern"
//
//import microsites.CdnDirectives
//
//micrositeCDNDirectives := CdnDirectives(
//  jsList = List(
//    "https://cdnjs.cloudflare.com/ajax/libs/ag-grid/7.0.2/ag-grid.min.js",
//    "https://cdnjs.cloudflare.com/ajax/libs/ajaxify/6.6.0/ajaxify.min.js"
//  ),
//  cssList = List(
//    "https://cdnjs.cloudflare.com/ajax/libs/animate.css/3.5.2/animate.css",
//    "https://cdnjs.cloudflare.com/ajax/libs/cssgram/0.1.12/1977.min.css",
//    "https://cdnjs.cloudflare.com/ajax/libs/cssgram/0.1.12/brooklyn.css"
//  )
//)
