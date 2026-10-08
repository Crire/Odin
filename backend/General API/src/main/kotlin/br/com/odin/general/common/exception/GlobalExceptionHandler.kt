package br.com.odin.general.common.exception

import br.com.odin.general.common.web.CorrelationIdFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handleApiException(exception: ApiException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(exception.status).body(
            ApiErrorResponse(
                code = exception.code,
                message = exception.message,
                path = request.requestURI,
            ),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                code = "VALIDATION_ERROR",
                message = "Requisicao invalida.",
                path = request.requestURI,
                details = exception.bindingResult.fieldErrors.map(FieldValidationError::from),
            ),
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                code = "VALIDATION_ERROR",
                message = "Requisicao invalida.",
                path = request.requestURI,
                details = exception.constraintViolations.map {
                    FieldValidationError(field = it.propertyPath.toString(), reason = it.message)
                },
            ),
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiErrorResponse(
                code = "FORBIDDEN",
                message = "Permissao insuficiente.",
                path = request.requestURI,
            ),
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception, request: HttpServletRequest): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(
                code = "INTERNAL_ERROR",
                message = "Erro interno inesperado.",
                path = request.requestURI,
            ),
        )
}

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val path: String,
    val correlationId: String? = MDC.get(CorrelationIdFilter.MDC_KEY),
    val details: List<FieldValidationError> = emptyList(),
    val timestamp: Instant = Instant.now(),
)

data class FieldValidationError(
    val field: String,
    val reason: String,
) {
    companion object {
        fun from(error: FieldError): FieldValidationError =
            FieldValidationError(field = error.field, reason = error.defaultMessage ?: "valor invalido")
    }
}
