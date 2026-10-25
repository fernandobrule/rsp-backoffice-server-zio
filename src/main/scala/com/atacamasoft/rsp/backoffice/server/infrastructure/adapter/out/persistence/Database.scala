package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence

import com.atacamasoft.rsp.backoffice.server.application.port.out.HealthProbe
import com.atacamasoft.rsp.backoffice.server.domain.DomainError
import com.atacamasoft.rsp.backoffice.server.infrastructure.config.DbConfig
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.{Connection, SQLException}
import org.postgresql.util.PSQLException
import zio.*

enum Op:
  case Read, Write, Remove

object SqlErrors:
  def toDomain(op: Op, t: Throwable): DomainError = t match
    case e: SQLException =>
      val constraint = e match
        case p: PSQLException => Option(p.getServerErrorMessage).flatMap(m => Option(m.getConstraint))
        case _                => None
      val on = constraint.map(c => s" (constraint $c)").getOrElse("")
      Option(e.getSQLState).getOrElse("") match
        case "23505" => DomainError.Conflict(s"A record with the same unique value already exists$on")
        case "23503" =>
          if op == Op.Remove then DomainError.Conflict(s"Other records still reference this record$on")
          else DomainError.Unprocessable(s"A referenced record does not exist$on")
        case "23514" | "23502" => DomainError.Unprocessable(s"The data violates an integrity rule$on")
        case s if s.startsWith("22") => DomainError.Unprocessable(s"A value is out of range or has an invalid format$on")
        case _ => DomainError.Internal(s"Database error: ${e.getMessage}", Some(e))
    case other => DomainError.Internal(s"Unexpected persistence error: ${other.getMessage}", Some(other))

final class Database(val ds: HikariDataSource) extends HealthProbe:
  def run[A](op: Op)(f: Connection => A): IO[DomainError, A] =
    ZIO.attemptBlocking {
      val c = ds.getConnection()
      try f(c) finally c.close()
    }.mapError(SqlErrors.toDomain(op, _)).tapError {
      case DomainError.Internal(m, _) => ZIO.logError(m)
      case _                          => ZIO.unit
    }

  override def checkDatabase: IO[Throwable, Unit] =
    ZIO.attemptBlocking {
      val c = ds.getConnection()
      try
        val st = c.createStatement()
        try st.execute("select 1") finally st.close()
      finally c.close()
    }.unit

object Database:
  private def hikari(cfg: DbConfig): HikariConfig =
    val hc = new HikariConfig()
    hc.setPoolName("rsp-hikari")
    hc.setJdbcUrl(cfg.url)
    hc.setUsername(cfg.user)
    hc.setPassword(cfg.password)
    hc.setMaximumPoolSize(cfg.poolSize)
    hc.setConnectionTimeout(5000)
    // Start even when PostgreSQL is down: /health stays UP, /health/ready reports DOWN.
    hc.setInitializationFailTimeout(-1)
    hc.setConnectionInitSql(s"SET search_path TO ${cfg.schema}, public")
    hc

  def make(cfg: DbConfig): ZIO[Scope, Throwable, Database] =
    ZIO.acquireRelease(
      ZIO.attemptBlocking(new Database(new HikariDataSource(hikari(cfg))))
    )(db => ZIO.attemptBlocking(db.ds.close()).ignore)
