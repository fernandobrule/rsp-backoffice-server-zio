package com.atacamasoft.rsp.backoffice.server.domain

/** Tri-state used by PATCH commands: property absent, explicitly null, or assigned. */
enum Patch[+A]:
  case Keep
  case Clear
  case Assign(value: A)

object Patch:
  extension [A](p: Patch[A])
    def isPresent: Boolean = p match
      case Patch.Keep => false
      case _          => true
    def applyTo(current: Option[A]): Option[A] = p match
      case Patch.Keep      => current
      case Patch.Clear     => None
      case Patch.Assign(a) => Some(a)

final case class FieldIssue(field: Option[String], issue: String)

enum DomainError:
  case NotFound(resource: String, id: String)
  case Conflict(message: String)
  case Unprocessable(message: String, details: List[FieldIssue] = Nil)
  case BadInput(message: String, details: List[FieldIssue] = Nil)
  case Unauthorized(message: String)
  case Internal(message: String, cause: Option[Throwable] = None)

final case class PageRequest(limit: Int, offset: Int)
final case class Page[+A](items: List[A], total: Long, limit: Int, offset: Int)
