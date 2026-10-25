package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence

import com.atacamasoft.rsp.backoffice.server.application.port.out.CrudRepository
import com.atacamasoft.rsp.backoffice.server.domain.{DomainError, Page, PageRequest}

import java.sql.ResultSet
import zio.*

/** Declarative description of how an aggregate maps to a table. */
final case class TableDef[Id, E, C, P, F](
    table: String,
    selectCols: List[String],
    idWhere: Id => List[Frag],
    read: ResultSet => E,
    insert: C => Cols,
    patch: P => Cols,
    filter: F => List[Frag],
    orderBy: String,
    updatedAtCol: String)

class JdbcCrudRepository[Id, E, C, P, F](db: Database, t: TableDef[Id, E, C, P, F], resource: String, show: Id => String)
    extends CrudRepository[Id, E, C, P, F]:

  private val cols = t.selectCols.mkString(", ")

  private def where(fs: List[Frag]): String =
    if fs.isEmpty then "" else fs.map(_.sql).mkString(" where ", " and ", "")

  private def orNotFound(id: Id)(o: Option[E]): IO[DomainError, E] =
    ZIO.fromOption(o).orElseFail(DomainError.NotFound(resource, show(id)))

  def list(filter: F, page: PageRequest): IO[DomainError, Page[E]] =
    db.run(Op.Read) { c =>
      val fs = t.filter(filter)
      val w = where(fs)
      val total = Sql.query(c, s"select count(*) from ${t.table}$w", fs)(_.getLong(1)).headOption.getOrElse(0L)
      val items = Sql.query(
        c, s"select $cols from ${t.table}$w order by ${t.orderBy} limit ? offset ?", fs,
        List(Sql.intBind(page.limit), Sql.intBind(page.offset)))(t.read)
      Page(items, total, page.limit, page.offset)
    }

  def get(id: Id): IO[DomainError, E] =
    db.run(Op.Read) { c =>
      val fs = t.idWhere(id)
      Sql.query(c, s"select $cols from ${t.table}${where(fs)}", fs)(t.read).headOption
    }.flatMap(orNotFound(id))

  def create(cmd: C): IO[DomainError, E] =
    db.run(Op.Write) { c =>
      val cs = t.insert(cmd)
      val sql =
        if cs.isEmpty then s"insert into ${t.table} default values returning $cols"
        else s"insert into ${t.table} (${cs.map(_._1).mkString(", ")}) values (${cs.map(_._2.sql).mkString(", ")}) returning $cols"
      Sql.query(c, sql, cs.map(_._2))(t.read).head
    }

  def update(id: Id, patch: P): IO[DomainError, E] =
    db.run(Op.Write) { c =>
      val cs = t.patch(patch)
      val sets = cs.map((n, f) => s"$n = ${f.sql}") :+ s"${t.updatedAtCol} = now()"
      val idf = t.idWhere(id)
      Sql.query(c, s"update ${t.table} set ${sets.mkString(", ")}${where(idf)} returning $cols", cs.map(_._2) ++ idf)(t.read).headOption
    }.flatMap(orNotFound(id))

  def delete(id: Id): IO[DomainError, Unit] =
    db.run(Op.Remove) { c =>
      val fs = t.idWhere(id)
      Sql.exec(c, s"delete from ${t.table}${where(fs)}", fs)
    }.flatMap(n => ZIO.fail(DomainError.NotFound(resource, show(id))).when(n == 0).unit)
