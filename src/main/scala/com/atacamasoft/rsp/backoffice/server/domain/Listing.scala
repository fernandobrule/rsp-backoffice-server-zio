package com.atacamasoft.rsp.backoffice.server.domain

import java.time.Instant
import java.util.UUID

enum TransactionType:
  case SALE, RENT

enum ListingStatus:
  case DRAFT, PENDING, PUBLISHED, REJECTED, PAUSED, SOLD, RENTED, EXPIRED, REMOVED

final case class Listing(
    id: UUID, propertyId: UUID, agencyId: UUID, transactionType: TransactionType, title: String,
    description: Option[String], price: BigDecimal, currency: String, condoFee: Option[BigDecimal],
    iptuAnnual: Option[BigDecimal], propertyTax: Option[BigDecimal], publishedAt: Option[Instant],
    expiresAt: Option[Instant], status: ListingStatus, createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class ListingCreate(
    propertyId: UUID, agencyId: UUID, transactionType: TransactionType, title: String, description: Option[String],
    price: BigDecimal, currency: Option[String], condoFee: Option[BigDecimal], iptuAnnual: Option[BigDecimal],
    propertyTax: Option[BigDecimal], publishedAt: Option[Instant], expiresAt: Option[Instant],
    status: Option[ListingStatus], verifiedAt: Option[Instant])

final case class ListingPatch(
    propertyId: Option[UUID] = None, agencyId: Option[UUID] = None, transactionType: Option[TransactionType] = None,
    title: Option[String] = None, description: Patch[String] = Patch.Keep, price: Option[BigDecimal] = None,
    currency: Option[String] = None, condoFee: Patch[BigDecimal] = Patch.Keep, iptuAnnual: Patch[BigDecimal] = Patch.Keep,
    propertyTax: Patch[BigDecimal] = Patch.Keep, publishedAt: Patch[Instant] = Patch.Keep,
    expiresAt: Patch[Instant] = Patch.Keep, status: Option[ListingStatus] = None, verifiedAt: Patch[Instant] = Patch.Keep)

final case class ListingFilter(
    propertyId: Option[UUID], agencyId: Option[UUID], transactionType: Option[TransactionType],
    status: Option[ListingStatus], minPrice: Option[BigDecimal], maxPrice: Option[BigDecimal])

// ---- Listing media ----
final case class MediaKey(listingId: UUID, mediaId: UUID)

final case class ListingMedia(
    id: UUID, listingId: UUID, storageKey: String, altText: Option[String], displayOrder: Int, isCover: Boolean,
    createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class ListingMediaCreate(
    listingId: UUID, storageKey: String, altText: Option[String], displayOrder: Option[Int], isCover: Option[Boolean],
    verifiedAt: Option[Instant])

final case class ListingMediaPatch(
    storageKey: Option[String] = None, altText: Patch[String] = Patch.Keep, displayOrder: Option[Int] = None,
    isCover: Option[Boolean] = None, verifiedAt: Patch[Instant] = Patch.Keep)

final case class ListingMediaFilter(listingId: UUID)

// ---- Leads ----
enum LeadSource:
  case WEBSITE, WHATSAPP, PHONE, OTHER

enum LeadStatus:
  case NEW, CONTACTED, QUALIFIED, CONVERTED, CLOSED, SPAM

final case class Lead(
    id: UUID, listingId: UUID, agencyId: UUID, contactName: String, contactEmail: Option[String],
    contactPhone: Option[String], message: Option[String], source: LeadSource, status: LeadStatus,
    createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class LeadCreate(
    listingId: UUID, agencyId: UUID, contactName: String, contactEmail: Option[String], contactPhone: Option[String],
    message: Option[String], source: Option[LeadSource], status: Option[LeadStatus], verifiedAt: Option[Instant])

final case class LeadPatch(
    listingId: Option[UUID] = None, agencyId: Option[UUID] = None, contactName: Option[String] = None,
    contactEmail: Patch[String] = Patch.Keep, contactPhone: Patch[String] = Patch.Keep, message: Patch[String] = Patch.Keep,
    source: Option[LeadSource] = None, status: Option[LeadStatus] = None, verifiedAt: Patch[Instant] = Patch.Keep)

final case class LeadFilter(listingId: Option[UUID], agencyId: Option[UUID], status: Option[LeadStatus], source: Option[LeadSource])
