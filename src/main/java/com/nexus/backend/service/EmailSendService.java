package com.nexus.backend.service;

import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.nexus.backend.dto.request.SendEmailRequest;
import com.nexus.backend.entity.User;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.ServiceException;
import com.nexus.backend.exception.UnauthorizedException;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmailSendService {

    private final UserRepository userRepository;
    private final OutlookAuthService outlookAuthService;
    private final EmailSyncService emailSyncService;

    /**
     * 메일 발송
     */
    public void sendEmail(UUID userId, SendEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getOutlookAccessToken() == null) {
            throw new UnauthorizedException("Outlook 계정이 연동되지 않았습니다");
        }

        try {
            GraphServiceClient graphClient = outlookAuthService.createGraphClient(user);

            // Message 객체 생성
            Message message = new Message();
            message.setSubject(request.getSubject());

            // 본문 설정
            ItemBody body = new ItemBody();
            body.setContentType(request.getBodyType().equalsIgnoreCase("HTML") ? BodyType.Html : BodyType.Text);
            body.setContent(request.getBody());
            message.setBody(body);

            // 받는 사람
            List<Recipient> toRecipients = new ArrayList<>();
            for (String email : request.getToRecipients()) {
                Recipient recipient = new Recipient();
                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setAddress(email);
                recipient.setEmailAddress(emailAddress);
                toRecipients.add(recipient);
            }
            message.setToRecipients(toRecipients);

            // 참조
            if (request.getCcRecipients() != null && !request.getCcRecipients().isEmpty()) {
                List<Recipient> ccRecipients = new ArrayList<>();
                for (String email : request.getCcRecipients()) {
                    Recipient recipient = new Recipient();
                    EmailAddress emailAddress = new EmailAddress();
                    emailAddress.setAddress(email);
                    recipient.setEmailAddress(emailAddress);
                    ccRecipients.add(recipient);
                }
                message.setCcRecipients(ccRecipients);
            }

            // BCC
            if (request.getBccRecipients() != null && !request.getBccRecipients().isEmpty()) {
                List<Recipient> bccRecipients = new ArrayList<>();
                for (String email : request.getBccRecipients()) {
                    Recipient recipient = new Recipient();
                    EmailAddress emailAddress = new EmailAddress();
                    emailAddress.setAddress(email);
                    recipient.setEmailAddress(emailAddress);
                    bccRecipients.add(recipient);
                }
                message.setBccRecipients(bccRecipients);
            }

            // 메일 발송
            com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody requestBody =
                    new com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody();
            requestBody.setMessage(message);
            requestBody.setSaveToSentItems(true); // 보낸편지함에 저장

            graphClient.me().sendMail().post(requestBody);

            log.info("Email sent successfully from user: {}", userId);

            // 보낸편지함 동기화 (비동기로 실행하여 메일 전송 응답 속도 유지)
            try {
                emailSyncService.syncSentItems(userId);
                log.info("SentItems synced after sending email for user: {}", userId);
            } catch (Exception syncError) {
                // 동기화 실패해도 메일은 이미 전송되었으므로 로그만 남김
                log.error("Failed to sync SentItems after sending email", syncError);
            }

        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new ServiceException("메일 발송 실패: " + e.getMessage(), e);
        }
    }
}
