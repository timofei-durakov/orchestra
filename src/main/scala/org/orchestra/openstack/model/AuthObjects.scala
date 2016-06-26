package org.orchestra.actor.model

/**
  * Created by tdurakov on 19.03.16.
  */
final case class PasswordCredentials(username: String, password: String)

final case class Auth(tenantName: String, passwordCredentials: PasswordCredentials)

final case class AuthRequest(auth: Auth)

final case class Endpoint(adminURL: String, region: String, internalURL: String, id: String, publicURL: String)

final case class Trust(id: String, trustee_user_id: String, trustor_user_id: String, impersonation: Boolean)

final case class Metadata(is_admin: Int, roles: List[String])

final case class Role(name: String)

final case class User(username: String, roles_links: List[String], id: String, roles: List[Role], name: String)

final case class Tenant(description: String, enabled: Boolean, id: String, name: String)

final case class Token(issued_at: String, expires: String, id: String, tenant: Tenant)

final case class Service(name: String, `type`: String, endpoint_links: Option[List[String]], endpoints:List[Endpoint])

final case class Access(token: Token, serviceCatalog: List[Service], user: User, metadata: Metadata,
                        trust: Option[Trust])

final case class AuthResponse(access: Access)