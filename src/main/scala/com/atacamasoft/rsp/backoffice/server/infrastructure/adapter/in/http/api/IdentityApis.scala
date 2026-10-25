package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.api

import com.atacamasoft.rsp.backoffice.server.application.port.in.{AgencyUseCase, AgentUseCase, UserUseCase}
import com.atacamasoft.rsp.backoffice.server.domain.{Agency, AgencyCreate, AgencyFilter, AgencyPatch, AgencyStatus, Agent, AgentCreate, AgentFilter, AgentId, AgentPatch, AgentRole, DocumentType, DomainError, Patch, User, UserCreate, UserFilter, UserPatch, UserStatus}
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.HttpCodecs.given
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.{Check, HttpRuntime, PageDto}

import java.time.Instant
import java.util.UUID
import sttp.model.StatusCode
import sttp.tapir.Schema
import sttp.tapir.json.zio.*
import sttp.tapir.ztapir.*
import zio.*
import zio.json.*

// =================================================================== Users

final case class UserDto(
    id: UUID, firstName: String, lastName: String, email: String, emailVerifiedAt: Option[Instant], phone: Option[String],
    keycloakId: UUID, status: String, verifiedAt: Option[Instant], createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object UserDto:
  def from(u: User): UserDto = UserDto(u.id, u.firstName, u.lastName, u.email, u.emailVerifiedAt, u.phone, u.keycloakId,
    u.status.toString, u.verifiedAt, u.createdAt, u.updatedAt)

final case class UserCreateDto(
    firstName: String, lastName: String, email: String, emailVerifiedAt: Option[Instant], phone: Option[String],
    keycloakId: UUID, status: Option[String], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class UserUpdateDto(
    firstName: Patch[String] = Patch.Keep, lastName: Patch[String] = Patch.Keep, email: Patch[String] = Patch.Keep,
    emailVerifiedAt: Patch[Instant] = Patch.Keep, phone: Patch[String] = Patch.Keep, keycloakId: Patch[UUID] = Patch.Keep,
    status: Patch[String] = Patch.Keep, verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object UserMapper:
  def create(d: UserCreateDto): Either[DomainError, UserCreate] =
    val c = Check()
    val fn = c.text("firstName", 100)(d.firstName)
    val ln = c.text("lastName", 100)(d.lastName)
    val em = c.email("email", 254)(d.email)
    val ph = d.phone.flatMap(c.text("phone", 30))
    val st = d.status.flatMap(c.en("status", UserStatus.values))
    c.done(UserCreate(fn.get, ln.get, em.get, d.emailVerifiedAt, ph, d.keycloakId, st, d.verifiedAt))

  def update(d: UserUpdateDto): Either[DomainError, UserPatch] =
    val c = Check()
    c.atLeastOne(d.firstName, d.lastName, d.email, d.emailVerifiedAt, d.phone, d.keycloakId, d.status, d.verifiedAt)
    val p = UserPatch(
      firstName = c.pOpt("firstName", d.firstName)(c.text("firstName", 100)),
      lastName = c.pOpt("lastName", d.lastName)(c.text("lastName", 100)),
      email = c.pOpt("email", d.email)(c.email("email", 254)),
      emailVerifiedAt = d.emailVerifiedAt,
      phone = c.pNul("phone", d.phone)(c.text("phone", 30)),
      keycloakId = c.pOpt("keycloakId", d.keycloakId)(Some(_)),
      status = c.pOpt("status", d.status)(c.en("status", UserStatus.values)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class UserApi(uc: UserUseCase, rt: HttpRuntime):
  private val users = rt.base.in("users").tag("Users")
  private val byId = users.in(path[UUID]("userId"))

  val listEp = users.get
    .in(rt.pageInput).in(query[Option[String]]("status")).in(query[Option[String]]("email"))
    .out(jsonBody[PageDto[UserDto]]).name("listUsers").summary("List users")
    .zServerLogic[Any] { case (ctx, page, status, email) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        st <- rt.enumQuery("status", status, UserStatus.values)
        r  <- uc.list(UserFilter(st, email), p)
      yield PageDto.of(r)(UserDto.from))
    }

  val createEp = users.post
    .in(jsonBody[UserCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[UserDto])
    .name("createUser").summary("Create a user")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(UserMapper.create(body)).flatMap(uc.create).map(u => (s"/api/v1/users/${u.id}", UserDto.from(u))))
    }

  val getEp = byId.get.out(jsonBody[UserDto]).name("getUser").summary("Get a user by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(UserDto.from)) }

  val updateEp = byId.patch.in(jsonBody[UserUpdateDto]).out(jsonBody[UserDto]).name("updateUser").summary("Partially update a user")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(UserMapper.update(body)).flatMap(uc.update(id, _)).map(UserDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteUser").summary("Delete a user")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Agencies

final case class AgencyDto(
    id: UUID, name: String, documentNumber: Option[String], documentType: String, creciNumber: Option[String],
    logoUrl: Option[String], contactEmail: Option[String], status: String, verifiedAt: Option[Instant],
    createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object AgencyDto:
  def from(a: Agency): AgencyDto = AgencyDto(a.id, a.name, a.documentNumber, a.documentType.toString, a.creciNumber, a.logoUrl,
    a.contactEmail, a.status.toString, a.verifiedAt, a.createdAt, a.updatedAt)

final case class AgencyCreateDto(
    name: String, documentNumber: Option[String], documentType: Option[String], creciNumber: Option[String],
    logoUrl: Option[String], contactEmail: Option[String], status: Option[String], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class AgencyUpdateDto(
    name: Patch[String] = Patch.Keep, documentNumber: Patch[String] = Patch.Keep, documentType: Patch[String] = Patch.Keep,
    creciNumber: Patch[String] = Patch.Keep, logoUrl: Patch[String] = Patch.Keep, contactEmail: Patch[String] = Patch.Keep,
    status: Patch[String] = Patch.Keep, verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object AgencyMapper:
  def create(d: AgencyCreateDto): Either[DomainError, AgencyCreate] =
    val c = Check()
    val name = c.text("name", 255)(d.name)
    val doc = d.documentNumber.flatMap(c.text("documentNumber", 20))
    val dt = d.documentType.flatMap(c.en("documentType", DocumentType.values))
    val creci = d.creciNumber.flatMap(c.text("creciNumber", 50))
    val logo = d.logoUrl.flatMap(c.uri("logoUrl"))
    val mail = d.contactEmail.flatMap(c.email("contactEmail", 255))
    val st = d.status.flatMap(c.en("status", AgencyStatus.values))
    c.done(AgencyCreate(name.get, doc, dt, creci, logo, mail, st, d.verifiedAt))

  def update(d: AgencyUpdateDto): Either[DomainError, AgencyPatch] =
    val c = Check()
    c.atLeastOne(d.name, d.documentNumber, d.documentType, d.creciNumber, d.logoUrl, d.contactEmail, d.status, d.verifiedAt)
    val p = AgencyPatch(
      name = c.pOpt("name", d.name)(c.text("name", 255)),
      documentNumber = c.pNul("documentNumber", d.documentNumber)(c.text("documentNumber", 20)),
      documentType = c.pOpt("documentType", d.documentType)(c.en("documentType", DocumentType.values)),
      creciNumber = c.pNul("creciNumber", d.creciNumber)(c.text("creciNumber", 50)),
      logoUrl = c.pNul("logoUrl", d.logoUrl)(c.uri("logoUrl")),
      contactEmail = c.pNul("contactEmail", d.contactEmail)(c.email("contactEmail", 255)),
      status = c.pOpt("status", d.status)(c.en("status", AgencyStatus.values)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class AgencyApi(uc: AgencyUseCase, rt: HttpRuntime):
  private val agencies = rt.base.in("agencies").tag("Agencies")
  private val byId = agencies.in(path[UUID]("agencyId"))

  val listEp = agencies.get
    .in(rt.pageInput).in(query[Option[String]]("status")).in(query[Option[String]]("documentNumber")).in(query[Option[String]]("creciNumber"))
    .out(jsonBody[PageDto[AgencyDto]]).name("listAgencies").summary("List agencies")
    .zServerLogic[Any] { case (ctx, page, status, doc, creci) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        st <- rt.enumQuery("status", status, AgencyStatus.values)
        r  <- uc.list(AgencyFilter(st, doc, creci), p)
      yield PageDto.of(r)(AgencyDto.from))
    }

  val createEp = agencies.post
    .in(jsonBody[AgencyCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[AgencyDto])
    .name("createAgency").summary("Create a agency")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(AgencyMapper.create(body)).flatMap(uc.create).map(a => (s"/api/v1/agencies/${a.id}", AgencyDto.from(a))))
    }

  val getEp = byId.get.out(jsonBody[AgencyDto]).name("getAgency").summary("Get a agency by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(AgencyDto.from)) }

  val updateEp = byId.patch.in(jsonBody[AgencyUpdateDto]).out(jsonBody[AgencyDto]).name("updateAgency").summary("Partially update a agency")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(AgencyMapper.update(body)).flatMap(uc.update(id, _)).map(AgencyDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteAgency").summary("Delete a agency")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Agents (identified by agencyId + userId)

final case class AgentDto(
    agencyId: UUID, userId: UUID, role: String, licenseNumber: String, bio: Option[String], verifiedAt: Option[Instant],
    rating: BigDecimal, createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object AgentDto:
  def from(a: Agent): AgentDto = AgentDto(a.agencyId, a.userId, a.role.toString, a.licenseNumber, a.bio, a.verifiedAt,
    a.rating, a.createdAt, a.updatedAt)

final case class AgentCreateDto(
    userId: UUID, role: String, licenseNumber: String, bio: Option[String], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class AgentUpdateDto(
    role: Patch[String] = Patch.Keep, licenseNumber: Patch[String] = Patch.Keep, bio: Patch[String] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object AgentMapper:
  def create(agencyId: UUID, d: AgentCreateDto): Either[DomainError, AgentCreate] =
    val c = Check()
    val role = c.en("role", AgentRole.values)(d.role)
    val lic = c.text("licenseNumber", 100)(d.licenseNumber)
    c.done(AgentCreate(agencyId, d.userId, role.get, lic.get, d.bio, d.verifiedAt))

  def update(d: AgentUpdateDto): Either[DomainError, AgentPatch] =
    val c = Check()
    c.atLeastOne(d.role, d.licenseNumber, d.bio, d.verifiedAt)
    val p = AgentPatch(
      role = c.pOpt("role", d.role)(c.en("role", AgentRole.values)),
      licenseNumber = c.pOpt("licenseNumber", d.licenseNumber)(c.text("licenseNumber", 100)),
      bio = d.bio, verifiedAt = d.verifiedAt)
    c.done(p)

final class AgentApi(uc: AgentUseCase, rt: HttpRuntime):
  private val agents = rt.base.in("agencies" / path[UUID]("agencyId") / "agents").tag("Agents")
  private val byId = agents.in(path[UUID]("userId"))

  val listEp = agents.get
    .in(rt.pageInput).in(query[Option[String]]("role"))
    .out(jsonBody[PageDto[AgentDto]]).name("listAgents").summary("List agents")
    .zServerLogic[Any] { case (ctx, agencyId, page, role) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        ro <- rt.enumQuery("role", role, AgentRole.values)
        r  <- uc.list(AgentFilter(agencyId, ro), p)
      yield PageDto.of(r)(AgentDto.from))
    }

  val createEp = agents.post
    .in(jsonBody[AgentCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[AgentDto])
    .name("createAgent").summary("Create a agent")
    .zServerLogic[Any] { case (ctx, agencyId, body) =>
      rt.run(ctx)(ZIO.fromEither(AgentMapper.create(agencyId, body)).flatMap(uc.create)
        .map(a => (s"/api/v1/agencies/${a.agencyId}/agents/${a.userId}", AgentDto.from(a))))
    }

  val getEp = byId.get.out(jsonBody[AgentDto]).name("getAgent").summary("Get a agent by id")
    .zServerLogic[Any] { case (ctx, agencyId, userId) => rt.run(ctx)(uc.get(AgentId(agencyId, userId)).map(AgentDto.from)) }

  val updateEp = byId.patch.in(jsonBody[AgentUpdateDto]).out(jsonBody[AgentDto]).name("updateAgent").summary("Partially update a agent")
    .zServerLogic[Any] { case (ctx, agencyId, userId, body) =>
      rt.run(ctx)(ZIO.fromEither(AgentMapper.update(body)).flatMap(uc.update(AgentId(agencyId, userId), _)).map(AgentDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteAgent").summary("Delete a agent")
    .zServerLogic[Any] { case (ctx, agencyId, userId) => rt.run(ctx)(uc.delete(AgentId(agencyId, userId))) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)
