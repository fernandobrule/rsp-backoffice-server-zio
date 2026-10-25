package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence

import com.atacamasoft.rsp.backoffice.server.domain.{Agency, AgencyCreate, AgencyFilter, AgencyPatch, AgencyStatus, Agent, AgentCreate, AgentFilter, AgentId, AgentPatch, AgentRole, DocumentType, User, UserCreate, UserFilter, UserPatch, UserStatus}

import java.util.UUID


object UserTable:
  def apply(schema: String): TableDef[UUID, User, UserCreate, UserPatch, UserFilter] = TableDef(
    table = s"$schema.users",
    selectCols = List("user_id", "user_first_name", "user_last_name", "user_email", "user_email_verified_at", "user_phone",
      "user_keycloak_id", "user_status", "user_created_at", "user_updated_at", "user_verified_at"),
    idWhere = id => List(Frag("user_id = ?", Frag.uuid(id).binds)),
    read = rs => User(rs.uuid("user_id"), rs.str("user_first_name"), rs.str("user_last_name"), rs.str("user_email"),
      rs.instantOpt("user_email_verified_at"), rs.strOpt("user_phone"), rs.uuid("user_keycloak_id"),
      UserStatus.valueOf(rs.str("user_status")), rs.instant("user_created_at"), rs.instant("user_updated_at"),
      rs.instantOpt("user_verified_at")),
    insert = c => List(
      "user_first_name" -> Frag.text(c.firstName), "user_last_name" -> Frag.text(c.lastName),
      "user_email" -> Frag.text(c.email), "user_keycloak_id" -> Frag.uuid(c.keycloakId)) ++
      c.emailVerifiedAt.col("user_email_verified_at")(Frag.ts) ++ c.phone.col("user_phone")(Frag.text) ++
      c.status.col("user_status")(en) ++ c.verifiedAt.col("user_verified_at")(Frag.ts),
    patch = p =>
      p.firstName.col("user_first_name")(Frag.text) ++ p.lastName.col("user_last_name")(Frag.text) ++
      p.email.col("user_email")(Frag.text) ++ p.emailVerifiedAt.col("user_email_verified_at")(Frag.ts) ++
      p.phone.col("user_phone")(Frag.text) ++ p.keycloakId.col("user_keycloak_id")(Frag.uuid) ++
      p.status.col("user_status")(en) ++ p.verifiedAt.col("user_verified_at")(Frag.ts),
    filter = f => f.status.cond("user_status = ?")(en) ++ f.email.cond("user_email = ?")(Frag.text),
    orderBy = "user_created_at desc, user_id",
    updatedAtCol = "user_updated_at")

object AgencyTable:
  def apply(schema: String): TableDef[UUID, Agency, AgencyCreate, AgencyPatch, AgencyFilter] = TableDef(
    table = s"$schema.agencies",
    selectCols = List("agency_id", "agency_name", "agency_document_number", "agency_document_type", "agency_creci_number",
      "agency_logo_url", "agency_contact_email", "agency_status", "agency_created_at", "agency_updated_at", "agency_verified_at"),
    idWhere = id => List(Frag("agency_id = ?", Frag.uuid(id).binds)),
    read = rs => Agency(rs.uuid("agency_id"), rs.str("agency_name"), rs.strOpt("agency_document_number"),
      DocumentType.valueOf(rs.str("agency_document_type")), rs.strOpt("agency_creci_number"), rs.strOpt("agency_logo_url"),
      rs.strOpt("agency_contact_email"), AgencyStatus.valueOf(rs.str("agency_status")), rs.instant("agency_created_at"),
      rs.instant("agency_updated_at"), rs.instantOpt("agency_verified_at")),
    insert = c => List("agency_name" -> Frag.text(c.name)) ++
      c.documentNumber.col("agency_document_number")(Frag.text) ++ c.documentType.col("agency_document_type")(en) ++
      c.creciNumber.col("agency_creci_number")(Frag.text) ++ c.logoUrl.col("agency_logo_url")(Frag.text) ++
      c.contactEmail.col("agency_contact_email")(Frag.text) ++ c.status.col("agency_status")(en) ++
      c.verifiedAt.col("agency_verified_at")(Frag.ts),
    patch = p =>
      p.name.col("agency_name")(Frag.text) ++ p.documentNumber.col("agency_document_number")(Frag.text) ++
      p.documentType.col("agency_document_type")(en) ++ p.creciNumber.col("agency_creci_number")(Frag.text) ++
      p.logoUrl.col("agency_logo_url")(Frag.text) ++ p.contactEmail.col("agency_contact_email")(Frag.text) ++
      p.status.col("agency_status")(en) ++ p.verifiedAt.col("agency_verified_at")(Frag.ts),
    filter = f => f.status.cond("agency_status = ?")(en) ++ f.documentNumber.cond("agency_document_number = ?")(Frag.text) ++
      f.creciNumber.cond("agency_creci_number = ?")(Frag.text),
    orderBy = "agency_created_at desc, agency_id",
    updatedAtCol = "agency_updated_at")

object AgentTable:
  def apply(schema: String): TableDef[AgentId, Agent, AgentCreate, AgentPatch, AgentFilter] = TableDef(
    table = s"$schema.agents",
    selectCols = List("agent_agency_id", "agent_user_id", "agent_role", "agent_license_number", "agent_bio", "agent_rating",
      "agent_created_at", "agent_updated_at", "agent_verified_at"),
    idWhere = id => List(Frag("agent_agency_id = ?", Frag.uuid(id.agencyId).binds), Frag("agent_user_id = ?", Frag.uuid(id.userId).binds)),
    read = rs => Agent(rs.uuid("agent_agency_id"), rs.uuid("agent_user_id"), AgentRole.valueOf(rs.str("agent_role")),
      rs.str("agent_license_number"), rs.strOpt("agent_bio"), rs.dec("agent_rating"), rs.instant("agent_created_at"),
      rs.instant("agent_updated_at"), rs.instantOpt("agent_verified_at")),
    insert = c => List(
      "agent_agency_id" -> Frag.uuid(c.agencyId), "agent_user_id" -> Frag.uuid(c.userId),
      "agent_role" -> en(c.role), "agent_license_number" -> Frag.text(c.licenseNumber)) ++
      c.bio.col("agent_bio")(Frag.text) ++ c.verifiedAt.col("agent_verified_at")(Frag.ts),
    patch = p => p.role.col("agent_role")(en) ++ p.licenseNumber.col("agent_license_number")(Frag.text) ++
      p.bio.col("agent_bio")(Frag.text) ++ p.verifiedAt.col("agent_verified_at")(Frag.ts),
    filter = f => List(Frag("agent_agency_id = ?", Frag.uuid(f.agencyId).binds)) ++ f.role.cond("agent_role = ?")(en),
    orderBy = "agent_created_at desc, agent_user_id",
    updatedAtCol = "agent_updated_at")
