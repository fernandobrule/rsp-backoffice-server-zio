package com.atacamasoft.rsp.backoffice.server.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Instant
import java.util.UUID

enum PropertyType:
  case APARTMENT, HOUSE, LAND, COMMERCIAL, FARM, OTHER

final case class GeoPoint(latitude: Double, longitude: Double)

final case class Property(
    id: UUID, propertyType: PropertyType, country: String, stateCode: String, city: String,
    postalCode: Option[String], neighborhood: Option[String], street: Option[String], number: Option[String],
    complement: Option[String], location: Option[GeoPoint], addressHash: Option[String],
    bedrooms: Option[Int], bathrooms: Option[Int], parkingSpots: Option[Int],
    usableAreaM2: Option[BigDecimal], totalAreaM2: Option[BigDecimal], yearBuilt: Option[Int],
    /** Raw JSON object text (jsonb column). */
    attributes: String,
    createdAt: Instant, updatedAt: Instant, verifiedAt: Option[Instant])

final case class PropertyCreate(
    propertyType: PropertyType, country: Option[String], stateCode: String, city: String,
    postalCode: Option[String], neighborhood: Option[String], street: Option[String], number: Option[String],
    complement: Option[String], location: Option[GeoPoint], bedrooms: Option[Int], bathrooms: Option[Int],
    parkingSpots: Option[Int], usableAreaM2: Option[BigDecimal], totalAreaM2: Option[BigDecimal],
    yearBuilt: Option[Int], attributes: Option[String], verifiedAt: Option[Instant],
    /** Computed by the service. */
    addressHash: Option[String] = None)

final case class PropertyPatch(
    propertyType: Option[PropertyType] = None, country: Option[String] = None, stateCode: Option[String] = None,
    city: Option[String] = None, postalCode: Patch[String] = Patch.Keep, neighborhood: Patch[String] = Patch.Keep,
    street: Patch[String] = Patch.Keep, number: Patch[String] = Patch.Keep, complement: Patch[String] = Patch.Keep,
    location: Patch[GeoPoint] = Patch.Keep, bedrooms: Patch[Int] = Patch.Keep, bathrooms: Patch[Int] = Patch.Keep,
    parkingSpots: Patch[Int] = Patch.Keep, usableAreaM2: Patch[BigDecimal] = Patch.Keep,
    totalAreaM2: Patch[BigDecimal] = Patch.Keep, yearBuilt: Patch[Int] = Patch.Keep,
    attributes: Option[String] = None, verifiedAt: Patch[Instant] = Patch.Keep,
    /** Computed by the service. */
    addressHash: Option[String] = None)

final case class PropertyFilter(
    propertyType: Option[PropertyType], stateCode: Option[String], city: Option[String],
    neighborhood: Option[String], minBedrooms: Option[Int])

object AddressHash:
  private def norm(s: Option[String]): String =
    val n = Normalizer.normalize(s.getOrElse(""), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
    n.toLowerCase.trim.replaceAll("\\s+", " ")

  /** SHA-256 (hex, 64 chars) of the normalized address, used for duplicate detection. */
  def of(country: String, state: String, city: String, postal: Option[String], neighborhood: Option[String],
         street: Option[String], number: Option[String], complement: Option[String]): String =
    val raw = List(Some(country), Some(state), Some(city), postal.map(_.filter(_.isLetterOrDigit)), neighborhood, street, number, complement)
      .map(norm).mkString("|")
    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)).map("%02x".format(_)).mkString
