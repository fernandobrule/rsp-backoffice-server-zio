package com.atacamasoft.rsp.backoffice.server.domain

import java.time.Instant
import java.util.UUID

// ---- Plans ----
final case class Plan(
    id: UUID, code: String, name: String, monthlyPrice: BigDecimal, maxActiveListings: Option[Int], featuredSlots: Int,
    isActive: Boolean, createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class PlanCreate(
    code: String, name: String, monthlyPrice: BigDecimal, maxActiveListings: Option[Int], featuredSlots: Option[Int],
    isActive: Option[Boolean], verifiedAt: Option[Instant])

final case class PlanPatch(
    code: Option[String] = None, name: Option[String] = None, monthlyPrice: Option[BigDecimal] = None,
    maxActiveListings: Patch[Int] = Patch.Keep, featuredSlots: Option[Int] = None, isActive: Option[Boolean] = None,
    verifiedAt: Patch[Instant] = Patch.Keep)

final case class PlanFilter(code: Option[String], isActive: Option[Boolean])

// ---- Subscriptions ----
enum SubscriptionStatus:
  case TRIALING, ACTIVE, PAST_DUE, CANCELED, EXPIRED

final case class Subscription(
    id: UUID, agencyId: UUID, planId: UUID, status: SubscriptionStatus, billingProvider: Option[String],
    providerSubscriptionId: Option[String], currentPeriodStart: Option[Instant], currentPeriodEnd: Option[Instant],
    createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class SubscriptionCreate(
    agencyId: UUID, planId: UUID, status: SubscriptionStatus, billingProvider: Option[String],
    providerSubscriptionId: Option[String], currentPeriodStart: Option[Instant], currentPeriodEnd: Option[Instant],
    verifiedAt: Option[Instant])

final case class SubscriptionPatch(
    agencyId: Option[UUID] = None, planId: Option[UUID] = None, status: Option[SubscriptionStatus] = None,
    billingProvider: Patch[String] = Patch.Keep, providerSubscriptionId: Patch[String] = Patch.Keep,
    currentPeriodStart: Patch[Instant] = Patch.Keep, currentPeriodEnd: Patch[Instant] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep)

final case class SubscriptionFilter(agencyId: Option[UUID], planId: Option[UUID], status: Option[SubscriptionStatus])

// ---- Payments ----
enum PaymentStatus:
  case PENDING, PAID, FAILED, REFUNDED, CANCELED

final case class Payment(
    id: UUID, subscriptionId: UUID, providerPaymentId: Option[String], amount: BigDecimal, currency: String,
    status: PaymentStatus, paidAt: Option[Instant], createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class PaymentCreate(
    subscriptionId: UUID, providerPaymentId: Option[String], amount: BigDecimal, currency: Option[String],
    status: PaymentStatus, paidAt: Option[Instant], verifiedAt: Option[Instant])

final case class PaymentPatch(
    subscriptionId: Option[UUID] = None, providerPaymentId: Patch[String] = Patch.Keep, amount: Option[BigDecimal] = None,
    currency: Option[String] = None, status: Option[PaymentStatus] = None, paidAt: Patch[Instant] = Patch.Keep,
    verifiedAt: Patch[Instant] = Patch.Keep)

final case class PaymentFilter(subscriptionId: Option[UUID], status: Option[PaymentStatus])
