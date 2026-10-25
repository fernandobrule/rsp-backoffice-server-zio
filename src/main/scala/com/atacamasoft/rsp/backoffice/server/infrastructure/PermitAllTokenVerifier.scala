package com.atacamasoft.rsp.backoffice.server.infrastructure

import com.atacamasoft.rsp.backoffice.server.application.port.out.TokenVerifier
import com.atacamasoft.rsp.backoffice.server.domain.DomainError
import zio.*

/**
 * Development verifier: accepts every request. The OpenAPI contract declares bearerAuth (Keycloak JWT);
 * replace this adapter with one that validates the token (signature via JWKS, issuer, expiry).
 */
final class PermitAllTokenVerifier extends TokenVerifier:
  override def verify(token: Option[String]): IO[DomainError, Unit] = ZIO.unit
