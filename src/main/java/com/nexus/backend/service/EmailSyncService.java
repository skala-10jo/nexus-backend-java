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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSyncService {

    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final OutlookAuthService outlookAuthService;

    /**
     * 사용자 메일 동기화 (Inbox + SentItems)
     * 삭제 감지 포함
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

            // Inbox 동기화 (삭제 감지 포함)
            totalSynced += syncFolderMails(graphClient, user, "Inbox");

            // SentItems 동기화 (삭제 감지 포함)
            totalSynced += syncFolderMails(graphClient, user, "SentItems");

            log.info("Total synced {} new emails for user: {}", totalSynced, userId);
            return totalSynced;

        } catch (Exception e) {
            log.error("Failed to sync emails for user: {}", userId, e);
            throw new RuntimeException("메일 동기화 실패: " + e.getMessage());
        }
    }

    /**
     * 보낸편지함만 동기화 (메일 전송 후 호출용)
     */
    @Transactional
    public int syncSentItems(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOutlookAccessToken() == null) {
            throw new RuntimeException("Outlook 계정이 연동되지 않았습니다");
        }

        try {
            log.info("Syncing SentItems for user: {}", userId);

            // GraphServiceClient 생성
            com.microsoft.graph.serviceclient.GraphServiceClient graphClient =
                    outlookAuthService.createGraphClient(user);

            // SentItems만 동기화
            int syncedCount = syncFolderMails(graphClient, user, "SentItems");

            log.info("Synced {} new emails from SentItems for user: {}", syncedCount, userId);
            return syncedCount;

        } catch (Exception e) {
            log.error("Failed to sync SentItems for user: {}", userId, e);
            throw new RuntimeException("보낸메일함 동기화 실패: " + e.getMessage());
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

            // [1단계] 모든 메일 ID만 먼저 가져오기 (삭제 감지용, 경량)
            Set<String> allOutlookMessageIds = new HashSet<>();
            try {
                com.microsoft.graph.models.MessageCollectionResponse allIdsResponse =
                        graphClient.me()
                                .mailFolders()
                                .byMailFolderId(folderName)
                                .messages()
                                .get(requestConfig -> {
                                    requestConfig.queryParameters.top = 999;  // 최대한 많이
                                    requestConfig.queryParameters.select = new String[]{"id", "parentFolderId"};  // ID와 폴더 ID
                                });

                if (allIdsResponse != null && allIdsResponse.getValue() != null) {
                    log.info("Raw response from Outlook: {} messages", allIdsResponse.getValue().size());

                    for (var msg : allIdsResponse.getValue()) {
                        allOutlookMessageIds.add(msg.getId());
                    }

                    // 처음 3개 ID 샘플 로그
                    List<String> sampleIds = allOutlookMessageIds.stream()
                        .limit(3)
                        .collect(Collectors.toList());
                    log.info("Retrieved {} total message IDs from Outlook folder: {} (sample: {})",
                        allOutlookMessageIds.size(), folderName, sampleIds);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch all message IDs, will skip deletion detection: {}", e.getMessage(), e);
            }

            // [2단계] 삭제된 메일 먼저 제거
            if (!allOutlookMessageIds.isEmpty()) {
                detectAndDeleteRemovedEmails(user.getId(), folderName, allOutlookMessageIds);
            }

            // [3단계] 최신 메일 상세 정보 가져오기 (신규/업데이트용)
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

            log.info("Retrieved {} recent messages from folder: {}", totalCount, folderName);

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

            log.info("Folder '{}' sync complete: {} new, {} skipped, {} total in Outlook",
                folderName, syncedCount, skippedCount, allOutlookMessageIds.size());
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
     * Outlook에서 삭제된 메일을 DB에서도 제거
     *
     * @param userId 사용자 ID
     * @param folderName 폴더 이름
     * @param outlookMessageIds Outlook에서 가져온 현재 메일 ID 목록
     * @return 삭제된 메일 개수
     */
    private int detectAndDeleteRemovedEmails(UUID userId, String folderName, Set<String> outlookMessageIds) {
        try {
            // DB에서 해당 사용자의 해당 폴더 메일 ID 목록 조회
            List<String> dbMessageIds = emailRepository.findMessageIdsByUserIdAndFolder(userId, folderName);

            // DB 샘플 3개 로그
            List<String> dbSampleIds = dbMessageIds.stream()
                .limit(3)
                .collect(Collectors.toList());

            // Outlook 샘플 3개 로그
            List<String> outlookSampleIds = outlookMessageIds.stream()
                .limit(3)
                .collect(Collectors.toList());

            log.info("DB has {} emails in folder '{}' (sample: {})",
                dbMessageIds.size(), folderName, dbSampleIds);
            log.info("Outlook has {} emails in folder '{}' (sample: {})",
                outlookMessageIds.size(), folderName, outlookSampleIds);

            // DB에만 있고 Outlook에는 없는 메일 찾기
            List<String> messageIdsToDelete = new ArrayList<>();
            for (String dbMessageId : dbMessageIds) {
                if (!outlookMessageIds.contains(dbMessageId)) {
                    messageIdsToDelete.add(dbMessageId);
                }
            }

            log.info("Found {} emails to delete from folder '{}' (to be deleted: {})",
                messageIdsToDelete.size(), folderName,
                messageIdsToDelete.size() > 0 ? messageIdsToDelete.subList(0, Math.min(3, messageIdsToDelete.size())) : "none");

            // 삭제할 메일이 있으면 한 번에 벌크 삭제
            if (!messageIdsToDelete.isEmpty()) {
                log.info("Deleting {} emails from folder '{}' that were removed from Outlook: {}",
                    messageIdsToDelete.size(), folderName, messageIdsToDelete);

                emailRepository.deleteByMessageIdsAndUserId(messageIdsToDelete, userId);

                log.info("Successfully deleted {} emails from folder '{}'", messageIdsToDelete.size(), folderName);
                return messageIdsToDelete.size();
            }

            return 0;

        } catch (Exception e) {
            log.error("Failed to detect and delete removed emails from folder: {}", folderName, e);
            return 0;
        }
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
