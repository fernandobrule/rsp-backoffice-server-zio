package com.atacamasoft.rsp.backoffice.server.infrastructure

import com.atacamasoft.rsp.backoffice.server.application.service.*
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.api.*
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.in.http.*
import com.atacamasoft.rsp.backoffice.server.infrastructure.adapter.out.persistence.*
import com.atacamasoft.rsp.backoffice.server.infrastructure.config.AppConfig
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.*
import zio.http.{Response, Routes}

/** Composition root: wires adapters, services and endpoints (hexagonal architecture). */
object Application:

  def routes(cfg: AppConfig, db: Database): Routes[Any, Response] =
    val schema = cfg.db.schema

    // driven adapters (persistence)
    val users = JdbcUserRepository(db, schema)
    val agencies = JdbcAgencyRepository(db, schema)
    val agents = JdbcAgentRepository(db, schema)
    val properties = JdbcPropertyRepository(db, schema)
    val listings = JdbcListingRepository(db, schema)
    val media = JdbcListingMediaRepository(db, schema)
    val leads = JdbcLeadRepository(db, schema)
    val plans = JdbcPlanRepository(db, schema)
    val subscriptions = JdbcSubscriptionRepository(db, schema)
    val payments = JdbcPaymentRepository(db, schema)

    // application services (use cases)
    val rt = HttpRuntime(PermitAllTokenVerifier())

    // driving adapters (HTTP)
    val endpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] =
      UserApi(UserService(users), rt).endpoints ++
      AgencyApi(AgencyService(agencies), rt).endpoints ++
      AgentApi(AgentService(agents, agencies), rt).endpoints ++
      PropertyApi(PropertyService(properties), rt).endpoints ++
      ListingApi(ListingService(listings), rt).endpoints ++
      ListingMediaApi(ListingMediaService(media, listings), rt).endpoints ++
      LeadApi(LeadService(leads, listings), rt).endpoints ++
      PlanApi(PlanService(plans), rt).endpoints ++
      SubscriptionApi(SubscriptionService(subscriptions), rt).endpoints ++
      PaymentApi(PaymentService(payments), rt).endpoints

    val docs: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] =
      SwaggerInterpreter().fromEndpoints[Task](endpoints.map(_.endpoint), "Real Estate Portal API", "0.1.0")
    val interpreter = ZioHttpInterpreter[Any](ZioHttpServerOptions.default[Any])
    val tapirRoutes = interpreter.toHttp(endpoints ++ docs)

    val cors = CorsPolicy(cfg.cors)

    // order matters: health and preflight first, then the Tapir catch-all
    (HealthRoutes.routes(db) ++ PreflightHandler.routes(cors) ++ tapirRoutes) @@ RequestContextMiddleware(cors)
