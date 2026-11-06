
--
-- Copyright(c) by ATACAMASOFT
-- All rights reserved.
-- This software is confidential and proprietary information of ATACAMASOFT
-- ("Confidential Information"). You shall not disclose such Confidential Information and shall use
-- it only in accordance with the terms of the license agreement you entered with ATACAMASOFT
--

set search_path to public, rsp_db_schema_dev;

--
-- Agency
--

create table if not exists rsp_db_schema_dev.users(
  user_id                  uuid primary key default gen_random_uuid(),
  user_first_name          varchar(100)  not null,
  user_last_name           varchar(100)   not null,
  user_email               varchar(254) not null unique,
  user_email_verified_at   timestamptz,
  user_phone               varchar(30),
  user_keycloak_id         uuid unique not null,
  user_status              varchar(20) not null default 'ACTIVE' check (user_status in (
                             'ACTIVE', 'SUSPENDED', 'DELETED'
                           )),
  user_created_at          timestamptz not null default now(),
  user_updated_at          timestamptz not null default now(),
  user_verified_at         timestamptz
);


create table if not exists rsp_db_schema_dev.agencies(
  agency_id               uuid primary key default gen_random_uuid(),
  agency_name             varchar(255) not null,
  agency_document_number  varchar(20) unique,
  agency_document_type    varchar(20) not null default 'CNPJ' check (agency_document_type in (
                            'CPF', 'CNPJ'
                          )),
  agency_creci_number     varchar(50) unique,
  agency_logo_url         text,
  agency_contact_email    varchar(255),
  agency_status           varchar(20) not null default 'PENDING' check (agency_status in (
                            'PENDING', 'VERIFIED', 'SUSPENDED'
                          )),
  agency_created_at       timestamptz not null default now(),
  agency_updated_at       timestamptz not null default now(),
  agency_verified_at      timestamptz
);


create table if not exists rsp_db_schema_dev.agents(
  agent_agency_id       uuid not null references agencies(agency_id),
  agent_user_id         uuid references users(user_id) on delete cascade,
  agent_role            varchar(20) not null check (agent_role in ('OWNER', 'ADMIN', 'AGENT')),
  agent_license_number  varchar(100) unique not null,
  agent_bio             text,
  agent_rating          numeric(3,2) default 0.00,
  agent_created_at      timestamptz not null default now(),
  agent_updated_at      timestamptz not null default now(),
  agent_verified_at     timestamptz,
  primary key (agent_agency_id, agent_user_id)
);

--
-- Properties
--

create table if not exists rsp_db_schema_dev.properties(
  property_id              uuid primary key default gen_random_uuid(),
  -- type
  property_type            varchar(30) not null check (property_type in (
                             'APARTMENT', 'HOUSE', 'LAND',
                             'COMMERCIAL', 'FARM', 'OTHER'
                           )),
  -- address
  property_country         char(2) not null default 'BR',
  property_state_code      char(2) not null,
  property_city            varchar(100) not null,
  property_postal_code     varchar(20),
  property_neighborhood    varchar(120),
  property_street          varchar(200),
  property_number          varchar(20),
  property_complement      varchar(100),
  property_location        geography(point, 4326),
  property_address_hash    char(64), -- normalized address, for duplicate detection
  -- characteristics
  property_bedrooms        smallint,
  property_bathrooms       smallint,
  property_parking_spots   smallint,
  property_usable_area_m2  numeric(10,2),
  property_total_area_m2   numeric(12,2),
  property_year_built      smallint,
  property_attributes      jsonb not null default '{}', -- type-specific extras
  property_created_at      timestamptz not null default now(),
  property_updated_at      timestamptz not null default now(),
  property_verified_at     timestamptz
);

--
-- Listings
--

create table if not exists rsp_db_schema_dev.listings(
  listing_id                uuid primary key default gen_random_uuid(),
  listing_property_id       uuid not null references properties(property_id),
  listing_agency_id         uuid not null references agencies(agency_id),
  listing_transaction_type  varchar(10) not null check (listing_transaction_type in ('SALE', 'RENT')),
  --
  listing_title             varchar(200) not null,
  listing_description       text,
  listing_price             numeric(14,2) not null,

  listing_currency          char(3) not null default 'BRL',
  listing_condo_fee         numeric(12,2) check (listing_condo_fee >= 0),
  listing_iptu_annual       numeric(12,2) check (listing_iptu_annual >= 0),
  listing_property_tax      numeric(12,2) check (listing_property_tax >= 0),
  --
  listing_published_at      timestamptz,
  listing_expires_at        timestamptz,
  listing_status            varchar(20) not null default 'DRAFT' check (listing_status in (
                              'DRAFT', 'PENDING', 'PUBLISHED',
                              'REJECTED', 'PAUSED', 'SOLD', 'RENTED',
                              'EXPIRED', 'REMOVED'
                            )),
  listing_created_at        timestamptz not null default now(),
  listing_updated_at        timestamptz not null default now(),
  listing_verified_at       timestamptz
);


create table if not exists rsp_db_schema_dev.listing_media (
  listing_media_id              uuid primary key default gen_random_uuid(),
  listing_media_listing_id      uuid not null references listings(listing_id),
  listing_media_storage_key     text not null,
  listing_media_alt_text        varchar(250),
  listing_media_display_order   integer not null default 0,
  listing_media_is_cover        boolean not null default false,
  listing_media_created_at      timestamptz not null default now(),
  listing_media_updated_at      timestamptz not null default now(),
  listing_media_verified_at     timestamptz,
  unique (listing_media_listing_id, listing_media_display_order)
);

--
-- Leads and Contact Requests
--

create table if not exists rsp_db_schema_dev.leads (
  lead_id              uuid primary key default gen_random_uuid(),
  lead_listing_id      uuid not null references listings(listing_id),
  lead_agency_id       uuid not null references agencies(agency_id),
  lead_contact_name    varchar(150) not null,
  lead_contact_email   varchar(254),
  lead_contact_phone   varchar(30),
  lead_message         text,
  lead_source          varchar(30) not null default 'WEBSITE' check (lead_source in (
                         'WEBSITE', 'WHATSAPP', 'PHONE', 'OTHER'
                       )),
  lead_status          varchar(20) not null default 'NEW' check (lead_status in (
                         'NEW', 'CONTACTED', 'QUALIFIED',
                         'CONVERTED', 'CLOSED', 'SPAM'
                       )),
  lead_created_at      timestamptz not null default now(),
  lead_updated_at      timestamptz not null default now(),
  lead_verified_at     timestamptz
);

--
-- Subscription plans and Billing
--

create table if not exists rsp_db_schema_dev.plans (
  plan_id                  uuid primary key default gen_random_uuid(),
  plan_code                varchar(50) not null unique,
  plan_name                varchar(100) not null,
  plan_monthly_price       numeric(12,2) not null check (plan_monthly_price >= 0),
  plan_max_active_listings integer check (plan_max_active_listings >= 0),
  plan_featured_slots      integer not null default 0 check (plan_featured_slots >= 0),
  plan_is_active           boolean not null default true,
  plan_created_at          timestamptz not null default now(),
  plan_updated_at          timestamptz not null default now(),
  plan_verified_at         timestamptz
);


create table if not exists rsp_db_schema_dev.subscriptions (
  subscription_id                        uuid primary key default gen_random_uuid(),
  subscription_agency_id                 uuid not null references agencies(agency_id),
  subscription_plan_id                   uuid not null references plans(plan_id),
  subscription_status                    varchar(20) not null check (subscription_status in (
                                            'TRIALING', 'ACTIVE', 'PAST_DUE',
                                            'CANCELED', 'EXPIRED'
                                         )),
  subscription_billing_provider          varchar(40),
  subscription_provider_subscription_id  text,
  subscription_current_period_start      timestamptz,
  subscription_current_period_end        timestamptz,
  subscription_created_at                timestamptz not null default now(),
  subscription_updated_at                timestamptz not null default now(),
  subscription_verified_at               timestamptz
);


create table if not exists rsp_db_schema_dev.payments (
  payment_id                   uuid primary key default gen_random_uuid(),
  payment_subscription_id      uuid not null references subscriptions(subscription_id),
  payment_provider_payment_id  text,
  payment_amount               numeric(12,2) not null check (payment_amount >= 0),
  payment_currency             char(3) not null default 'BRL',
  payment_status               varchar(20) not null check (payment_status in (
                                 'PENDING', 'PAID', 'FAILED',
                                 'REFUNDED', 'CANCELED'
                               )),
  payment_paid_at              timestamptz,
  payment_created_at           timestamptz not null default now(),
  payment_updated_at           timestamptz not null default now(),
  payment_verified_at          timestamptz
);
