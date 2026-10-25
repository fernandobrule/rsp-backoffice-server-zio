import sbt.*

object Dependencies {

  object V {
    val zioHttp    = "3.11.6"
    val tapir      = "1.13.33"
    val logback    = "1.6.5"
    val zioLogging = "2.5.0"
    val postgresql = "42.7.8"
    val hikari     = "6.3.0"
  }

  private val tapirOrg = "com.softwaremill.sttp.tapir"

  val all: Seq[ModuleID] = Seq(
    // HTTP server
    "dev.zio" %% "zio-http" % V.zioHttp,
    // Tapir (SoftwareMill)
    tapirOrg %% "tapir-zio"              % V.tapir,
    tapirOrg %% "tapir-zio-http-server"  % V.tapir,
    tapirOrg %% "tapir-json-zio"         % V.tapir,
    tapirOrg %% "tapir-swagger-ui-bundle" % V.tapir,
    // Logging
    "ch.qos.logback" % "logback-classic"    % V.logback,
    "dev.zio"       %% "zio-logging-slf4j2" % V.zioLogging,
    // PostgreSQL
    "org.postgresql" % "postgresql" % V.postgresql,
    "com.zaxxer"     % "HikariCP"   % V.hikari
  )
}
