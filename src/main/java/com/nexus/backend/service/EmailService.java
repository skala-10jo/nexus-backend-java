package com.nexus.backend.service;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.nexus.backend.dto.request.AssignProjectRequest;
import com.nexus.backend.dto.request.UpdateReadStatusRequest;
import com.nexus.backend.dto.response.EmailDetailResponse;
import com.nexus.backend.dto.response.EmailResponse;
import com.nexus.backend.entity.Email;
import com.nexus.backend.entity.Project;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.ServiceException;
import com.nexus.backend.repository.EmailRepository;
import com.nexus.backend.repository.ProjectRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final OutlookAuthService outlookAuthService;

    /**
     * 메일 목록 조회
     */
    public Page<EmailResponse> getEmails(UUID userId, String folder, UUID projectId, Pageable pageable) {
        Page<Email> emails;

        if (folder != null && projectId != null) {
            // 폴더 + 프로젝트 필터
            emails = emailRepository.advancedSearch(userId, "", folder, projectId, pageable);
        } else if (folder != null) {
            // 폴더만 필터
            emails = emailRepository.findByUserIdAndFolder(userId, folder, pageable);
        } else if (projectId != null) {
            // 프로젝트만 필터
            emails = emailRepository.findByUserIdAndProjectId(userId, projectId, pageable);
        } else {
            // 전체 조회
            emails = emailRepository.findByUserId(userId, pageable);
        }

        return emails.map(this::toEmailResponse);
    }

    /**
     * 메일 상세 조회
     */
    public EmailDetailResponse getEmailDetail(UUID userId, UUID emailId) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Email", "id", emailId));

        return toEmailDetailResponse(email);
    }

    /**
     * 메일 검색
     */
    public Page<EmailResponse> searchEmails(UUID userId, String query, Pageable pageable) {
        Page<Email> emails = emailRepository.searchEmails(userId, query, pageable);
        return emails.map(this::toEmailResponse);
    }

    /**
     * 읽음 상태 변경 (양방향 동기화)
     */
    @Transactional
    public EmailResponse updateReadStatus(UUID userId, UUID emailId, UpdateReadStatusRequest request) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Email", "id", emailId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 1. DB 업데이트
        email.setIsRead(request.getIsRead());
        emailRepository.save(email);

        // 2. Microsoft Graph API로 동기화
        try {
            GraphServiceClient graphClient = outlookAuthService.createGraphClient(user);
            com.microsoft.graph.models.Message message = new com.microsoft.graph.models.Message();
            message.setIsRead(request.getIsRead());

            graphClient.me().messages().byMessageId(email.getMessageId())
                    .patch(message);

            log.info("Read status updated in Outlook for email: {}", emailId);
        } catch (Exception e) {
            log.error("Failed to update read status in Outlook", e);
            // DB는 업데이트되었으므로 계속 진행
        }

        return toEmailResponse(email);
    }

    /**
     * 프로젝트 할당
     */
    @Transactional
    public EmailResponse assignProject(UUID userId, UUID emailId, AssignProjectRequest request) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Email", "id", emailId));

        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));
            email.setProject(project);
        } else {
            email.setProject(null);
        }

        emailRepository.save(email);
        log.info("Project assigned to email: {}", emailId);

        return toEmailResponse(email);
    }

    /**
     * 메일 삭제
     */
    @Transactional
    public void deleteEmail(UUID userId, UUID emailId) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Email", "id", emailId));

        emailRepository.delete(email);
        log.info("Email deleted: {}", emailId);
    }

    /**
     * 안읽은 메일 개수
     */
    public long getUnreadCount(UUID userId) {
        return emailRepository.countByUserIdAndIsRead(userId, false);
    }

    // ========== Mapping Methods ==========

    private EmailResponse toEmailResponse(Email email) {
        return EmailResponse.builder()
                .id(email.getId())
                .messageId(email.getMessageId())
                .subject(email.getSubject())
                .fromAddress(email.getFromAddress())
                .fromName(email.getFromName())
                .toRecipients(email.getToRecipients())  // 수신자 추가
                .bodyPreview(email.getBodyPreview())
                .hasAttachments(email.getHasAttachments())
                .isRead(email.getIsRead())
                .folder(email.getFolder())
                .receivedDateTime(email.getReceivedDateTime())
                .sentDateTime(email.getSentDateTime())
                .projectId(email.getProject() != null ? email.getProject().getId() : null)
                .projectName(email.getProject() != null ? email.getProject().getName() : null)
                .syncedAt(email.getSyncedAt())
                .build();
    }

    private EmailDetailResponse toEmailDetailResponse(Email email) {
        return EmailDetailResponse.builder()
                .id(email.getId())
                .messageId(email.getMessageId())
                .subject(email.getSubject())
                .fromAddress(email.getFromAddress())
                .fromName(email.getFromName())
                .toRecipients(email.getToRecipients())
                .ccRecipients(email.getCcRecipients())
                .bccRecipients(email.getBccRecipients())
                .body(email.getBody())
                .bodyPreview(email.getBodyPreview())
                .bodyType(email.getBodyType())
                .hasAttachments(email.getHasAttachments())
                .isRead(email.getIsRead())
                .conversationId(email.getConversationId())
                .folder(email.getFolder())
                .receivedDateTime(email.getReceivedDateTime())
                .sentDateTime(email.getSentDateTime())
                .projectId(email.getProject() != null ? email.getProject().getId() : null)
                .projectName(email.getProject() != null ? email.getProject().getName() : null)
                .attachments(null) // TODO: 첨부파일 매핑
                .syncedAt(email.getSyncedAt())
                .build();
    }
}
