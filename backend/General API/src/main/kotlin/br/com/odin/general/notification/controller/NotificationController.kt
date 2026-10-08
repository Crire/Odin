package br.com.odin.general.notification.controller

import br.com.odin.general.identity.service.CurrentUser
import br.com.odin.general.notification.dto.CreateNotificationRuleRequest
import br.com.odin.general.notification.dto.NotificationResponse
import br.com.odin.general.notification.dto.NotificationRuleResponse
import br.com.odin.general.notification.dto.UpdateNotificationRuleRequest
import br.com.odin.general.notification.service.NotificationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @GetMapping("/notifications")
    fun listNotifications(@AuthenticationPrincipal currentUser: CurrentUser): List<NotificationResponse> =
        notificationService.listNotifications(currentUser)

    @PatchMapping("/notifications/{notificationId}/read")
    fun markRead(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable notificationId: UUID,
    ): NotificationResponse = notificationService.markRead(currentUser, notificationId)

    @PostMapping("/notification-rules")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRule(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @Valid @RequestBody request: CreateNotificationRuleRequest,
    ): NotificationRuleResponse = notificationService.createRule(currentUser, request)

    @GetMapping("/notification-rules")
    fun listRules(@AuthenticationPrincipal currentUser: CurrentUser): List<NotificationRuleResponse> =
        notificationService.listRules(currentUser)

    @PatchMapping("/notification-rules/{ruleId}")
    fun updateRule(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable ruleId: UUID,
        @Valid @RequestBody request: UpdateNotificationRuleRequest,
    ): NotificationRuleResponse = notificationService.updateRule(currentUser, ruleId, request)

    @DeleteMapping("/notification-rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRule(
        @AuthenticationPrincipal currentUser: CurrentUser,
        @PathVariable ruleId: UUID,
    ) {
        notificationService.deleteRule(currentUser, ruleId)
    }
}
