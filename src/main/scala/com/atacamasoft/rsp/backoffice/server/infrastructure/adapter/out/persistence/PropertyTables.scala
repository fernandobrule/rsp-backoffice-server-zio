package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence

import com.atacamasoft.rsp.backoffice.server.domain.{GeoPoint, Lead, LeadCreate, LeadFilter, LeadPatch, LeadSource, LeadStatus, Listing, ListingCreate, ListingFilter, ListingMedia, ListingMediaCreate, ListingMediaFilter, ListingMediaPatch, ListingPatch, ListingStatus, MediaKey, Property, PropertyCreate, PropertyFilter, PropertyPatch, PropertyType, TransactionType}

import java.util.UUID

object PropertyTable:
  def apply(schema: String): TableDef[UUID, Property, PropertyCreate, PropertyPatch, PropertyFilter] = TableDef(
    table = s"$schema.properties",
    selectCols = List("property_id", "property_type", "property_country", "property_state_code", "property_city",
      "property_postal_code", "property_neighborhood", "property_street", "property_number", "property_complement",
      "ST_Y(property_location::geometry) as property_latitude", "ST_X(property_location::geometry) as property_longitude",
      "property_address_hash", "property_bedrooms", "property_bathrooms", "property_parking_spots",
      "property_usable_area_m2", "property_total_area_m2", "property_year_built", "property_attributes",
      "property_created_at", "property_updated_at", "property_verified_at"),
    idWhere = id => List(Frag("property_id = ?", Frag.uuid(id).binds)),
    read = rs => Property(rs.uuid("property_id"), PropertyType.valueOf(rs.str("property_type")), rs.str("property_country"),
      rs.str("property_state_code"), rs.str("property_city"), rs.strOpt("property_postal_code"),
      rs.strOpt("property_neighborhood"), rs.strOpt("property_street"), rs.strOpt("property_number"),
      rs.strOpt("property_complement"),
      for lat <- rs.doubleOpt("property_latitude"); lon <- rs.doubleOpt("property_longitude") yield GeoPoint(lat, lon),
      rs.strOpt("property_address_hash").map(_.trim), rs.intOpt("property_bedrooms"), rs.intOpt("property_bathrooms"),
      rs.intOpt("property_parking_spots"), rs.decOpt("property_usable_area_m2"), rs.decOpt("property_total_area_m2"),
      rs.intOpt("property_year_built"), rs.str("property_attributes"), rs.instant("property_created_at"),
      rs.instant("property_updated_at"), rs.instantOpt("property_verified_at")),
    insert = c => List("property_type" -> en(c.propertyType), "property_state_code" -> Frag.text(c.stateCode),
      "property_city" -> Frag.text(c.city)) ++
      c.country.col("property_country")(Frag.text) ++ c.postalCode.col("property_postal_code")(Frag.text) ++
      c.neighborhood.col("property_neighborhood")(Frag.text) ++ c.street.col("property_street")(Frag.text) ++
      c.number.col("property_number")(Frag.text) ++ c.complement.col("property_complement")(Frag.text) ++
      c.location.col("property_location")(Frag.geography) ++ c.addressHash.col("property_address_hash")(Frag.text) ++
      c.bedrooms.col("property_bedrooms")(Frag.int) ++ c.bathrooms.col("property_bathrooms")(Frag.int) ++
      c.parkingSpots.col("property_parking_spots")(Frag.int) ++ c.usableAreaM2.col("property_usable_area_m2")(Frag.dec) ++
      c.totalAreaM2.col("property_total_area_m2")(Frag.dec) ++ c.yearBuilt.col("property_year_built")(Frag.int) ++
      c.attributes.col("property_attributes")(Frag.jsonb) ++ c.verifiedAt.col("property_verified_at")(Frag.ts),
    patch = p =>
      p.propertyType.col("property_type")(en) ++ p.country.col("property_country")(Frag.text) ++
      p.stateCode.col("property_state_code")(Frag.text) ++ p.city.col("property_city")(Frag.text) ++
      p.postalCode.col("property_postal_code")(Frag.text) ++ p.neighborhood.col("property_neighborhood")(Frag.text) ++
      p.street.col("property_street")(Frag.text) ++ p.number.col("property_number")(Frag.text) ++
      p.complement.col("property_complement")(Frag.text) ++ p.location.col("property_location")(Frag.geography) ++
      p.addressHash.col("property_address_hash")(Frag.text) ++ p.bedrooms.col("property_bedrooms")(Frag.int) ++
      p.bathrooms.col("property_bathrooms")(Frag.int) ++ p.parkingSpots.col("property_parking_spots")(Frag.int) ++
      p.usableAreaM2.col("property_usable_area_m2")(Frag.dec) ++ p.totalAreaM2.col("property_total_area_m2")(Frag.dec) ++
      p.yearBuilt.col("property_year_built")(Frag.int) ++ p.attributes.col("property_attributes")(Frag.jsonb) ++
      p.verifiedAt.col("property_verified_at")(Frag.ts),
    filter = f => f.propertyType.cond("property_type = ?")(en) ++ f.stateCode.cond("property_state_code = ?")(Frag.text) ++
      f.city.cond("lower(property_city) = lower(?)")(Frag.text) ++
      f.neighborhood.cond("lower(property_neighborhood) = lower(?)")(Frag.text) ++
      f.minBedrooms.cond("property_bedrooms >= ?")(Frag.int),
    orderBy = "property_created_at desc, property_id",
    updatedAtCol = "property_updated_at")

object ListingTable:
  def apply(schema: String): TableDef[UUID, Listing, ListingCreate, ListingPatch, ListingFilter] = TableDef(
    table = s"$schema.listings",
    selectCols = List("listing_id", "listing_property_id", "listing_agency_id", "listing_transaction_type", "listing_title",
      "listing_description", "listing_price", "listing_currency", "listing_condo_fee", "listing_iptu_annual",
      "listing_property_tax", "listing_published_at", "listing_expires_at", "listing_status", "listing_created_at",
      "listing_updated_at", "listing_verified_at"),
    idWhere = id => List(Frag("listing_id = ?", Frag.uuid(id).binds)),
    read = rs => Listing(rs.uuid("listing_id"), rs.uuid("listing_property_id"), rs.uuid("listing_agency_id"),
      TransactionType.valueOf(rs.str("listing_transaction_type")), rs.str("listing_title"), rs.strOpt("listing_description"),
      rs.dec("listing_price"), rs.str("listing_currency").trim, rs.decOpt("listing_condo_fee"), rs.decOpt("listing_iptu_annual"),
      rs.decOpt("listing_property_tax"), rs.instantOpt("listing_published_at"), rs.instantOpt("listing_expires_at"),
      ListingStatus.valueOf(rs.str("listing_status")), rs.instant("listing_created_at"), rs.instant("listing_updated_at"),
      rs.instantOpt("listing_verified_at")),
    insert = c => List("listing_property_id" -> Frag.uuid(c.propertyId), "listing_agency_id" -> Frag.uuid(c.agencyId),
      "listing_transaction_type" -> en(c.transactionType), "listing_title" -> Frag.text(c.title),
      "listing_price" -> Frag.dec(c.price)) ++
      c.description.col("listing_description")(Frag.text) ++ c.currency.col("listing_currency")(Frag.text) ++
      c.condoFee.col("listing_condo_fee")(Frag.dec) ++ c.iptuAnnual.col("listing_iptu_annual")(Frag.dec) ++
      c.propertyTax.col("listing_property_tax")(Frag.dec) ++ c.publishedAt.col("listing_published_at")(Frag.ts) ++
      c.expiresAt.col("listing_expires_at")(Frag.ts) ++ c.status.col("listing_status")(en) ++
      c.verifiedAt.col("listing_verified_at")(Frag.ts),
    patch = p =>
      p.propertyId.col("listing_property_id")(Frag.uuid) ++ p.agencyId.col("listing_agency_id")(Frag.uuid) ++
      p.transactionType.col("listing_transaction_type")(en) ++ p.title.col("listing_title")(Frag.text) ++
      p.description.col("listing_description")(Frag.text) ++ p.price.col("listing_price")(Frag.dec) ++
      p.currency.col("listing_currency")(Frag.text) ++ p.condoFee.col("listing_condo_fee")(Frag.dec) ++
      p.iptuAnnual.col("listing_iptu_annual")(Frag.dec) ++ p.propertyTax.col("listing_property_tax")(Frag.dec) ++
      p.publishedAt.col("listing_published_at")(Frag.ts) ++ p.expiresAt.col("listing_expires_at")(Frag.ts) ++
      p.status.col("listing_status")(en) ++ p.verifiedAt.col("listing_verified_at")(Frag.ts),
    filter = f => f.propertyId.cond("listing_property_id = ?")(Frag.uuid) ++ f.agencyId.cond("listing_agency_id = ?")(Frag.uuid) ++
      f.transactionType.cond("listing_transaction_type = ?")(en) ++ f.status.cond("listing_status = ?")(en) ++
      f.minPrice.cond("listing_price >= ?")(Frag.dec) ++ f.maxPrice.cond("listing_price <= ?")(Frag.dec),
    orderBy = "listing_created_at desc, listing_id",
    updatedAtCol = "listing_updated_at")

object ListingMediaTable:
  def apply(schema: String): TableDef[MediaKey, ListingMedia, ListingMediaCreate, ListingMediaPatch, ListingMediaFilter] = TableDef(
    table = s"$schema.listing_media",
    selectCols = List("listing_media_id", "listing_media_listing_id", "listing_media_storage_key", "listing_media_alt_text",
      "listing_media_display_order", "listing_media_is_cover", "listing_media_created_at", "listing_media_updated_at",
      "listing_media_verified_at"),
    idWhere = k => List(Frag("listing_media_listing_id = ?", Frag.uuid(k.listingId).binds),
      Frag("listing_media_id = ?", Frag.uuid(k.mediaId).binds)),
    read = rs => ListingMedia(rs.uuid("listing_media_id"), rs.uuid("listing_media_listing_id"), rs.str("listing_media_storage_key"),
      rs.strOpt("listing_media_alt_text"), rs.getInt("listing_media_display_order"), rs.getBoolean("listing_media_is_cover"),
      rs.instant("listing_media_created_at"), rs.instant("listing_media_updated_at"), rs.instantOpt("listing_media_verified_at")),
    insert = c => List("listing_media_listing_id" -> Frag.uuid(c.listingId), "listing_media_storage_key" -> Frag.text(c.storageKey)) ++
      c.altText.col("listing_media_alt_text")(Frag.text) ++ c.displayOrder.col("listing_media_display_order")(Frag.int) ++
      c.isCover.col("listing_media_is_cover")(Frag.bool) ++ c.verifiedAt.col("listing_media_verified_at")(Frag.ts),
    patch = p => p.storageKey.col("listing_media_storage_key")(Frag.text) ++ p.altText.col("listing_media_alt_text")(Frag.text) ++
      p.displayOrder.col("listing_media_display_order")(Frag.int) ++ p.isCover.col("listing_media_is_cover")(Frag.bool) ++
      p.verifiedAt.col("listing_media_verified_at")(Frag.ts),
    filter = f => List(Frag("listing_media_listing_id = ?", Frag.uuid(f.listingId).binds)),
    orderBy = "listing_media_display_order, listing_media_id",
    updatedAtCol = "listing_media_updated_at")

object LeadTable:
  def apply(schema: String): TableDef[UUID, Lead, LeadCreate, LeadPatch, LeadFilter] = TableDef(
    table = s"$schema.leads",
    selectCols = List("lead_id", "lead_listing_id", "lead_agency_id", "lead_contact_name", "lead_contact_email",
      "lead_contact_phone", "lead_message", "lead_source", "lead_status", "lead_created_at", "lead_updated_at", "lead_verified_at"),
    idWhere = id => List(Frag("lead_id = ?", Frag.uuid(id).binds)),
    read = rs => Lead(rs.uuid("lead_id"), rs.uuid("lead_listing_id"), rs.uuid("lead_agency_id"), rs.str("lead_contact_name"),
      rs.strOpt("lead_contact_email"), rs.strOpt("lead_contact_phone"), rs.strOpt("lead_message"),
      LeadSource.valueOf(rs.str("lead_source")), LeadStatus.valueOf(rs.str("lead_status")), rs.instant("lead_created_at"),
      rs.instant("lead_updated_at"), rs.instantOpt("lead_verified_at")),
    insert = c => List("lead_listing_id" -> Frag.uuid(c.listingId), "lead_agency_id" -> Frag.uuid(c.agencyId),
      "lead_contact_name" -> Frag.text(c.contactName)) ++
      c.contactEmail.col("lead_contact_email")(Frag.text) ++ c.contactPhone.col("lead_contact_phone")(Frag.text) ++
      c.message.col("lead_message")(Frag.text) ++ c.source.col("lead_source")(en) ++ c.status.col("lead_status")(en) ++
      c.verifiedAt.col("lead_verified_at")(Frag.ts),
    patch = p => p.listingId.col("lead_listing_id")(Frag.uuid) ++ p.agencyId.col("lead_agency_id")(Frag.uuid) ++
      p.contactName.col("lead_contact_name")(Frag.text) ++ p.contactEmail.col("lead_contact_email")(Frag.text) ++
      p.contactPhone.col("lead_contact_phone")(Frag.text) ++ p.message.col("lead_message")(Frag.text) ++
      p.source.col("lead_source")(en) ++ p.status.col("lead_status")(en) ++ p.verifiedAt.col("lead_verified_at")(Frag.ts),
    filter = f => f.listingId.cond("lead_listing_id = ?")(Frag.uuid) ++ f.agencyId.cond("lead_agency_id = ?")(Frag.uuid) ++
      f.status.cond("lead_status = ?")(en) ++ f.source.cond("lead_source = ?")(en),
    orderBy = "lead_created_at desc, lead_id",
    updatedAtCol = "lead_updated_at")
