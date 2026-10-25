package com.atacamasoft.rsp.backoffice.server.application.port.in

import com.atacamasoft.rsp.backoffice.server.domain.{Agency, AgencyCreate, AgencyFilter, AgencyPatch, Agent, AgentCreate, AgentFilter, AgentId, AgentPatch, DomainError, Lead, LeadCreate, LeadFilter, LeadPatch, Listing, ListingCreate, ListingFilter, ListingMedia, ListingMediaCreate, ListingMediaFilter, ListingMediaPatch, ListingPatch, MediaKey, Page, PageRequest, Payment, PaymentCreate, PaymentFilter, PaymentPatch, Plan, PlanCreate, PlanFilter, PlanPatch, Property, PropertyCreate, PropertyFilter, PropertyPatch, Subscription, SubscriptionCreate, SubscriptionFilter, SubscriptionPatch, User, UserCreate, UserFilter, UserPatch}

import java.util.UUID
import zio.IO

/** Driving port shape shared by every CRUD aggregate. */
trait CrudUseCase[Id, E, C, P, F]:
  def list(filter: F, page: PageRequest): IO[DomainError, Page[E]]
  def get(id: Id): IO[DomainError, E]
  def create(cmd: C): IO[DomainError, E]
  def update(id: Id, patch: P): IO[DomainError, E]
  def delete(id: Id): IO[DomainError, Unit]

trait UserUseCase         extends CrudUseCase[UUID, User, UserCreate, UserPatch, UserFilter]
trait AgencyUseCase       extends CrudUseCase[UUID, Agency, AgencyCreate, AgencyPatch, AgencyFilter]
trait AgentUseCase        extends CrudUseCase[AgentId, Agent, AgentCreate, AgentPatch, AgentFilter]
trait PropertyUseCase     extends CrudUseCase[UUID, Property, PropertyCreate, PropertyPatch, PropertyFilter]
trait ListingUseCase      extends CrudUseCase[UUID, Listing, ListingCreate, ListingPatch, ListingFilter]
trait ListingMediaUseCase extends CrudUseCase[MediaKey, ListingMedia, ListingMediaCreate, ListingMediaPatch, ListingMediaFilter]
trait LeadUseCase         extends CrudUseCase[UUID, Lead, LeadCreate, LeadPatch, LeadFilter]
trait PlanUseCase         extends CrudUseCase[UUID, Plan, PlanCreate, PlanPatch, PlanFilter]
trait SubscriptionUseCase extends CrudUseCase[UUID, Subscription, SubscriptionCreate, SubscriptionPatch, SubscriptionFilter]
trait PaymentUseCase      extends CrudUseCase[UUID, Payment, PaymentCreate, PaymentPatch, PaymentFilter]
