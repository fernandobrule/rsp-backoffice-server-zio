package com.atacamasoft.rsp.backoffice.server.domain

import java.time.Instant
import java.util.UUID

// ---- Users ----
enum UserStatus:
  case ACTIVE, SUSPENDED, DELETED

final case class User(
    id: UUID, firstName: String, lastName: String, email: String, emailVerifiedAt: Option[Instant],
    phone: Option[String], keycloakId: UUID, status: UserStatus, createdAt: Instant, updatedAt: Instant,
    verifiedAt: Option[Instant])

final case class UserCreate(
    firstName: String, lastName: String, email: String, emailVerifiedAt: Option[Instant], phone: Option[String],
    keycloakId: UUID, status: Option[UserStatus], verifiedAt: Option[Instant])

final case class UserPatch(
    firstName: Option[String] = None, lastName: Option[String] = None, email: Option[String] = None,
    emailVerifiedAt: Patch[Instant] = Patch.Keep, phone: Patch[String] = Patch.Keep,
    keycloakId: Option[UUID] = None, status: Option[UserStatus] = None, verifiedAt: Patch[Instant] = Patch.Keep)

final case class UserFilter(status: Option[UserStatus], email: Option[String])

// ---- Agencies ----
enum DocumentType:
  case CPF, CNPJ

enum AgencyStatus:
  case PENDING, VERIFIED, SUSPENDED

final case class Agency(
    id: UUID, name: String, documentNumber: Option[String], documentType: DocumentType,
    creciNumber: Option[String], logoUrl: Option[String], contactEmail: Option[String], status: AgencyStatus,
    createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class AgencyCreate(
    name: String, documentNumber: Option[String], documentType: Option[DocumentType], creciNumber: Option[String],
    logoUrl: Option[String], contactEmail: Option[String], status: Option[AgencyStatus], verifiedAt: Option[Instant])

final case class AgencyPatch(
    name: Option[String] = None, documentNumber: Patch[String] = Patch.Keep, documentType: Option[DocumentType] = None,
    creciNumber: Patch[String] = Patch.Keep, logoUrl: Patch[String] = Patch.Keep,
    contactEmail: Patch[String] = Patch.Keep, status: Option[AgencyStatus] = None, verifiedAt: Patch[Instant] = Patch.Keep)

final case class AgencyFilter(status: Option[AgencyStatus], documentNumber: Option[String], creciNumber: Option[String])

// ---- Agents ----
enum AgentRole:
  case OWNER, ADMIN, AGENT

final case class AgentId(agencyId: UUID, userId: UUID)

final case class Agent(
    agencyId: UUID, userId: UUID, role: AgentRole, licenseNumber: String, bio: Option[String], rating: BigDecimal,
    createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class AgentCreate(
    agencyId: UUID, userId: UUID, role: AgentRole, licenseNumber: String, bio: Option[String], verifiedAt: Option[Instant])

final case class AgentPatch(
    role: Option[AgentRole] = None, licenseNumber: Option[String] = None, bio: Patch[String] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep)

final case class AgentFilter(agencyId: UUID, role: Option[AgentRole])
