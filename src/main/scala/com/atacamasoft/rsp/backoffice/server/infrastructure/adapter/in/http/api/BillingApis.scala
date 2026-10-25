package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.api

import com.atacamasoft.rsp.backoffice.server.application.port.in.{PaymentUseCase, PlanUseCase, SubscriptionUseCase}
import com.atacamasoft.rsp.backoffice.server.domain.{DomainError, Patch, Payment, PaymentCreate, PaymentFilter, PaymentPatch, PaymentStatus, Plan, PlanCreate, PlanFilter, PlanPatch, Subscription, SubscriptionCreate, SubscriptionFilter, SubscriptionPatch, SubscriptionStatus}
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

// =================================================================== Plans

final case class PlanDto(
    id: UUID, code: String, name: String, monthlyPrice: BigDecimal, maxActiveListings: Option[Int], featuredSlots: Int,
    isActive: Boolean, verifiedAt: Option[Instant], createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object PlanDto:
  def from(p: Plan): PlanDto = PlanDto(p.id, p.code, p.name, p.monthlyPrice, p.maxActiveListings, p.featuredSlots, p.isActive,
    p.verifiedAt, p.createdAt, p.updatedAt)

final case class PlanCreateDto(
    code: String, name: String, monthlyPrice: BigDecimal, maxActiveListings: Option[Int], featuredSlots: Option[Int],
    isActive: Option[Boolean], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class PlanUpdateDto(
    code: Patch[String] = Patch.Keep, name: Patch[String] = Patch.Keep, monthlyPrice: Patch[BigDecimal] = Patch.Keep,
    maxActiveListings: Patch[Int] = Patch.Keep, featuredSlots: Patch[Int] = Patch.Keep, isActive: Patch[Boolean] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object PlanMapper:
  private val Max = Int.MaxValue

  def create(d: PlanCreateDto): Either[DomainError, PlanCreate] =
    val c = Check()
    val code = c.text("code", 50)(d.code)
    val name = c.text("name", 100)(d.name)
    val price = c.dec("monthlyPrice", true, true)(d.monthlyPrice)
    val max = d.maxActiveListings.flatMap(c.int("maxActiveListings", 0, Max))
    val slots = d.featuredSlots.flatMap(c.int("featuredSlots", 0, Max))
    c.done(PlanCreate(code.get, name.get, price.get, max, slots, d.isActive, d.verifiedAt))

  def update(d: PlanUpdateDto): Either[DomainError, PlanPatch] =
    val c = Check()
    c.atLeastOne(d.code, d.name, d.monthlyPrice, d.maxActiveListings, d.featuredSlots, d.isActive, d.verifiedAt)
    val p = PlanPatch(
      code = c.pOpt("code", d.code)(c.text("code", 50)),
      name = c.pOpt("name", d.name)(c.text("name", 100)),
      monthlyPrice = c.pOpt("monthlyPrice", d.monthlyPrice)(c.dec("monthlyPrice", true, true)),
      maxActiveListings = c.pNul("maxActiveListings", d.maxActiveListings)(c.int("maxActiveListings", 0, Max)),
      featuredSlots = c.pOpt("featuredSlots", d.featuredSlots)(c.int("featuredSlots", 0, Max)),
      isActive = c.pOpt("isActive", d.isActive)(Some(_)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class PlanApi(uc: PlanUseCase, rt: HttpRuntime):
  private val plans = rt.base.in("plans").tag("Plans")
  private val byId = plans.in(path[UUID]("planId"))

  val listEp = plans.get
    .in(rt.pageInput).in(query[Option[String]]("code")).in(query[Option[Boolean]]("isActive"))
    .out(jsonBody[PageDto[PlanDto]]).name("listPlans").summary("List plans")
    .zServerLogic[Any] { case (ctx, page, code, isActive) =>
      rt.run(ctx)(rt.pageRequest(page).flatMap(uc.list(PlanFilter(code, isActive), _)).map(r => PageDto.of(r)(PlanDto.from)))
    }

  val createEp = plans.post
    .in(jsonBody[PlanCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[PlanDto])
    .name("createPlan").summary("Create a plan")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(PlanMapper.create(body)).flatMap(uc.create).map(p => (s"/api/v1/plans/${p.id}", PlanDto.from(p))))
    }

  val getEp = byId.get.out(jsonBody[PlanDto]).name("getPlan").summary("Get a plan by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(PlanDto.from)) }

  val updateEp = byId.patch.in(jsonBody[PlanUpdateDto]).out(jsonBody[PlanDto]).name("updatePlan").summary("Partially update a plan")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(PlanMapper.update(body)).flatMap(uc.update(id, _)).map(PlanDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deletePlan").summary("Delete a plan")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Subscriptions

final case class SubscriptionDto(
    id: UUID, agencyId: UUID, planId: UUID, status: String, billingProvider: Option[String], providerSubscriptionId: Option[String],
    currentPeriodStart: Option[Instant], currentPeriodEnd: Option[Instant], verifiedAt: Option[Instant],
    createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object SubscriptionDto:
  def from(s: Subscription): SubscriptionDto = SubscriptionDto(s.id, s.agencyId, s.planId, s.status.toString, s.billingProvider,
    s.providerSubscriptionId, s.currentPeriodStart, s.currentPeriodEnd, s.verifiedAt, s.createdAt, s.updatedAt)

final case class SubscriptionCreateDto(
    agencyId: UUID, planId: UUID, status: String, billingProvider: Option[String], providerSubscriptionId: Option[String],
    currentPeriodStart: Option[Instant], currentPeriodEnd: Option[Instant], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class SubscriptionUpdateDto(
    agencyId: Patch[UUID] = Patch.Keep, planId: Patch[UUID] = Patch.Keep, status: Patch[String] = Patch.Keep,
    billingProvider: Patch[String] = Patch.Keep, providerSubscriptionId: Patch[String] = Patch.Keep,
    currentPeriodStart: Patch[Instant] = Patch.Keep, currentPeriodEnd: Patch[Instant] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object SubscriptionMapper:
  def create(d: SubscriptionCreateDto): Either[DomainError, SubscriptionCreate] =
    val c = Check()
    val st = c.en("status", SubscriptionStatus.values)(d.status)
    val bp = d.billingProvider.flatMap(c.text("billingProvider", 40))
    c.done(SubscriptionCreate(d.agencyId, d.planId, st.get, bp, d.providerSubscriptionId, d.currentPeriodStart,
      d.currentPeriodEnd, d.verifiedAt))

  def update(d: SubscriptionUpdateDto): Either[DomainError, SubscriptionPatch] =
    val c = Check()
    c.atLeastOne(d.agencyId, d.planId, d.status, d.billingProvider, d.providerSubscriptionId, d.currentPeriodStart,
      d.currentPeriodEnd, d.verifiedAt)
    val p = SubscriptionPatch(
      agencyId = c.pOpt("agencyId", d.agencyId)(Some(_)),
      planId = c.pOpt("planId", d.planId)(Some(_)),
      status = c.pOpt("status", d.status)(c.en("status", SubscriptionStatus.values)),
      billingProvider = c.pNul("billingProvider", d.billingProvider)(c.text("billingProvider", 40)),
      providerSubscriptionId = d.providerSubscriptionId,
      currentPeriodStart = d.currentPeriodStart, currentPeriodEnd = d.currentPeriodEnd, verifiedAt = d.verifiedAt)
    c.done(p)

final class SubscriptionApi(uc: SubscriptionUseCase, rt: HttpRuntime):
  private val subs = rt.base.in("subscriptions").tag("Subscriptions")
  private val byId = subs.in(path[UUID]("subscriptionId"))

  val listEp = subs.get
    .in(rt.pageInput).in(query[Option[UUID]]("agencyId")).in(query[Option[UUID]]("planId")).in(query[Option[String]]("status"))
    .out(jsonBody[PageDto[SubscriptionDto]]).name("listSubscriptions").summary("List subscriptions")
    .zServerLogic[Any] { case (ctx, page, agencyId, planId, status) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        st <- rt.enumQuery("status", status, SubscriptionStatus.values)
        r  <- uc.list(SubscriptionFilter(agencyId, planId, st), p)
      yield PageDto.of(r)(SubscriptionDto.from))
    }

  val createEp = subs.post
    .in(jsonBody[SubscriptionCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[SubscriptionDto])
    .name("createSubscription").summary("Create a subscription")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(SubscriptionMapper.create(body)).flatMap(uc.create)
        .map(s => (s"/api/v1/subscriptions/${s.id}", SubscriptionDto.from(s))))
    }

  val getEp = byId.get.out(jsonBody[SubscriptionDto]).name("getSubscription").summary("Get a subscription by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(SubscriptionDto.from)) }

  val updateEp = byId.patch.in(jsonBody[SubscriptionUpdateDto]).out(jsonBody[SubscriptionDto])
    .name("updateSubscription").summary("Partially update a subscription")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(SubscriptionMapper.update(body)).flatMap(uc.update(id, _)).map(SubscriptionDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteSubscription").summary("Delete a subscription")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Payments

final case class PaymentDto(
    id: UUID, subscriptionId: UUID, providerPaymentId: Option[String], amount: BigDecimal, currency: String, status: String,
    paidAt: Option[Instant], verifiedAt: Option[Instant], createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object PaymentDto:
  def from(p: Payment): PaymentDto = PaymentDto(p.id, p.subscriptionId, p.providerPaymentId, p.amount, p.currency,
    p.status.toString, p.paidAt, p.verifiedAt, p.createdAt, p.updatedAt)

final case class PaymentCreateDto(
    subscriptionId: UUID, providerPaymentId: Option[String], amount: BigDecimal, currency: Option[String], status: String,
    paidAt: Option[Instant], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class PaymentUpdateDto(
    subscriptionId: Patch[UUID] = Patch.Keep, providerPaymentId: Patch[String] = Patch.Keep, amount: Patch[BigDecimal] = Patch.Keep,
    currency: Patch[String] = Patch.Keep, status: Patch[String] = Patch.Keep, paidAt: Patch[Instant] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object PaymentMapper:
  private val CurrencyMsg = "must be an ISO 4217 code (three upper-case letters)"

  def create(d: PaymentCreateDto): Either[DomainError, PaymentCreate] =
    val c = Check()
    val amount = c.dec("amount", true, true)(d.amount)
    val cur = d.currency.flatMap(c.pattern("currency", Check.Currency, CurrencyMsg))
    val st = c.en("status", PaymentStatus.values)(d.status)
    c.done(PaymentCreate(d.subscriptionId, d.providerPaymentId, amount.get, cur, st.get, d.paidAt, d.verifiedAt))

  def update(d: PaymentUpdateDto): Either[DomainError, PaymentPatch] =
    val c = Check()
    c.atLeastOne(d.subscriptionId, d.providerPaymentId, d.amount, d.currency, d.status, d.paidAt, d.verifiedAt)
    val p = PaymentPatch(
      subscriptionId = c.pOpt("subscriptionId", d.subscriptionId)(Some(_)),
      providerPaymentId = d.providerPaymentId,
      amount = c.pOpt("amount", d.amount)(c.dec("amount", true, true)),
      currency = c.pOpt("currency", d.currency)(c.pattern("currency", Check.Currency, CurrencyMsg)),
      status = c.pOpt("status", d.status)(c.en("status", PaymentStatus.values)),
      paidAt = d.paidAt, verifiedAt = d.verifiedAt)
    c.done(p)

final class PaymentApi(uc: PaymentUseCase, rt: HttpRuntime):
  private val payments = rt.base.in("payments").tag("Payments")
  private val byId = payments.in(path[UUID]("paymentId"))

  val listEp = payments.get
    .in(rt.pageInput).in(query[Option[UUID]]("subscriptionId")).in(query[Option[String]]("status"))
    .out(jsonBody[PageDto[PaymentDto]]).name("listPayments").summary("List payments")
    .zServerLogic[Any] { case (ctx, page, subscriptionId, status) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        st <- rt.enumQuery("status", status, PaymentStatus.values)
        r  <- uc.list(PaymentFilter(subscriptionId, st), p)
      yield PageDto.of(r)(PaymentDto.from))
    }

  val createEp = payments.post
    .in(jsonBody[PaymentCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[PaymentDto])
    .name("createPayment").summary("Create a payment")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(PaymentMapper.create(body)).flatMap(uc.create).map(p => (s"/api/v1/payments/${p.id}", PaymentDto.from(p))))
    }

  val getEp = byId.get.out(jsonBody[PaymentDto]).name("getPayment").summary("Get a payment by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(PaymentDto.from)) }

  val updateEp = byId.patch.in(jsonBody[PaymentUpdateDto]).out(jsonBody[PaymentDto]).name("updatePayment").summary("Partially update a payment")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(PaymentMapper.update(body)).flatMap(uc.update(id, _)).map(PaymentDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deletePayment").summary("Delete a payment")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)
