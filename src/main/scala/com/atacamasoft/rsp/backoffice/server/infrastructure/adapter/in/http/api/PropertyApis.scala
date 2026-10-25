package com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.api

import com.atacamasoft.rsp.backoffice.server.application.port.in.{LeadUseCase, ListingMediaUseCase, ListingUseCase, PropertyUseCase}
import com.atacamasoft.rsp.backoffice.server.domain.{DomainError, Lead, LeadCreate, LeadFilter, LeadPatch, LeadSource, LeadStatus, Listing, ListingCreate, ListingFilter, ListingMedia, ListingMediaCreate, ListingMediaFilter, ListingMediaPatch, ListingPatch, ListingStatus, MediaKey, Patch, Property, PropertyCreate, PropertyFilter, PropertyPatch, PropertyType, TransactionType}
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.HttpCodecs.given
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.{Check, GeoPointDto, HttpRuntime, JsonObjectDto, PageDto}

import java.time.Instant
import java.util.UUID
import sttp.model.StatusCode
import sttp.tapir.Schema
import sttp.tapir.json.zio.*
import sttp.tapir.ztapir.*
import zio.*
import zio.json.*
import zio.json.ast.Json

// =================================================================== Properties

final case class PropertyDto(
    id: UUID, `type`: String, country: String, stateCode: String, city: String, postalCode: Option[String],
    neighborhood: Option[String], street: Option[String], number: Option[String], complement: Option[String],
    location: Option[GeoPointDto], bedrooms: Option[Int], bathrooms: Option[Int], parkingSpots: Option[Int],
    usableAreaM2: Option[BigDecimal], totalAreaM2: Option[BigDecimal], yearBuilt: Option[Int], attributes: JsonObjectDto,
    verifiedAt: Option[Instant], addressHash: Option[String], createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object PropertyDto:
  def from(p: Property): PropertyDto =
    val attrs = p.attributes.fromJson[Json].getOrElse(Json.Obj())
    PropertyDto(p.id, p.propertyType.toString, p.country, p.stateCode, p.city, p.postalCode, p.neighborhood, p.street, p.number,
      p.complement, p.location.map(g => GeoPointDto(g.latitude, g.longitude)), p.bedrooms, p.bathrooms, p.parkingSpots,
      p.usableAreaM2, p.totalAreaM2, p.yearBuilt, JsonObjectDto(attrs), p.verifiedAt, p.addressHash, p.createdAt, p.updatedAt)

final case class PropertyCreateDto(
    `type`: String, country: Option[String], stateCode: String, city: String, postalCode: Option[String],
    neighborhood: Option[String], street: Option[String], number: Option[String], complement: Option[String],
    location: Option[GeoPointDto], bedrooms: Option[Int], bathrooms: Option[Int], parkingSpots: Option[Int],
    usableAreaM2: Option[BigDecimal], totalAreaM2: Option[BigDecimal], yearBuilt: Option[Int],
    attributes: Option[JsonObjectDto], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class PropertyUpdateDto(
    `type`: Patch[String] = Patch.Keep, country: Patch[String] = Patch.Keep, stateCode: Patch[String] = Patch.Keep,
    city: Patch[String] = Patch.Keep, postalCode: Patch[String] = Patch.Keep, neighborhood: Patch[String] = Patch.Keep,
    street: Patch[String] = Patch.Keep, number: Patch[String] = Patch.Keep, complement: Patch[String] = Patch.Keep,
    location: Patch[GeoPointDto] = Patch.Keep, bedrooms: Patch[Int] = Patch.Keep, bathrooms: Patch[Int] = Patch.Keep,
    parkingSpots: Patch[Int] = Patch.Keep, usableAreaM2: Patch[BigDecimal] = Patch.Keep, totalAreaM2: Patch[BigDecimal] = Patch.Keep,
    yearBuilt: Patch[Int] = Patch.Keep, attributes: Patch[JsonObjectDto] = Patch.Keep, verifiedAt: Patch[Instant] = Patch.Keep)
    derives JsonCodec, Schema

object PropertyMapper:
  private val SmallInt = 32767

  private def attrs(c: Check)(o: JsonObjectDto): Option[String] = o.json match
    case _: Json.Obj => Some(o.json.toJson)
    case _           => c.fail("attributes", "must be a JSON object"); None

  def create(d: PropertyCreateDto): Either[DomainError, PropertyCreate] =
    val c = Check()
    val tp = c.en("type", PropertyType.values)(d.`type`)
    val country = d.country.flatMap(c.pattern("country", Check.StateCode, "must be an ISO 3166-1 alpha-2 code"))
    val state = c.pattern("stateCode", Check.StateCode, "must be two upper-case letters")(d.stateCode)
    val city = c.text("city", 100)(d.city)
    val postal = d.postalCode.flatMap(c.text("postalCode", 20))
    val nb = d.neighborhood.flatMap(c.text("neighborhood", 120))
    val street = d.street.flatMap(c.text("street", 200))
    val number = d.number.flatMap(c.text("number", 20))
    val compl = d.complement.flatMap(c.text("complement", 100))
    val loc = d.location.flatMap(c.geo("location"))
    val bed = d.bedrooms.flatMap(c.int("bedrooms", 0, SmallInt))
    val bath = d.bathrooms.flatMap(c.int("bathrooms", 0, SmallInt))
    val park = d.parkingSpots.flatMap(c.int("parkingSpots", 0, SmallInt))
    val ua = d.usableAreaM2.flatMap(c.dec("usableAreaM2", true, false))
    val ta = d.totalAreaM2.flatMap(c.dec("totalAreaM2", true, false))
    val year = d.yearBuilt.flatMap(c.int("yearBuilt", 0, SmallInt))
    val at = d.attributes.flatMap(attrs(c))
    c.done(PropertyCreate(tp.get, country, state.get, city.get, postal, nb, street, number, compl, loc, bed, bath, park, ua, ta,
      year, at, d.verifiedAt))

  def update(d: PropertyUpdateDto): Either[DomainError, PropertyPatch] =
    val c = Check()
    c.atLeastOne(d.`type`, d.country, d.stateCode, d.city, d.postalCode, d.neighborhood, d.street, d.number, d.complement,
      d.location, d.bedrooms, d.bathrooms, d.parkingSpots, d.usableAreaM2, d.totalAreaM2, d.yearBuilt, d.attributes, d.verifiedAt)
    val p = PropertyPatch(
      propertyType = c.pOpt("type", d.`type`)(c.en("type", PropertyType.values)),
      country = c.pOpt("country", d.country)(c.pattern("country", Check.StateCode, "must be an ISO 3166-1 alpha-2 code")),
      stateCode = c.pOpt("stateCode", d.stateCode)(c.pattern("stateCode", Check.StateCode, "must be two upper-case letters")),
      city = c.pOpt("city", d.city)(c.text("city", 100)),
      postalCode = c.pNul("postalCode", d.postalCode)(c.text("postalCode", 20)),
      neighborhood = c.pNul("neighborhood", d.neighborhood)(c.text("neighborhood", 120)),
      street = c.pNul("street", d.street)(c.text("street", 200)),
      number = c.pNul("number", d.number)(c.text("number", 20)),
      complement = c.pNul("complement", d.complement)(c.text("complement", 100)),
      location = c.pNul("location", d.location)(c.geo("location")),
      bedrooms = c.pNul("bedrooms", d.bedrooms)(c.int("bedrooms", 0, SmallInt)),
      bathrooms = c.pNul("bathrooms", d.bathrooms)(c.int("bathrooms", 0, SmallInt)),
      parkingSpots = c.pNul("parkingSpots", d.parkingSpots)(c.int("parkingSpots", 0, SmallInt)),
      usableAreaM2 = c.pNul("usableAreaM2", d.usableAreaM2)(c.dec("usableAreaM2", true, false)),
      totalAreaM2 = c.pNul("totalAreaM2", d.totalAreaM2)(c.dec("totalAreaM2", true, false)),
      yearBuilt = c.pNul("yearBuilt", d.yearBuilt)(c.int("yearBuilt", 0, SmallInt)),
      attributes = c.pOpt("attributes", d.attributes)(attrs(c)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class PropertyApi(uc: PropertyUseCase, rt: HttpRuntime):
  private val props = rt.base.in("properties").tag("Properties")
  private val byId = props.in(path[UUID]("propertyId"))

  val listEp = props.get
    .in(rt.pageInput).in(query[Option[String]]("type")).in(query[Option[String]]("stateCode"))
    .in(query[Option[String]]("city")).in(query[Option[String]]("neighborhood")).in(query[Option[Int]]("minBedrooms"))
    .out(jsonBody[PageDto[PropertyDto]]).name("listProperties").summary("List properties")
    .zServerLogic[Any] { case (ctx, page, tp, state, city, nb, minBed) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        t  <- rt.enumQuery("type", tp, PropertyType.values)
        r  <- uc.list(PropertyFilter(t, state, city, nb, minBed), p)
      yield PageDto.of(r)(PropertyDto.from))
    }

  val createEp = props.post
    .in(jsonBody[PropertyCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[PropertyDto])
    .name("createProperty").summary("Create a property")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(PropertyMapper.create(body)).flatMap(uc.create).map(p => (s"/api/v1/properties/${p.id}", PropertyDto.from(p))))
    }

  val getEp = byId.get.out(jsonBody[PropertyDto]).name("getProperty").summary("Get a property by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(PropertyDto.from)) }

  val updateEp = byId.patch.in(jsonBody[PropertyUpdateDto]).out(jsonBody[PropertyDto]).name("updateProperty").summary("Partially update a property")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(PropertyMapper.update(body)).flatMap(uc.update(id, _)).map(PropertyDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteProperty").summary("Delete a property")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Listings

final case class ListingDto(
    id: UUID, propertyId: UUID, agencyId: UUID, transactionType: String, title: String, description: Option[String],
    price: BigDecimal, currency: String, condoFee: Option[BigDecimal], iptuAnnual: Option[BigDecimal],
    propertyTax: Option[BigDecimal], publishedAt: Option[Instant], expiresAt: Option[Instant], status: String,
    verifiedAt: Option[Instant], createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object ListingDto:
  def from(l: Listing): ListingDto = ListingDto(l.id, l.propertyId, l.agencyId, l.transactionType.toString, l.title, l.description,
    l.price, l.currency, l.condoFee, l.iptuAnnual, l.propertyTax, l.publishedAt, l.expiresAt, l.status.toString, l.verifiedAt,
    l.createdAt, l.updatedAt)

final case class ListingCreateDto(
    propertyId: UUID, agencyId: UUID, transactionType: String, title: String, description: Option[String], price: BigDecimal,
    currency: Option[String], condoFee: Option[BigDecimal], iptuAnnual: Option[BigDecimal], propertyTax: Option[BigDecimal],
    publishedAt: Option[Instant], expiresAt: Option[Instant], status: Option[String], verifiedAt: Option[Instant])
    derives JsonCodec, Schema

final case class ListingUpdateDto(
    propertyId: Patch[UUID] = Patch.Keep, agencyId: Patch[UUID] = Patch.Keep, transactionType: Patch[String] = Patch.Keep,
    title: Patch[String] = Patch.Keep, description: Patch[String] = Patch.Keep, price: Patch[BigDecimal] = Patch.Keep,
    currency: Patch[String] = Patch.Keep, condoFee: Patch[BigDecimal] = Patch.Keep, iptuAnnual: Patch[BigDecimal] = Patch.Keep,
    propertyTax: Patch[BigDecimal] = Patch.Keep, publishedAt: Patch[Instant] = Patch.Keep, expiresAt: Patch[Instant] = Patch.Keep,
    status: Patch[String] = Patch.Keep, verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object ListingMapper:
  private val CurrencyMsg = "must be an ISO 4217 code (three upper-case letters)"

  def create(d: ListingCreateDto): Either[DomainError, ListingCreate] =
    val c = Check()
    val tt = c.en("transactionType", TransactionType.values)(d.transactionType)
    val title = c.text("title", 200)(d.title)
    val price = c.dec("price", false, true)(d.price)
    val cur = d.currency.flatMap(c.pattern("currency", Check.Currency, CurrencyMsg))
    val condo = d.condoFee.flatMap(c.dec("condoFee", true, true))
    val iptu = d.iptuAnnual.flatMap(c.dec("iptuAnnual", true, true))
    val tax = d.propertyTax.flatMap(c.dec("propertyTax", true, true))
    val st = d.status.flatMap(c.en("status", ListingStatus.values))
    c.done(ListingCreate(d.propertyId, d.agencyId, tt.get, title.get, d.description, price.get, cur, condo, iptu, tax,
      d.publishedAt, d.expiresAt, st, d.verifiedAt))

  def update(d: ListingUpdateDto): Either[DomainError, ListingPatch] =
    val c = Check()
    c.atLeastOne(d.propertyId, d.agencyId, d.transactionType, d.title, d.description, d.price, d.currency, d.condoFee,
      d.iptuAnnual, d.propertyTax, d.publishedAt, d.expiresAt, d.status, d.verifiedAt)
    val p = ListingPatch(
      propertyId = c.pOpt("propertyId", d.propertyId)(Some(_)),
      agencyId = c.pOpt("agencyId", d.agencyId)(Some(_)),
      transactionType = c.pOpt("transactionType", d.transactionType)(c.en("transactionType", TransactionType.values)),
      title = c.pOpt("title", d.title)(c.text("title", 200)),
      description = d.description,
      price = c.pOpt("price", d.price)(c.dec("price", false, true)),
      currency = c.pOpt("currency", d.currency)(c.pattern("currency", Check.Currency, CurrencyMsg)),
      condoFee = c.pNul("condoFee", d.condoFee)(c.dec("condoFee", true, true)),
      iptuAnnual = c.pNul("iptuAnnual", d.iptuAnnual)(c.dec("iptuAnnual", true, true)),
      propertyTax = c.pNul("propertyTax", d.propertyTax)(c.dec("propertyTax", true, true)),
      publishedAt = d.publishedAt, expiresAt = d.expiresAt,
      status = c.pOpt("status", d.status)(c.en("status", ListingStatus.values)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class ListingApi(uc: ListingUseCase, rt: HttpRuntime):
  private val listings = rt.base.in("listings").tag("Listings")
  private val byId = listings.in(path[UUID]("listingId"))

  val listEp = listings.get
    .in(rt.pageInput).in(query[Option[UUID]]("propertyId")).in(query[Option[UUID]]("agencyId"))
    .in(query[Option[String]]("transactionType")).in(query[Option[String]]("status"))
    .in(query[Option[BigDecimal]]("minPrice")).in(query[Option[BigDecimal]]("maxPrice"))
    .out(jsonBody[PageDto[ListingDto]]).name("listListings").summary("List listings")
    .zServerLogic[Any] { case (ctx, page, propertyId, agencyId, tt, status, minP, maxP) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        t  <- rt.enumQuery("transactionType", tt, TransactionType.values)
        s  <- rt.enumQuery("status", status, ListingStatus.values)
        r  <- uc.list(ListingFilter(propertyId, agencyId, t, s, minP, maxP), p)
      yield PageDto.of(r)(ListingDto.from))
    }

  val createEp = listings.post
    .in(jsonBody[ListingCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[ListingDto])
    .name("createListing").summary("Create a listing")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(ListingMapper.create(body)).flatMap(uc.create).map(l => (s"/api/v1/listings/${l.id}", ListingDto.from(l))))
    }

  val getEp = byId.get.out(jsonBody[ListingDto]).name("getListing").summary("Get a listing by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(ListingDto.from)) }

  val updateEp = byId.patch.in(jsonBody[ListingUpdateDto]).out(jsonBody[ListingDto]).name("updateListing").summary("Partially update a listing")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(ListingMapper.update(body)).flatMap(uc.update(id, _)).map(ListingDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteListing").summary("Delete a listing")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Listing media

final case class ListingMediaDto(
    id: UUID, listingId: UUID, storageKey: String, altText: Option[String], displayOrder: Int, isCover: Boolean,
    verifiedAt: Option[Instant], createdAt: Instant, updatedAt: Instant) derives JsonCodec, Schema

object ListingMediaDto:
  def from(m: ListingMedia): ListingMediaDto = ListingMediaDto(m.id, m.listingId, m.storageKey, m.altText, m.displayOrder,
    m.isCover, m.verifiedAt, m.createdAt, m.updatedAt)

final case class ListingMediaCreateDto(
    storageKey: String, altText: Option[String], displayOrder: Option[Int], isCover: Option[Boolean], verifiedAt: Option[Instant])
    derives JsonCodec, Schema

final case class ListingMediaUpdateDto(
    storageKey: Patch[String] = Patch.Keep, altText: Patch[String] = Patch.Keep, displayOrder: Patch[Int] = Patch.Keep,
    isCover: Patch[Boolean] = Patch.Keep, verifiedAt: Patch[Instant] = Patch.Keep) derives JsonCodec, Schema

object ListingMediaMapper:
  def create(listingId: UUID, d: ListingMediaCreateDto): Either[DomainError, ListingMediaCreate] =
    val c = Check()
    val alt = d.altText.flatMap(c.text("altText", 250))
    c.done(ListingMediaCreate(listingId, d.storageKey, alt, d.displayOrder, d.isCover, d.verifiedAt))

  def update(d: ListingMediaUpdateDto): Either[DomainError, ListingMediaPatch] =
    val c = Check()
    c.atLeastOne(d.storageKey, d.altText, d.displayOrder, d.isCover, d.verifiedAt)
    val p = ListingMediaPatch(
      storageKey = c.pOpt("storageKey", d.storageKey)(Some(_)),
      altText = c.pNul("altText", d.altText)(c.text("altText", 250)),
      displayOrder = c.pOpt("displayOrder", d.displayOrder)(Some(_)),
      isCover = c.pOpt("isCover", d.isCover)(Some(_)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class ListingMediaApi(uc: ListingMediaUseCase, rt: HttpRuntime):
  private val media = rt.base.in("listings" / path[UUID]("listingId") / "media").tag("Listing Media")
  private val byId = media.in(path[UUID]("mediaId"))

  val listEp = media.get.in(rt.pageInput).out(jsonBody[PageDto[ListingMediaDto]]).name("listListingMedia").summary("List listing media")
    .zServerLogic[Any] { case (ctx, listingId, page) =>
      rt.run(ctx)(rt.pageRequest(page).flatMap(uc.list(ListingMediaFilter(listingId), _)).map(r => PageDto.of(r)(ListingMediaDto.from)))
    }

  val createEp = media.post
    .in(jsonBody[ListingMediaCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[ListingMediaDto])
    .name("createListingMedia").summary("Create a listing media item")
    .zServerLogic[Any] { case (ctx, listingId, body) =>
      rt.run(ctx)(ZIO.fromEither(ListingMediaMapper.create(listingId, body)).flatMap(uc.create)
        .map(m => (s"/api/v1/listings/${m.listingId}/media/${m.id}", ListingMediaDto.from(m))))
    }

  val getEp = byId.get.out(jsonBody[ListingMediaDto]).name("getListingMedia").summary("Get a listing media item by id")
    .zServerLogic[Any] { case (ctx, listingId, mediaId) => rt.run(ctx)(uc.get(MediaKey(listingId, mediaId)).map(ListingMediaDto.from)) }

  val updateEp = byId.patch.in(jsonBody[ListingMediaUpdateDto]).out(jsonBody[ListingMediaDto])
    .name("updateListingMedia").summary("Partially update a listing media item")
    .zServerLogic[Any] { case (ctx, listingId, mediaId, body) =>
      rt.run(ctx)(ZIO.fromEither(ListingMediaMapper.update(body)).flatMap(uc.update(MediaKey(listingId, mediaId), _)).map(ListingMediaDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteListingMedia").summary("Delete a listing media item")
    .zServerLogic[Any] { case (ctx, listingId, mediaId) => rt.run(ctx)(uc.delete(MediaKey(listingId, mediaId))) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)

// =================================================================== Leads

final case class LeadDto(
    id: UUID, listingId: UUID, agencyId: UUID, contactName: String, contactEmail: Option[String], contactPhone: Option[String],
    message: Option[String], source: String, status: String, verifiedAt: Option[Instant], createdAt: Instant, updatedAt: Instant)
    derives JsonCodec, Schema

object LeadDto:
  def from(l: Lead): LeadDto = LeadDto(l.id, l.listingId, l.agencyId, l.contactName, l.contactEmail, l.contactPhone, l.message,
    l.source.toString, l.status.toString, l.verifiedAt, l.createdAt, l.updatedAt)

final case class LeadCreateDto(
    listingId: UUID, agencyId: UUID, contactName: String, contactEmail: Option[String], contactPhone: Option[String],
    message: Option[String], source: Option[String], status: Option[String], verifiedAt: Option[Instant]) derives JsonCodec, Schema

final case class LeadUpdateDto(
    listingId: Patch[UUID] = Patch.Keep, agencyId: Patch[UUID] = Patch.Keep, contactName: Patch[String] = Patch.Keep,
    contactEmail: Patch[String] = Patch.Keep, contactPhone: Patch[String] = Patch.Keep, message: Patch[String] = Patch.Keep,
    source: Patch[String] = Patch.Keep, status: Patch[String] = Patch.Keep, verifiedAt: Patch[Instant] = Patch.Keep)
    derives JsonCodec, Schema

object LeadMapper:
  def create(d: LeadCreateDto): Either[DomainError, LeadCreate] =
    val c = Check()
    val name = c.text("contactName", 150)(d.contactName)
    val mail = d.contactEmail.flatMap(c.email("contactEmail", 254))
    val phone = d.contactPhone.flatMap(c.text("contactPhone", 30))
    val src = d.source.flatMap(c.en("source", LeadSource.values))
    val st = d.status.flatMap(c.en("status", LeadStatus.values))
    c.done(LeadCreate(d.listingId, d.agencyId, name.get, mail, phone, d.message, src, st, d.verifiedAt))

  def update(d: LeadUpdateDto): Either[DomainError, LeadPatch] =
    val c = Check()
    c.atLeastOne(d.listingId, d.agencyId, d.contactName, d.contactEmail, d.contactPhone, d.message, d.source, d.status, d.verifiedAt)
    val p = LeadPatch(
      listingId = c.pOpt("listingId", d.listingId)(Some(_)),
      agencyId = c.pOpt("agencyId", d.agencyId)(Some(_)),
      contactName = c.pOpt("contactName", d.contactName)(c.text("contactName", 150)),
      contactEmail = c.pNul("contactEmail", d.contactEmail)(c.email("contactEmail", 254)),
      contactPhone = c.pNul("contactPhone", d.contactPhone)(c.text("contactPhone", 30)),
      message = d.message,
      source = c.pOpt("source", d.source)(c.en("source", LeadSource.values)),
      status = c.pOpt("status", d.status)(c.en("status", LeadStatus.values)),
      verifiedAt = d.verifiedAt)
    c.done(p)

final class LeadApi(uc: LeadUseCase, rt: HttpRuntime):
  private val leads = rt.base.in("leads").tag("Leads")
  private val byId = leads.in(path[UUID]("leadId"))

  val listEp = leads.get
    .in(rt.pageInput).in(query[Option[UUID]]("listingId")).in(query[Option[UUID]]("agencyId"))
    .in(query[Option[String]]("status")).in(query[Option[String]]("source"))
    .out(jsonBody[PageDto[LeadDto]]).name("listLeads").summary("List leads")
    .zServerLogic[Any] { case (ctx, page, listingId, agencyId, status, source) =>
      rt.run(ctx)(for
        p  <- rt.pageRequest(page)
        st <- rt.enumQuery("status", status, LeadStatus.values)
        so <- rt.enumQuery("source", source, LeadSource.values)
        r  <- uc.list(LeadFilter(listingId, agencyId, st, so), p)
      yield PageDto.of(r)(LeadDto.from))
    }

  val createEp = leads.post
    .in(jsonBody[LeadCreateDto]).out(statusCode(StatusCode.Created)).out(header[String]("Location")).out(jsonBody[LeadDto])
    .name("createLead").summary("Create a lead")
    .zServerLogic[Any] { case (ctx, body) =>
      rt.run(ctx)(ZIO.fromEither(LeadMapper.create(body)).flatMap(uc.create).map(l => (s"/api/v1/leads/${l.id}", LeadDto.from(l))))
    }

  val getEp = byId.get.out(jsonBody[LeadDto]).name("getLead").summary("Get a lead by id")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.get(id).map(LeadDto.from)) }

  val updateEp = byId.patch.in(jsonBody[LeadUpdateDto]).out(jsonBody[LeadDto]).name("updateLead").summary("Partially update a lead")
    .zServerLogic[Any] { case (ctx, id, body) =>
      rt.run(ctx)(ZIO.fromEither(LeadMapper.update(body)).flatMap(uc.update(id, _)).map(LeadDto.from))
    }

  val deleteEp = byId.delete.out(statusCode(StatusCode.NoContent)).name("deleteLead").summary("Delete a lead")
    .zServerLogic[Any] { case (ctx, id) => rt.run(ctx)(uc.delete(id)) }

  val endpoints = List(listEp, createEp, getEp, updateEp, deleteEp)
