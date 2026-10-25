package com.atacamasoft.rsp.backoffice.server

import com.atacamasoft.rsp.backoffice.server.infrastructure.Application
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence.Database
import com.atacamasoft.rsp.backoffice.server.infrastructure.config.AppConfig
import zio.*
import zio.http.Server
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault:

  // ZIO logs go through SLF4J -> logback-classic
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val program: Task[Nothing] =
    ZIO.scoped {
      for
        cfg    <- AppConfig.load
        db     <- Database.make(cfg.db)
        routes  = Application.routes(cfg, db)
        _      <- ZIO.logInfo(s"Real Estate Portal API listening on ${cfg.http.host}:${cfg.http.port} (docs: /docs, liveness: /health, readiness: /health/ready)")
        result <- Server.serve(routes).provide(Server.defaultWith(_.binding(cfg.http.host, cfg.http.port)))
      yield result
    }

  override def run: ZIO[Any, Any, Any] = program
