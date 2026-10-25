package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence

import com.atacamasoft.rsp.backoffice.server.domain.{GeoPoint, Patch}

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

type Bind = (PreparedStatement, Int) => Unit

/** SQL fragment with positional bind functions (one per `?`). */
final case class Frag(sql: String, binds: List[Bind] = Nil)

object Frag:
  private def one(b: Bind): Frag = Frag("?", List(b))
  val Null: Frag = Frag("NULL")
  def uuid(v: UUID): Frag = one((ps, i) => ps.setObject(i, v))
  def text(v: String): Frag = one((ps, i) => ps.setString(i, v))
  def ts(v: Instant): Frag = one((ps, i) => ps.setObject(i, OffsetDateTime.ofInstant(v, ZoneOffset.UTC)))
  def dec(v: BigDecimal): Frag = one((ps, i) => ps.setBigDecimal(i, v.bigDecimal))
  def int(v: Int): Frag = one((ps, i) => ps.setInt(i, v))
  def bool(v: Boolean): Frag = one((ps, i) => ps.setBoolean(i, v))
  def jsonb(v: String): Frag = Frag("?::jsonb", List((ps, i) => ps.setString(i, v)))
  def geography(g: GeoPoint): Frag =
    Frag("ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography",
      List((ps, i) => ps.setDouble(i, g.longitude), (ps, i) => ps.setDouble(i, g.latitude)))

type Cols = List[(String, Frag)]

extension [A](o: Option[A])
  /** Column assignment when the option is defined (omitted otherwise, so DB defaults apply / value is kept). */
  def col(name: String)(f: A => Frag): Cols = o.toList.map(a => name -> f(a))
  /** WHERE condition when the option is defined. */
  def cond(sql: String)(f: A => Frag): List[Frag] = o.toList.map(a => Frag(sql, f(a).binds))

extension [A](p: Patch[A])
  def col(name: String)(f: A => Frag): Cols = p match
    case Patch.Keep      => Nil
    case Patch.Clear     => List(name -> Frag.Null)
    case Patch.Assign(a) => List(name -> f(a))

extension (rs: ResultSet)
  def uuid(c: String): UUID = rs.getObject(c, classOf[UUID])
  def str(c: String): String = rs.getString(c)
  def strOpt(c: String): Option[String] = Option(rs.getString(c))
  def instant(c: String): Instant = rs.getObject(c, classOf[OffsetDateTime]).toInstant
  def instantOpt(c: String): Option[Instant] = Option(rs.getObject(c, classOf[OffsetDateTime])).map(_.toInstant)
  def dec(c: String): BigDecimal = BigDecimal(rs.getBigDecimal(c))
  def decOpt(c: String): Option[BigDecimal] = Option(rs.getBigDecimal(c)).map(BigDecimal(_))
  def intOpt(c: String): Option[Int] = { val v = rs.getInt(c); if rs.wasNull then None else Some(v) }
  def doubleOpt(c: String): Option[Double] = { val v = rs.getDouble(c); if rs.wasNull then None else Some(v) }

object Sql:
  def intBind(v: Int): Bind = (ps, i) => ps.setInt(i, v)

  private def bindAll(ps: PreparedStatement, frags: List[Frag], extra: List[Bind]): Unit =
    var i = 1
    (frags.flatMap(_.binds) ++ extra).foreach { b => b(ps, i); i += 1 }

  def query[A](c: Connection, sql: String, frags: List[Frag], extra: List[Bind] = Nil)(read: ResultSet => A): List[A] =
    val ps = c.prepareStatement(sql)
    try
      bindAll(ps, frags, extra)
      val rs = ps.executeQuery()
      try
        val out = List.newBuilder[A]
        while rs.next() do out += read(rs)
        out.result()
      finally rs.close()
    finally ps.close()

  def exec(c: Connection, sql: String, frags: List[Frag]): Int =
    val ps = c.prepareStatement(sql)
    try
      bindAll(ps, frags, Nil)
      ps.executeUpdate()
    finally ps.close()

/** Binds a Scala 3 enum value as its name (all enum columns are varchar + check constraint). */
def en(v: scala.reflect.Enum): Frag = Frag.text(v.toString)
