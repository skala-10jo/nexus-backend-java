package com.nexus.backend.service;

import com.nexus.backend.entity.Email;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.EmailRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSyncService {

    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final OutlookAuthService outlookAuthService;

    /**
     * 사용자 메일 동기화 (Inbox + SentItems)
     */
    @Transactional
    public int syncUserEmails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOutlookAccessToken() == null) {
            throw new RuntimeException("Outlook 계정이 연동되지 않았습니다");
        }

        try {
            log.info("Syncing emails for user: {}", userId);

            // GraphServiceClient 생성
            com.microsoft.graph.serviceclient.GraphServiceClient graphClient =
                    outlookAuthService.createGraphClient(user);

            int totalSynced = 0;

            // Inbox 동기화
            totalSynced += syncFolderMails(graphClient, user, "Inbox");

            // SentItems 동기화
            totalSynced += syncFolderMails(graphClient, user, "SentItems");

            log.info("Total synced {} new emails for user: {}", totalSynced, userId);
            return totalSynced;

        } catch (Exception e) {
            log.error("Failed to sync emails for user: {}", userId, e);
            throw new RuntimeException("메일 동기화 실패: " + e.getMessage());
        }
    }

    /**
     * 특정 폴더의 메일 동기화
     */
    private int syncFolderMails(
            com.microsoft.graph.serviceclient.GraphServiceClient graphClient,
            User user,
            String folderName
    ) {
        try {
            log.info("Syncing folder: {} for user: {}", folderName, user.getId());

            // 폴더별 메일 가져오기
            com.microsoft.graph.models.MessageCollectionResponse messagesResponse =
                    graphClient.me()
                            .mailFolders()
                            .byMailFolderId(folderName)
                            .messages()
                            .get(requestConfig -> {
                                requestConfig.queryParameters.top = 50;
                                requestConfig.queryParameters.orderby = new String[]{"receivedDateTime DESC"};
                                requestConfig.queryParameters.select = new String[]{
                                        "id", "subject", "from", "toRecipients", "ccRecipients",
                                        "body", "bodyPreview", "hasAttachments", "isRead",
                                        "conversationId", "receivedDateTime", "sentDateTime",
                                        "parentFolderId"
                                };
                            });

            if (messagesResponse == null || messagesResponse.getValue() == null) {
                log.warn("No messages returned from folder: {}", folderName);
                return 0;
            }

            int syncedCount = 0;
            int skippedCount = 0;
            int totalCount = messagesResponse.getValue().size();

            log.info("Retrieved {} messages from folder: {}", totalCount, folderName);

            for (com.microsoft.graph.models.Message graphMessage : messagesResponse.getValue()) {
                // 이미 존재하는 메일인지 확인 (해당 사용자의 메일만)
                if (emailRepository.existsByMessageIdAndUserId(graphMessage.getId(), user.getId())) {
                    log.debug("Email already exists for user: {} (folder: {})", graphMessage.getId(), folderName);
                    skippedCount++;
                    continue;
                }

                // Graph Message를 Email 엔티티로 변환
                Email email = convertToEmail(graphMessage, user);
                email.setFolder(folderName);  // 폴더 정보 설정

                // DB에 저장
                emailRepository.save(email);
                syncedCount++;

                log.debug("Saved email: {} - folder: {}", email.getSubject(), folderName);
            }

            log.info("Folder '{}' sync complete: {} new, {} skipped, {} total",
                folderName, syncedCount, skippedCount, totalCount);
            return syncedCount;

        } catch (Exception e) {
            log.error("Failed to sync folder: {}", folderName, e);
            // 한 폴더 실패해도 계속 진행
            return 0;
        }
    }

    /**
     * Microsoft Graph 메시지를 Email 엔티티로 변환
     */
    private Email convertToEmail(com.microsoft.graph.models.Message graphMessage, User user) {
        Email email = new Email();
        email.setUser(user);
        email.setMessageId(graphMessage.getId());
        email.setSubject(graphMessage.getSubject());

        // 발신자 정보
        if (graphMessage.getFrom() != null && graphMessage.getFrom().getEmailAddress() != null) {
            email.setFromAddress(graphMessage.getFrom().getEmailAddress().getAddress());
            email.setFromName(graphMessage.getFrom().getEmailAddress().getName());
        }

        // 수신자 정보
        if (graphMessage.getToRecipients() != null && !graphMessage.getToRecipients().isEmpty()) {
            StringBuilder toRecipients = new StringBuilder();
            for (var recipient : graphMessage.getToRecipients()) {
                if (recipient.getEmailAddress() != null) {
                    toRecipients.append(recipient.getEmailAddress().getAddress()).append("; ");
                }
            }
            email.setToRecipients(toRecipients.toString());
        }

        // 참조 정보
        if (graphMessage.getCcRecipients() != null && !graphMessage.getCcRecipients().isEmpty()) {
            StringBuilder ccRecipients = new StringBuilder();
            for (var recipient : graphMessage.getCcRecipients()) {
                if (recipient.getEmailAddress() != null) {
                    ccRecipients.append(recipient.getEmailAddress().getAddress()).append("; ");
                }
            }
            email.setCcRecipients(ccRecipients.toString());
        }

        // 본문
        if (graphMessage.getBody() != null) {
            email.setBody(graphMessage.getBody().getContent());
            email.setBodyType(graphMessage.getBody().getContentType().toString());
        }

        email.setBodyPreview(graphMessage.getBodyPreview());
        email.setHasAttachments(graphMessage.getHasAttachments() != null ? graphMessage.getHasAttachments() : false);
        email.setIsRead(graphMessage.getIsRead() != null ? graphMessage.getIsRead() : false);
        email.setConversationId(graphMessage.getConversationId());

        // 폴더는 syncFolderMails에서 설정됨 (기본값 설정하지 않음)

        // 날짜 정보
        if (graphMessage.getReceivedDateTime() != null) {
            email.setReceivedDateTime(
                    LocalDateTime.ofInstant(
                            graphMessage.getReceivedDateTime().toInstant(),
                            ZoneId.systemDefault()
                    )
            );
        }

        if (graphMessage.getSentDateTime() != null) {
            email.setSentDateTime(
                    LocalDateTime.ofInstant(
                            graphMessage.getSentDateTime().toInstant(),
                            ZoneId.systemDefault()
                    )
            );
        }

        email.setSyncedAt(LocalDateTime.now());

        return email;
    }

    /**
     * Delta 동기화 (변경된 메일만 가져오기)
     */
    @Transactional
    public int deltaSyncEmails(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOutlookDeltaLink() == null) {
            // 첫 동기화인 경우 전체 동기화
            return syncUserEmails(userId);
        }

        try {
            // TODO: Delta link를 사용한 증분 동기화 구현
            log.info("Delta syncing emails for user: {}", userId);

            return 0;

        } catch (Exception e) {
            log.error("Failed to delta sync emails for user: {}", userId, e);
            throw new RuntimeException("Delta 동기화 실패: " + e.getMessage());
        }
    }
}
