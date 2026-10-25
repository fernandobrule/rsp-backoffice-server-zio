package com.atacamasoft.rsp.backoffice.server.infrastructure.config

import zio.*

final case class HttpConfig(host: String, port: Int)
final case class DbConfig(url: String, user: String, password: String, schema: String, poolSize: Int)
final case class CorsConfig(allowedOrigins: List[String])
final case class AppConfig(http: HttpConfig, db: DbConfig, cors: CorsConfig)

object AppConfig:
  private def opt(name: String): Task[Option[String]] =
    System.env(name).map(_.map(_.trim).filter(_.nonEmpty))

  private def str(name: String, default: String): Task[String] = opt(name).map(_.getOrElse(default))

  private def int(name: String, default: Int): Task[Int] =
    str(name, default.toString).flatMap(s => ZIO.attempt(s.toInt).mapError(_ => new IllegalArgumentException(s"$name must be an integer, got '$s'")))

  /** Reads configuration from environment variables (defaults target the local docker-compose database). */
  val load: Task[AppConfig] =
    for
      host     <- str("HTTP_HOST", "0.0.0.0")
      port     <- int("HTTP_PORT", 8080)
      url      <- str("DB_URL", "jdbc:postgresql://localhost:5432/rsp_db_dev")
      user     <- str("DB_USER", "rsp_db_role_dev")
      pwdA     <- opt("DB_PASSWORD")
      pwdB     <- opt("POSTGRES_PASSWORD")
      schema   <- str("DB_SCHEMA", "rsp_db_schema_dev")
      pool     <- int("DB_POOL_SIZE", 10)
      origins  <- str("CORS_ALLOWED_ORIGINS", "*")
      _        <- ZIO.fail(new IllegalArgumentException(s"DB_SCHEMA '$schema' is not a valid identifier"))
                    .unless(schema.matches("[A-Za-z_][A-Za-z0-9_]*"))
    yield AppConfig(
      HttpConfig(host, port),
      DbConfig(url, user, pwdA.orElse(pwdB).getOrElse("rsp_dev_password"), schema, pool),
      CorsConfig(origins.split(",").toList.map(_.trim).filter(_.nonEmpty)))
