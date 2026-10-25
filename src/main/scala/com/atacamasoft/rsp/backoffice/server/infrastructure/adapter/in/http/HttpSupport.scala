package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http

import com.atacamasoft.rsp.backoffice.server.application.port.out.TokenVerifier
import com.atacamasoft.rsp.backoffice.server.domain.{DomainError, FieldIssue, GeoPoint, Page, PageRequest, Patch}

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex
import sttp.model.StatusCode
import sttp.tapir.{EndpointInput, EndpointOutput, Schema}
import sttp.tapir.ztapir.*
import sttp.tapir.json.zio.*
import zio.*
import zio.json.*

// ---------------------------------------------------------------- Error contract (components/schemas/Error)

final case class ErrorDetailDto(field: Option[String], issue: String) derives JsonCodec, Schema
final case class ErrorDto(code: String, message: String, requestId: Option[String], details: Option[List[ErrorDetailDto]])
    derives JsonCodec, Schema

type ApiError = (StatusCode, ErrorDto)

object ApiErrors:
  def from(e: DomainError, requestId: Option[String]): ApiError =
    def dto(code: String, msg: String, ds: List[FieldIssue] = Nil) =
      ErrorDto(code, msg, requestId, if ds.isEmpty then None else Some(ds.map(d => ErrorDetailDto(d.field, d.issue))))
    e match
      case DomainError.NotFound(r, id)       => StatusCode.NotFound -> dto("NOT_FOUND", s"$r '$id' was not found")
      case DomainError.Conflict(m)           => StatusCode.Conflict -> dto("CONFLICT", m)
      case DomainError.Unprocessable(m, ds)  => StatusCode.UnprocessableEntity -> dto("UNPROCESSABLE_ENTITY", m, ds)
      case DomainError.BadInput(m, ds)       => StatusCode.BadRequest -> dto("VALIDATION_ERROR", m, ds)
      case DomainError.Unauthorized(m)       => StatusCode.Unauthorized -> dto("UNAUTHORIZED", m)
      case DomainError.Internal(_, _)        => StatusCode.InternalServerError -> dto("INTERNAL_ERROR", "Unexpected server error")

// ---------------------------------------------------------------- Pagination

final case class PageQuery(limit: Option[Int], offset: Option[Int])

final case class PageDto[A](items: List[A], total: Long, limit: Int, offset: Int)

object PageDto:
  def of[A, D](p: Page[A])(f: A => D): PageDto[D] = PageDto(p.items.map(f), p.total, p.limit, p.offset)
  given [A](using JsonEncoder[A], JsonDecoder[A]): JsonCodec[PageDto[A]] = DeriveJsonCodec.gen[PageDto[A]]
  given [A](using Schema[A]): Schema[PageDto[A]] = Schema.derived[PageDto[A]]

// ---------------------------------------------------------------- Request context + shared endpoint base

final case class RequestContext(bearerToken: Option[String], requestId: Option[String], correlationId: Option[String])

final class HttpRuntime(verifier: TokenVerifier):

  private val requestContext: EndpointInput[RequestContext] =
    (auth.bearer[Option[String]]() and header[Option[String]]("X-Request-Id") and header[Option[String]]("X-Correlation-Id"))
      .mapTo[RequestContext]

  private val errorOutput: EndpointOutput[ApiError] = statusCode.and(jsonBody[ErrorDto])

  /** Every endpoint starts here: /api/v1 prefix, bearer token + tracing headers, uniform Error output. */
  val base = endpoint.in("api" / "v1").in(requestContext).errorOut(errorOutput)

  val pageInput: EndpointInput[PageQuery] =
    (query[Option[Int]]("limit").description("Maximum number of records to return (1-100). Defaults to 20.") and
      query[Option[Int]]("offset").description("Number of records to skip. Defaults to 0.")).mapTo[PageQuery]

  /** Authenticates the request, runs the use case and maps domain errors to the HTTP error contract. */
  def run[A](ctx: RequestContext)(io: IO[DomainError, A]): IO[ApiError, A] =
    verifier.verify(ctx.bearerToken).zipRight(io).mapError(ApiErrors.from(_, ctx.requestId))

  def pageRequest(q: PageQuery): IO[DomainError, PageRequest] =
    val limit = q.limit.getOrElse(20)
    val offset = q.offset.getOrElse(0)
    if limit < 1 || limit > 100 then ZIO.fail(DomainError.BadInput("Invalid pagination", List(FieldIssue(Some("limit"), "must be between 1 and 100"))))
    else if offset < 0 then ZIO.fail(DomainError.BadInput("Invalid pagination", List(FieldIssue(Some("offset"), "must be greater than or equal to 0"))))
    else ZIO.succeed(PageRequest(limit, offset))

  /** Parses an optional enum query parameter (400 when the value is unknown). */
  def enumQuery[E](name: String, raw: Option[String], values: Array[E]): IO[DomainError, Option[E]] =
    raw match
      case None => ZIO.succeed(None)
      case Some(r) =>
        values.find(_.toString == r) match
          case Some(v) => ZIO.succeed(Some(v))
          case None    => ZIO.fail(DomainError.BadInput("Invalid parameter", List(FieldIssue(Some(name), s"must be one of ${values.mkString("[", ", ", "]")}"))))

// ---------------------------------------------------------------- Request body validation

/** Accumulates field issues of a request body (OpenAPI schema constraints -> 400). */
final class Check:
  private val issues = ListBuffer.empty[FieldIssue]
  private val Email: Regex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$".r

  def fail(field: String, msg: String): Unit = issues += FieldIssue(Some(field), msg)

  def text(field: String, max: Int)(v: String): Option[String] =
    if v.length > max then { fail(field, s"must have at most $max characters"); None } else Some(v)

  def email(field: String, max: Int)(v: String): Option[String] =
    text(field, max)(v).filter { s =>
      val ok = Email.matches(s)
      if !ok then fail(field, "must be a valid e-mail address")
      ok
    }

  def uri(field: String)(v: String): Option[String] =
    scala.util.Try(java.net.URI(v)).toOption.filter(_.getScheme != null) match
      case some @ Some(_) => Some(v)
      case None           => fail(field, "must be a valid URI"); None

  def pattern(field: String, re: Regex, msg: String)(v: String): Option[String] =
    if re.matches(v) then Some(v) else { fail(field, msg); None }

  def int(field: String, min: Int, max: Int)(v: Int): Option[Int] =
    if v < min || v > max then { fail(field, s"must be between $min and $max"); None } else Some(v)

  def dec(field: String, nonNegative: Boolean, cents: Boolean)(v: BigDecimal): Option[BigDecimal] =
    if nonNegative && v < 0 then { fail(field, "must be greater than or equal to 0"); None }
    else if cents && v.bigDecimal.stripTrailingZeros.scale > 2 then { fail(field, "must be a multiple of 0.01"); None }
    else Some(v)

  def geo(field: String)(g: GeoPointDto): Option[GeoPoint] =
    if g.latitude < -90 || g.latitude > 90 then { fail(s"$field.latitude", "must be between -90 and 90"); None }
    else if g.longitude < -180 || g.longitude > 180 then { fail(s"$field.longitude", "must be between -180 and 180"); None }
    else Some(GeoPoint(g.latitude, g.longitude))

  def en[E](field: String, values: Array[E])(v: String): Option[E] =
    values.find(_.toString == v) match
      case some @ Some(_) => some
      case None           => fail(field, s"must be one of ${values.mkString("[", ", ", "]")}"); None

  // PATCH helpers ---------------------------------------------------------
  /** Non-nullable property: absent -> None, null -> issue, value -> validated. */
  def pOpt[A, B](field: String, p: Patch[A])(conv: A => Option[B]): Option[B] = p match
    case Patch.Keep      => None
    case Patch.Clear     => fail(field, "must not be null"); None
    case Patch.Assign(a) => conv(a)

  /** Nullable property: absent -> Keep, null -> Clear, value -> validated. */
  def pNul[A, B](field: String, p: Patch[A])(conv: A => Option[B]): Patch[B] = p match
    case Patch.Keep      => Patch.Keep
    case Patch.Clear     => Patch.Clear
    case Patch.Assign(a) => conv(a) match
      case Some(b) => Patch.Assign(b)
      case None    => Patch.Keep

  def atLeastOne(ps: Patch[?]*): Unit =
    if !ps.exists(_.isPresent) then fail("body", "at least one property must be provided")

  def done[A](build: => A): Either[DomainError, A] =
    if issues.nonEmpty then Left(DomainError.BadInput("Invalid request body", issues.toList)) else Right(build)

object Check:
  val StateCode: Regex = "^[A-Z]{2}$".r
  val Currency: Regex = "^[A-Z]{3}$".r
