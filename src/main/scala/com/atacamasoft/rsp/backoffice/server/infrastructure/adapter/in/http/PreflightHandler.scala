package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http

import com.atacamasoft.rsp.backoffice.server.infrastructure.config.CorsConfig
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.trailing

/** CORS policy shared by the preflight handler and the response decorator. */
final class CorsPolicy(cfg: CorsConfig):
  val allowedMethods = "GET, POST, PATCH, DELETE, OPTIONS"
  val defaultAllowedHeaders = "Authorization, Content-Type, X-Request-Id, X-Correlation-Id"
  val exposedHeaders = "Location, X-Request-Id, X-Correlation-Id"

  def allowOrigin(origin: Option[String]): Option[String] =
    if cfg.allowedOrigins.contains("*") then Some("*") else origin.filter(cfg.allowedOrigins.contains)

  /** Adds CORS headers to a regular (non-preflight) response. */
  def decorate(resp: Response, origin: Option[String]): Response =
    allowOrigin(origin) match
      case Some(o) => resp.addHeader("Access-Control-Allow-Origin", o).addHeader("Access-Control-Expose-Headers", exposedHeaders).addHeader("Vary", "Origin")
      case None    => resp

/**
 * Handles every HTTP OPTIONS request, whatever the path.
 *  - CORS preflight (Origin + Access-Control-Request-Method): 204 with the Access-Control-* headers, or 403 when the origin is not allowed.
 *  - Plain OPTIONS: 204 with an `Allow` header.
 */
object PreflightHandler:

  def response(req: Request, cors: CorsPolicy): Response =
    val origin = req.headers.get("Origin")
    val requestedMethod = req.headers.get("Access-Control-Request-Method")
    val requestedHeaders = req.headers.get("Access-Control-Request-Headers")
    val plain = Response(status = Status.NoContent).addHeader("Allow", cors.allowedMethods)
    requestedMethod match
      case None => plain
      case Some(_) =>
        cors.allowOrigin(origin) match
          case None => Response(status = Status.Forbidden)
          case Some(o) =>
            plain
              .addHeader("Access-Control-Allow-Origin", o)
              .addHeader("Access-Control-Allow-Methods", cors.allowedMethods)
              .addHeader("Access-Control-Allow-Headers", requestedHeaders.getOrElse(cors.defaultAllowedHeaders))
              .addHeader("Access-Control-Max-Age", "3600")
              .addHeader("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers")

  def routes(cors: CorsPolicy): Routes[Any, Response] =
    Routes(
      Method.OPTIONS / trailing -> handler { (_: Path, req: Request) => response(req, cors) }
    )
