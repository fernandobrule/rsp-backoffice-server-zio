package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http

import java.util.UUID
import zio.*
import zio.http.*
import zio.json.*

/**
 * Cross-cutting concerns for every response:
 *  - X-Request-Id / X-Correlation-Id: reuse the incoming values or generate them, expose them to the endpoints and echo them back
 *  - access log (method, path, status, latency)
 *  - CORS headers
 *  - Tapir's plain-text 400 (decode failures) is rewritten to the `Error` JSON contract
 */
object RequestContextMiddleware:
  private val RequestId = "X-Request-Id"
  private val CorrelationId = "X-Correlation-Id"

  private final case class State(requestId: String, correlationId: String, method: String, path: String, origin: Option[String], startNanos: Long)

  private def isPlainText(resp: Response): Boolean =
    resp.headers.get("Content-Type").exists(_.toLowerCase.startsWith("text/plain"))

  private def asErrorJson(resp: Response, rid: String): UIO[Response] =
    if resp.status == Status.BadRequest && isPlainText(resp) then
      resp.body.asString.orElseSucceed("Bad request").map { msg =>
        val body = ErrorDto("VALIDATION_ERROR", msg, Some(rid), None).toJson
        Response(status = Status.BadRequest, headers = Headers(Header.ContentType(MediaType.application.json)), body = Body.fromString(body))
      }
    else ZIO.succeed(resp)

  def apply(cors: CorsPolicy): Middleware[Any] =
  //def apply(cors: CorsPolicy): HandlerAspect[Any, Unit] =
    HandlerAspect.interceptHandlerStateful(
      Handler.fromFunctionZIO[Request] { req =>
        Clock.nanoTime.map { start =>
          val rid = req.headers.get(RequestId).filter(_.nonEmpty).getOrElse(UUID.randomUUID().toString)
          val cid = req.headers.get(CorrelationId).filter(_.nonEmpty).getOrElse(rid)
          val enriched = req.addHeader(RequestId, rid).addHeader(CorrelationId, cid)
          (State(rid, cid, req.method.name, req.url.path.encode, req.headers.get("Origin"), start), (enriched, ()))
        }
      }
    )(
      Handler.fromFunctionZIO[(State, Response)] { case (st, resp) =>
        for
          end   <- Clock.nanoTime
          fixed <- asErrorJson(resp, st.requestId)
          out    = cors.decorate(fixed.addHeader(RequestId, st.requestId).addHeader(CorrelationId, st.correlationId), st.origin)
          _     <- ZIO.logInfo(s"${st.method} ${st.path} -> ${out.status.code} (${(end - st.startNanos) / 1000000} ms)") @@
                     ZIOAspect.annotated("request_id", st.requestId)
        yield out
      }
    )
