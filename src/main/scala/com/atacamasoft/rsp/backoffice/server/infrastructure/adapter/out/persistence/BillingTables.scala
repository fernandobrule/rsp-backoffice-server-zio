package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence

import com.atacamasoft.rsp.backoffice.server.domain.{Payment, PaymentCreate, PaymentFilter, PaymentPatch, PaymentStatus, Plan, PlanCreate, PlanFilter, PlanPatch, Subscription, SubscriptionCreate, SubscriptionFilter, SubscriptionPatch, SubscriptionStatus}

import java.util.UUID

object PlanTable:
  def apply(schema: String): TableDef[UUID, Plan, PlanCreate, PlanPatch, PlanFilter] = TableDef(
    table = s"$schema.plans",
    selectCols = List("plan_id", "plan_code", "plan_name", "plan_monthly_price", "plan_max_active_listings",
      "plan_featured_slots", "plan_is_active", "plan_created_at", "plan_updated_at", "plan_verified_at"),
    idWhere = id => List(Frag("plan_id = ?", Frag.uuid(id).binds)),
    read = rs => Plan(rs.uuid("plan_id"), rs.str("plan_code"), rs.str("plan_name"), rs.dec("plan_monthly_price"),
      rs.intOpt("plan_max_active_listings"), rs.getInt("plan_featured_slots"), rs.getBoolean("plan_is_active"),
      rs.instant("plan_created_at"), rs.instant("plan_updated_at"), rs.instantOpt("plan_verified_at")),
    insert = c => List("plan_code" -> Frag.text(c.code), "plan_name" -> Frag.text(c.name),
      "plan_monthly_price" -> Frag.dec(c.monthlyPrice)) ++
      c.maxActiveListings.col("plan_max_active_listings")(Frag.int) ++ c.featuredSlots.col("plan_featured_slots")(Frag.int) ++
      c.isActive.col("plan_is_active")(Frag.bool) ++ c.verifiedAt.col("plan_verified_at")(Frag.ts),
    patch = p => p.code.col("plan_code")(Frag.text) ++ p.name.col("plan_name")(Frag.text) ++
      p.monthlyPrice.col("plan_monthly_price")(Frag.dec) ++ p.maxActiveListings.col("plan_max_active_listings")(Frag.int) ++
      p.featuredSlots.col("plan_featured_slots")(Frag.int) ++ p.isActive.col("plan_is_active")(Frag.bool) ++
      p.verifiedAt.col("plan_verified_at")(Frag.ts),
    filter = f => f.code.cond("plan_code = ?")(Frag.text) ++ f.isActive.cond("plan_is_active = ?")(Frag.bool),
    orderBy = "plan_monthly_price, plan_id",
    updatedAtCol = "plan_updated_at")

object SubscriptionTable:
  def apply(schema: String): TableDef[UUID, Subscription, SubscriptionCreate, SubscriptionPatch, SubscriptionFilter] = TableDef(
    table = s"$schema.subscriptions",
    selectCols = List("subscription_id", "subscription_agency_id", "subscription_plan_id", "subscription_status",
      "subscription_billing_provider", "subscription_provider_subscription_id", "subscription_current_period_start",
      "subscription_current_period_end", "subscription_created_at", "subscription_updated_at", "subscription_verified_at"),
    idWhere = id => List(Frag("subscription_id = ?", Frag.uuid(id).binds)),
    read = rs => Subscription(rs.uuid("subscription_id"), rs.uuid("subscription_agency_id"), rs.uuid("subscription_plan_id"),
      SubscriptionStatus.valueOf(rs.str("subscription_status")), rs.strOpt("subscription_billing_provider"),
      rs.strOpt("subscription_provider_subscription_id"), rs.instantOpt("subscription_current_period_start"),
      rs.instantOpt("subscription_current_period_end"), rs.instant("subscription_created_at"),
      rs.instant("subscription_updated_at"), rs.instantOpt("subscription_verified_at")),
    insert = c => List("subscription_agency_id" -> Frag.uuid(c.agencyId), "subscription_plan_id" -> Frag.uuid(c.planId),
      "subscription_status" -> en(c.status)) ++
      c.billingProvider.col("subscription_billing_provider")(Frag.text) ++
      c.providerSubscriptionId.col("subscription_provider_subscription_id")(Frag.text) ++
      c.currentPeriodStart.col("subscription_current_period_start")(Frag.ts) ++
      c.currentPeriodEnd.col("subscription_current_period_end")(Frag.ts) ++ c.verifiedAt.col("subscription_verified_at")(Frag.ts),
    patch = p => p.agencyId.col("subscription_agency_id")(Frag.uuid) ++ p.planId.col("subscription_plan_id")(Frag.uuid) ++
      p.status.col("subscription_status")(en) ++ p.billingProvider.col("subscription_billing_provider")(Frag.text) ++
      p.providerSubscriptionId.col("subscription_provider_subscription_id")(Frag.text) ++
      p.currentPeriodStart.col("subscription_current_period_start")(Frag.ts) ++
      p.currentPeriodEnd.col("subscription_current_period_end")(Frag.ts) ++ p.verifiedAt.col("subscription_verified_at")(Frag.ts),
    filter = f => f.agencyId.cond("subscription_agency_id = ?")(Frag.uuid) ++ f.planId.cond("subscription_plan_id = ?")(Frag.uuid) ++
      f.status.cond("subscription_status = ?")(en),
    orderBy = "subscription_created_at desc, subscription_id",
    updatedAtCol = "subscription_updated_at")

object PaymentTable:
  def apply(schema: String): TableDef[UUID, Payment, PaymentCreate, PaymentPatch, PaymentFilter] = TableDef(
    table = s"$schema.payments",
    selectCols = List("payment_id", "payment_subscription_id", "payment_provider_payment_id", "payment_amount",
      "payment_currency", "payment_status", "payment_paid_at", "payment_created_at", "payment_updated_at", "payment_verified_at"),
    idWhere = id => List(Frag("payment_id = ?", Frag.uuid(id).binds)),
    read = rs => Payment(rs.uuid("payment_id"), rs.uuid("payment_subscription_id"), rs.strOpt("payment_provider_payment_id"),
      rs.dec("payment_amount"), rs.str("payment_currency").trim, PaymentStatus.valueOf(rs.str("payment_status")),
      rs.instantOpt("payment_paid_at"), rs.instant("payment_created_at"), rs.instant("payment_updated_at"),
      rs.instantOpt("payment_verified_at")),
    insert = c => List("payment_subscription_id" -> Frag.uuid(c.subscriptionId), "payment_amount" -> Frag.dec(c.amount),
      "payment_status" -> en(c.status)) ++
      c.providerPaymentId.col("payment_provider_payment_id")(Frag.text) ++ c.currency.col("payment_currency")(Frag.text) ++
      c.paidAt.col("payment_paid_at")(Frag.ts) ++ c.verifiedAt.col("payment_verified_at")(Frag.ts),
    patch = p => p.subscriptionId.col("payment_subscription_id")(Frag.uuid) ++
      p.providerPaymentId.col("payment_provider_payment_id")(Frag.text) ++ p.amount.col("payment_amount")(Frag.dec) ++
      p.currency.col("payment_currency")(Frag.text) ++ p.status.col("payment_status")(en) ++
      p.paidAt.col("payment_paid_at")(Frag.ts) ++ p.verifiedAt.col("payment_verified_at")(Frag.ts),
    filter = f => f.subscriptionId.cond("payment_subscription_id = ?")(Frag.uuid) ++ f.status.cond("payment_status = ?")(en),
    orderBy = "payment_created_at desc, payment_id",
    updatedAtCol = "payment_updated_at")
