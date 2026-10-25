package com.atacamasoft.rsp.backoffice.server.application.port.out

import com.atacamasoft.rsp.backoffice.server.domain.{Agency, AgencyCreate, AgencyFilter, AgencyPatch, Agent, AgentCreate, AgentFilter, AgentId, AgentPatch, DomainError, Lead, LeadCreate, LeadFilter, LeadPatch, Listing, ListingCreate, ListingFilter, ListingMedia, ListingMediaCreate, ListingMediaFilter, ListingMediaPatch, ListingPatch, MediaKey, Page, PageRequest, Payment, PaymentCreate, PaymentFilter, PaymentPatch, Plan, PlanCreate, PlanFilter, PlanPatch, Property, PropertyCreate, PropertyFilter, PropertyPatch, Subscription, SubscriptionCreate, SubscriptionFilter, SubscriptionPatch, User, UserCreate, UserFilter, UserPatch}

import java.util.UUID
import zio.IO

/** Driven port shape shared by every CRUD aggregate. */
trait CrudRepository[Id, E, C, P, F]:
  def list(filter: F, page: PageRequest): IO[DomainError, Page[E]]
  def get(id: Id): IO[DomainError, E]
  def create(cmd: C): IO[DomainError, E]
  def update(id: Id, patch: P): IO[DomainError, E]
  def delete(id: Id): IO[DomainError, Unit]

trait UserRepository         extends CrudRepository[UUID, User, UserCreate, UserPatch, UserFilter]
trait AgencyRepository       extends CrudRepository[UUID, Agency, AgencyCreate, AgencyPatch, AgencyFilter]
trait AgentRepository        extends CrudRepository[AgentId, Agent, AgentCreate, AgentPatch, AgentFilter]
trait PropertyRepository     extends CrudRepository[UUID, Property, PropertyCreate, PropertyPatch, PropertyFilter]:
  /** True when another property (different id) has the same normalized-address hash. */
  def existsByAddressHash(hash: String, excluding: Option[UUID]): IO[DomainError, Boolean]
trait ListingRepository      extends CrudRepository[UUID, Listing, ListingCreate, ListingPatch, ListingFilter]
trait ListingMediaRepository extends CrudRepository[MediaKey, ListingMedia, ListingMediaCreate, ListingMediaPatch, ListingMediaFilter]
trait LeadRepository         extends CrudRepository[UUID, Lead, LeadCreate, LeadPatch, LeadFilter]
trait PlanRepository         extends CrudRepository[UUID, Plan, PlanCreate, PlanPatch, PlanFilter]
trait SubscriptionRepository extends CrudRepository[UUID, Subscription, SubscriptionCreate, SubscriptionPatch, SubscriptionFilter]
trait PaymentRepository      extends CrudRepository[UUID, Payment, PaymentCreate, PaymentPatch, PaymentFilter]

/** Readiness probe port: can the application reach its persistence layer? */
trait HealthProbe:
  def checkDatabase: IO[Throwable, Unit]

/** Verifies the bearer token of a request. Plug a Keycloak/JWT adapter here. */
trait TokenVerifier:
  def verify(token: Option[String]): IO[DomainError, Unit]
