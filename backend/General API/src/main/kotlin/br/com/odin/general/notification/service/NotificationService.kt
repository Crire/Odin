package br.com.odin.general.notification.service

import br.com.odin.general.common.exception.BadRequestException
import br.com.odin.general.common.exception.NotFoundException
import br.com.odin.general.group.repository.GroupRepository
import br.com.odin.general.group.service.GroupPermissionService
import br.com.odin.general.identity.repository.UserRepository
import br.com.odin.general.identity.service.CurrentUser
import br.com.odin.general.notification.domain.NotificationRule
import br.com.odin.general.notification.dto.CreateNotificationRuleRequest
import br.com.odin.general.notification.dto.NotificationResponse
import br.com.odin.general.notification.dto.NotificationRuleResponse
import br.com.odin.general.notification.dto.UpdateNotificationRuleRequest
import br.com.odin.general.notification.repository.NotificationJobRepository
import br.com.odin.general.notification.repository.NotificationRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val notificationRuleRepository: NotificationRuleRepository,
    private val notificationJobRepository: NotificationJobRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val groupPermissionService: GroupPermissionService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun listNotifications(currentUser: CurrentUser): List<NotificationResponse> =
        notificationJobRepository.findAllByUser_IdOrderByCreatedAtDesc(currentUser.id).map(NotificationResponse::from)

    @Transactional
    fun markRead(currentUser: CurrentUser, notificationId: UUID): NotificationResponse {
        val job = notificationJobRepository.findByIdAndUser_Id(notificationId, currentUser.id)
            ?: throw NotFoundException("Notificacao")
        job.readAt = Instant.now(clock)
        return NotificationResponse.from(job)
    }

    @Transactional
    fun createRule(currentUser: CurrentUser, request: CreateNotificationRuleRequest): NotificationRuleResponse {
        validateTimeRange(request.startTime, request.endTime)
        val user = userRepository.findById(currentUser.id).orElseThrow { NotFoundException("Usuario") }
        val group = request.groupId?.let {
            groupPermissionService.requireActiveMember(it, currentUser.id)
            groupRepository.findById(it).orElseThrow { NotFoundException("Grupo") }
        }
        val rule = notificationRuleRepository.save(
            NotificationRule(
                user = user,
                group = group,
                intervalMinutes = request.intervalMinutes,
                startTime = request.startTime,
                endTime = request.endTime,
                daysOfWeek = request.daysOfWeek,
                enabled = request.enabled,
            ),
        )
        return NotificationRuleResponse.from(rule)
    }

    @Transactional(readOnly = true)
    fun listRules(currentUser: CurrentUser): List<NotificationRuleResponse> =
        notificationRuleRepository.findAllByUser_IdOrderByCreatedAtDesc(currentUser.id).map(NotificationRuleResponse::from)

    @Transactional
    fun updateRule(currentUser: CurrentUser, ruleId: UUID, request: UpdateNotificationRuleRequest): NotificationRuleResponse {
        validateTimeRange(request.startTime, request.endTime)
        val rule = notificationRuleRepository.findByIdAndUser_Id(ruleId, currentUser.id) ?: throw NotFoundException("Regra")
        rule.intervalMinutes = request.intervalMinutes
        rule.startTime = request.startTime
        rule.endTime = request.endTime
        rule.daysOfWeek = request.daysOfWeek
        rule.enabled = request.enabled
        return NotificationRuleResponse.from(rule)
    }

    @Transactional
    fun deleteRule(currentUser: CurrentUser, ruleId: UUID) {
        val rule = notificationRuleRepository.findByIdAndUser_Id(ruleId, currentUser.id) ?: throw NotFoundException("Regra")
        notificationRuleRepository.delete(rule)
    }

    private fun validateTimeRange(start: java.time.LocalTime, end: java.time.LocalTime) {
        if (!start.isBefore(end)) {
            throw BadRequestException("Horario inicial deve ser anterior ao horario final.")
        }
    }
}
