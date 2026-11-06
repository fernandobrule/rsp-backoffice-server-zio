#!/usr/bin/env bash
# curl calls for every operation of real-estate-portal-api.yaml (50 operations).
# Requires: curl, jq, uuidgen.   Usage: BASE=http://localhost:${PORT}/api/v1 ./rsp-api-curl.sh
# Section 1 creates one record of each resource (in FK order) and keeps the ids,
# sections 2-3 read/patch them, section 4 deletes everything in reverse order.
set -euo pipefail

PORT=8181
BASE="${BASE:-http://localhost:${PORT}/api/v1}"
AUTH=(-H "Authorization: Bearer ${TOKEN:-dev-token}")   # bearerAuth (not validated by the dev verifier)
JSON=(-H "Content-Type: application/json")
RID=(-H "X-Request-Id: curl-$(date +%s)")                # optional tracing headers
SFX="$RANDOM$RANDOM"                                     # keeps unique columns unique across runs

call() { curl -sS -w '\n-> HTTP %{http_code}\n' "${AUTH[@]}" "${RID[@]}" "$@"; }
post() { curl -sS "${AUTH[@]}" "${JSON[@]}" "${RID[@]}" -X POST "$@"; }

echo "### Health / preflight"
curl -sS -w '\n-> HTTP %{http_code}\n' "http://localhost:${PORT}/health"
curl -sS -w '\n-> HTTP %{http_code}\n' "http://localhost:${PORT}/health/ready"
curl -sS -i -X OPTIONS "$BASE/users" \
  -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: PATCH" \
  -H "Access-Control-Request-Headers: authorization,content-type"

echo;echo; echo "### 1. POST (create)"
USER_ID=$(post "$BASE/users" -d "{
  \"firstName\":\"Maria\",\"lastName\":\"Silva\",\"email\":\"maria.$SFX@example.com\",
  \"phone\":\"+5512999990000\",\"keycloakId\":\"$(uuidgen | tr 'A-Z' 'a-z')\",\"status\":\"ACTIVE\"}" | tee /dev/stderr | jq -r .id)

AGENCY_ID=$(post "$BASE/agencies" -d "{
  \"name\":\"Atacama Imoveis $SFX\",\"documentType\":\"CNPJ\",\"documentNumber\":\"$SFX\",
  \"creciNumber\":\"CRECI-$SFX\",\"logoUrl\":\"https://example.com/logo.png\",
  \"contactEmail\":\"contato.$SFX@example.com\",\"status\":\"PENDING\"}" | tee /dev/stderr | jq -r .id)

post "$BASE/agencies/$AGENCY_ID/agents" -d "{
  \"userId\":\"$USER_ID\",\"role\":\"OWNER\",\"licenseNumber\":\"LIC-$SFX\",\"bio\":\"Senior agent\"}" | tee /dev/stderr >/dev/null

PROPERTY_ID=$(post "$BASE/properties" -d "{
  \"type\":\"APARTMENT\",\"country\":\"BR\",\"stateCode\":\"SP\",\"city\":\"Sao Jose dos Campos\",
  \"postalCode\":\"12200-000\",\"neighborhood\":\"Jardim Apolo\",\"street\":\"Rua Exemplo\",\"number\":\"$SFX\",
  \"location\":{\"latitude\":-23.2237,\"longitude\":-45.9009},
  \"bedrooms\":3,\"bathrooms\":2,\"parkingSpots\":1,\"usableAreaM2\":85.5,\"totalAreaM2\":98.0,\"yearBuilt\":2015,
  \"attributes\":{\"elevator\":true,\"floor\":7}}" | tee /dev/stderr | jq -r .id)

LISTING_ID=$(post "$BASE/listings" -d "{
  \"propertyId\":\"$PROPERTY_ID\",\"agencyId\":\"$AGENCY_ID\",\"transactionType\":\"SALE\",
  \"title\":\"Apartamento 3 quartos\",\"description\":\"Otima localizacao\",\"price\":750000.00,\"currency\":\"BRL\",
  \"condoFee\":850.00,\"iptuAnnual\":2400.00,\"status\":\"DRAFT\"}" | tee /dev/stderr | jq -r .id)

MEDIA_ID=$(post "$BASE/listings/$LISTING_ID/media" -d "{
  \"storageKey\":\"listings/$LISTING_ID/cover.jpg\",\"altText\":\"Sala de estar\",\"displayOrder\":1,\"isCover\":true}" \
  | tee /dev/stderr | jq -r .id)

LEAD_ID=$(post "$BASE/leads" -d "{
  \"listingId\":\"$LISTING_ID\",\"agencyId\":\"$AGENCY_ID\",\"contactName\":\"Joao Souza\",
  \"contactEmail\":\"joao.$SFX@example.com\",\"contactPhone\":\"+5512988887777\",
  \"message\":\"Gostaria de visitar\",\"source\":\"WEBSITE\"}" | tee /dev/stderr | jq -r .id)

PLAN_ID=$(post "$BASE/plans" -d "{
  \"code\":\"PRO-$SFX\",\"name\":\"Pro\",\"monthlyPrice\":199.90,\"maxActiveListings\":50,\"featuredSlots\":3,\"isActive\":true}" \
  | tee /dev/stderr | jq -r .id)

SUBSCRIPTION_ID=$(post "$BASE/subscriptions" -d "{
  \"agencyId\":\"$AGENCY_ID\",\"planId\":\"$PLAN_ID\",\"status\":\"TRIALING\",\"billingProvider\":\"stripe\",
  \"providerSubscriptionId\":\"sub_$SFX\",\"currentPeriodStart\":\"2026-11-01T00:00:00Z\",
  \"currentPeriodEnd\":\"2026-12-01T00:00:00Z\"}" | tee /dev/stderr | jq -r .id)

PAYMENT_ID=$(post "$BASE/payments" -d "{
  \"subscriptionId\":\"$SUBSCRIPTION_ID\",\"providerPaymentId\":\"pay_$SFX\",\"amount\":199.90,\"currency\":\"BRL\",\"status\":\"PENDING\"}" \
  | tee /dev/stderr | jq -r .id)

echo;echo; echo "### 2. GET (list with filters + get by id)"
call "$BASE/users?limit=10&offset=0&status=ACTIVE"
call "$BASE/users/$USER_ID"
call "$BASE/agencies?status=PENDING&limit=5"
call "$BASE/agencies/$AGENCY_ID"
call "$BASE/agencies/$AGENCY_ID/agents?role=OWNER"
call "$BASE/agencies/$AGENCY_ID/agents/$USER_ID"
call "$BASE/properties?type=APARTMENT&stateCode=SP&city=Sao%20Jose%20dos%20Campos&minBedrooms=2"
call "$BASE/properties/$PROPERTY_ID"
call "$BASE/listings?agencyId=$AGENCY_ID&transactionType=SALE&status=DRAFT&minPrice=500000&maxPrice=900000"
call "$BASE/listings/$LISTING_ID"
call "$BASE/listings/$LISTING_ID/media"
call "$BASE/listings/$LISTING_ID/media/$MEDIA_ID"
call "$BASE/leads?listingId=$LISTING_ID&status=NEW&source=WEBSITE"
call "$BASE/leads/$LEAD_ID"
call "$BASE/plans?isActive=true"
call "$BASE/plans/$PLAN_ID"
call "$BASE/subscriptions?agencyId=$AGENCY_ID&status=TRIALING"
call "$BASE/subscriptions/$SUBSCRIPTION_ID"
call "$BASE/payments?subscriptionId=$SUBSCRIPTION_ID&status=PENDING"
call "$BASE/payments/$PAYMENT_ID"

echo;echo; echo "### 3. PATCH (partial update; null clears nullable fields)"
call "${JSON[@]}" -X PATCH "$BASE/users/$USER_ID" -d '{"phone":null,"status":"SUSPENDED"}'
call "${JSON[@]}" -X PATCH "$BASE/agencies/$AGENCY_ID" -d '{"status":"VERIFIED","verifiedAt":"2026-11-01T12:00:00Z"}'
call "${JSON[@]}" -X PATCH "$BASE/agencies/$AGENCY_ID/agents/$USER_ID" -d '{"role":"ADMIN","bio":null}'
call "${JSON[@]}" -X PATCH "$BASE/properties/$PROPERTY_ID" -d '{"bedrooms":4,"complement":"Apto 71","attributes":{"elevator":true,"floor":8}}'
call "${JSON[@]}" -X PATCH "$BASE/listings/$LISTING_ID" -d '{"price":720000.00,"status":"PUBLISHED","publishedAt":"2026-11-02T09:00:00Z"}'
call "${JSON[@]}" -X PATCH "$BASE/listings/$LISTING_ID/media/$MEDIA_ID" -d '{"altText":"Sala integrada","isCover":false}'
call "${JSON[@]}" -X PATCH "$BASE/leads/$LEAD_ID" -d '{"status":"CONTACTED"}'
call "${JSON[@]}" -X PATCH "$BASE/plans/$PLAN_ID" -d '{"monthlyPrice":219.90,"maxActiveListings":null}'
call "${JSON[@]}" -X PATCH "$BASE/subscriptions/$SUBSCRIPTION_ID" -d '{"status":"ACTIVE"}'
call "${JSON[@]}" -X PATCH "$BASE/payments/$PAYMENT_ID" -d '{"status":"PAID","paidAt":"2026-11-02T10:30:00Z"}'

echo;echo; echo "### 4. DELETE (reverse FK order)"
call -X DELETE "$BASE/payments/$PAYMENT_ID"
call -X DELETE "$BASE/subscriptions/$SUBSCRIPTION_ID"
call -X DELETE "$BASE/plans/$PLAN_ID"
call -X DELETE "$BASE/leads/$LEAD_ID"
call -X DELETE "$BASE/listings/$LISTING_ID/media/$MEDIA_ID"
call -X DELETE "$BASE/listings/$LISTING_ID"
call -X DELETE "$BASE/properties/$PROPERTY_ID"
call -X DELETE "$BASE/agencies/$AGENCY_ID/agents/$USER_ID"
call -X DELETE "$BASE/agencies/$AGENCY_ID"
call -X DELETE "$BASE/users/$USER_ID"
