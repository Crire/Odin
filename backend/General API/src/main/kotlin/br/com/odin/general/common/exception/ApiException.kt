package br.com.odin.general.common.exception

import org.springframework.http.HttpStatus

open class ApiException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
) : RuntimeException(message)

class ConflictException(message: String) : ApiException(HttpStatus.CONFLICT, "CONFLICT", message)

class BadRequestException(message: String) : ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message)

class ForbiddenException(message: String = "Permissao insuficiente.") :
    ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message)

class NotFoundException(resource: String) : ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "$resource nao encontrado.")

class UnauthorizedException(message: String = "Credenciais invalidas.") :
    ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message)

class UpstreamServiceException(message: String) :
    ApiException(HttpStatus.BAD_GATEWAY, "UPSTREAM_SERVICE_ERROR", message)
