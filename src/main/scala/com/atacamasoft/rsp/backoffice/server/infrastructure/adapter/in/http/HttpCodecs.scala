package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http

import com.atacamasoft.rsp.backoffice.server.domain.Patch
import sttp.tapir.{Schema, SchemaType}
import zio.json.*
import zio.json.ast.Json
import zio.json.internal.RetractReader

/** Codecs/schemas shared by every DTO (import with `import HttpCodecs.given`). */
object HttpCodecs:

  // PATCH semantics: missing property -> Keep, explicit null -> Clear, value -> Assign.
  given patchDecoder[A](using d: JsonDecoder[A]): JsonDecoder[Patch[A]] = new JsonDecoder[Patch[A]]:
    private val opt: JsonDecoder[Option[A]] = JsonDecoder.option[A](using d)
    override def unsafeDecodeMissing(trace: List[JsonError]): Patch[A] = Patch.Keep
    override def unsafeDecode(trace: List[JsonError], in: RetractReader): Patch[A] =
      opt.unsafeDecode(trace, in) match
        case None    => Patch.Clear
        case Some(a) => Patch.Assign(a)

  given patchEncoder[A](using e: JsonEncoder[A]): JsonEncoder[Patch[A]] =
    JsonEncoder.option[A](using e).contramap[Patch[A]] {
      case Patch.Assign(a) => Some(a)
      case _               => None
    }

  given patchSchema[A](using s: Schema[A]): Schema[Patch[A]] =
    s.asInstanceOf[Schema[Patch[A]]].copy(isOptional = true)

/** Free-form JSON value (jsonb `attributes`). */
final case class JsonObjectDto(json: Json)

object JsonObjectDto:
  given JsonEncoder[JsonObjectDto] = summon[JsonEncoder[Json]].contramap(_.json)
  given JsonDecoder[JsonObjectDto] = summon[JsonDecoder[Json]].map(JsonObjectDto(_))
  given Schema[JsonObjectDto] = Schema(SchemaType.SProduct[JsonObjectDto](Nil))

final case class GeoPointDto(latitude: Double, longitude: Double) derives JsonCodec, Schema
