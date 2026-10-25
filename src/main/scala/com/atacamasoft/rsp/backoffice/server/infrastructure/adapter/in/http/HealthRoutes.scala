package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http

import com.atacamasoft.rsp.backoffice.server.application.port.out.HealthProbe
import zio.*
import zio.http.*

/**
 *  GET /health        liveness  - "is the process up?"        (never touches PostgreSQL)
 *  GET /health/ready  readiness - "can it reach PostgreSQL?"   (200 UP / 503 DOWN)
 */
object HealthRoutes:

  private def json(status: Status, body: String): Response =
    Response(status = status, headers = Headers(Header.ContentType(MediaType.application.json)), body = Body.fromString(body))

  def routes(probe: HealthProbe): Routes[Any, Response] =
    Routes(
      Method.GET / "health" -> handler(Response.json("""{"status":"UP"}""")),
      Method.GET / "health" / "ready" -> handler(
        probe.checkDatabase
          .timeoutFail(new RuntimeException("timeout"))(3.seconds)
          .foldZIO(
            err =>
              ZIO.logWarning(s"Readiness check failed: ${err.getMessage}")
                .as(json(Status.ServiceUnavailable, """{"status":"DOWN","checks":{"postgresql":"DOWN"}}""")),
            _ => ZIO.succeed(json(Status.Ok, """{"status":"UP","checks":{"postgresql":"UP"}}"""))
          )
      )
    )
