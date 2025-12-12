package com.nexus.backend.service;

import com.microsoft.aad.msal4j.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.nexus.backend.dto.response.DeviceCodeResponse;
import com.nexus.backend.dto.response.OutlookAuthStatusResponse;
import com.nexus.backend.entity.User;
import com.nexus.backend.repository.EmailRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.backend.exception.BadRequestException;
import com.nexus.backend.exception.ResourceNotFoundException;
import com.nexus.backend.exception.ServiceException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OutlookAuthService {

    private final UserRepository userRepository;
    private final EmailRepository emailRepository;

    @Value("${GRAPH_CLIENT_ID:}")
    private String clientId;

    @Value("${GRAPH_TENANT_ID:consumers}")
    private String tenantId;

    private static final Set<String> SCOPES = Set.of(
            "User.Read",
            "Mail.Read",
            "Mail.ReadWrite",
            "Mail.Send",
            "Calendars.Read"
    );

    /**
     * MSAL PublicClientApplication 생성
     */
    private PublicClientApplication buildMsalApp() {
        try {
            String authority = "https://login.microsoftonline.com/" + tenantId;
            return PublicClientApplication.builder(clientId)
                    .authority(authority)
                    .build();
        } catch (Exception e) {
            log.error("Failed to build MSAL app", e);
            throw new ServiceException("MSAL 앱 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Device Flow 인증 시작
     */
    public CompletableFuture<DeviceCodeResponse> initiateDeviceFlow(UUID userId) {
        CompletableFuture<DeviceCodeResponse> future = new CompletableFuture<>();

        try {
            PublicClientApplication app = buildMsalApp();

            Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
                DeviceCodeResponse response = DeviceCodeResponse.builder()
                        .userCode(deviceCode.userCode())
                        .deviceCode(deviceCode.deviceCode())
                        .verificationUri(deviceCode.verificationUri())
                        .message(deviceCode.message())
                        .expiresIn((int) deviceCode.expiresIn())
                        .interval((int) deviceCode.interval())
                        .build();
                future.complete(response);
            };

            DeviceCodeFlowParameters parameters = DeviceCodeFlowParameters
                    .builder(SCOPES, deviceCodeConsumer)
                    .build();

            // 비동기로 토큰 획득 시작
            CompletableFuture<IAuthenticationResult> authFuture =
                    app.acquireToken(parameters);

            authFuture.whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Device code authentication failed", error);
                } else if (result != null) {
                    log.info("Device code authentication succeeded for: {}",
                            result.account().username());

                    // 인증 성공 시 DB에 저장
                    try {
                        saveAuthenticationResult(userId, result);
                    } catch (Exception e) {
                        log.error("Failed to save authentication result", e);
                    }
                }
            });

        } catch (Exception e) {
            log.error("Failed to initiate device flow", e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * 인증 결과를 DB에 저장
     */
    @Transactional
    private void saveAuthenticationResult(UUID userId, IAuthenticationResult result) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setOutlookEmail(result.account().username());
        user.setOutlookAccessToken(result.accessToken());
        user.setOutlookRefreshToken(null); // MSAL handles refresh internally
        user.setOutlookTokenExpiresAt(
                LocalDateTime.ofInstant(
                        result.expiresOnDate().toInstant(),
                        ZoneId.systemDefault()
                )
        );

        userRepository.save(user);
        log.info("Saved authentication result for user: {}, email: {}",
                userId, result.account().username());
    }

    /**
     * Device code로 토큰 획득 및 저장
     */
    @Transactional
    public OutlookAuthStatusResponse completeAuthentication(UUID userId, String deviceCode) {
        try {
            PublicClientApplication app = buildMsalApp();

            // Device code로 토큰 획득
            Consumer<DeviceCode> deviceCodeConsumer = (code) -> {
                // 이미 device code를 가지고 있으므로 아무것도 안함
            };

            DeviceCodeFlowParameters parameters = DeviceCodeFlowParameters
                    .builder(SCOPES, deviceCodeConsumer)
                    .build();

            CompletableFuture<IAuthenticationResult> resultFuture =
                    app.acquireToken(parameters);

            IAuthenticationResult result = resultFuture.join();

            if (result != null && result.accessToken() != null) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

                user.setOutlookEmail(result.account().username());
                user.setOutlookAccessToken(result.accessToken());
                user.setOutlookRefreshToken(null); // MSAL handles refresh internally
                user.setOutlookTokenExpiresAt(
                        LocalDateTime.ofInstant(
                                result.expiresOnDate().toInstant(),
                                ZoneId.systemDefault()
                        )
                );

                userRepository.save(user);

                return OutlookAuthStatusResponse.builder()
                        .isConnected(true)
                        .outlookEmail(user.getOutlookEmail())
                        .tokenExpiresAt(user.getOutlookTokenExpiresAt())
                        .message("Outlook 연동 완료")
                        .build();
            }

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to complete authentication", e);
            throw new ServiceException("인증 완료 실패: " + e.getMessage(), e);
        }

        return OutlookAuthStatusResponse.builder()
                .isConnected(false)
                .message("인증 실패")
                .build();
    }

    /**
     * 사용자의 Outlook 연동 상태 확인
     */
    public OutlookAuthStatusResponse getAuthStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean isConnected = user.getOutlookAccessToken() != null
                && user.getOutlookTokenExpiresAt() != null
                && user.getOutlookTokenExpiresAt().isAfter(LocalDateTime.now());

        return OutlookAuthStatusResponse.builder()
                .isConnected(isConnected)
                .outlookEmail(user.getOutlookEmail())
                .tokenExpiresAt(user.getOutlookTokenExpiresAt())
                .message(isConnected ? "Outlook 연동됨" : "Outlook 연동 필요")
                .build();
    }

    /**
     * Outlook 연동 해제
     */
    @Transactional
    public void disconnect(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 메일 데이터는 유지 (프로젝트 할당 등 보존)
        // emailRepository.deleteByUserId(userId);  // 삭제하지 않음!

        // Clear Outlook authentication data only
        user.setOutlookEmail(null);
        user.setOutlookAccessToken(null);
        user.setOutlookRefreshToken(null);
        user.setOutlookTokenExpiresAt(null);
        user.setOutlookDeltaLink(null);

        userRepository.save(user);
        log.info("Outlook disconnected for user: {} (emails preserved)", userId);
    }

    /**
     * GraphServiceClient 생성 (Access Token 기반)
     */
    public GraphServiceClient createGraphClient(User user) {
        if (user.getOutlookAccessToken() == null) {
            throw new BadRequestException("Outlook 계정이 연동되지 않았습니다");
        }

        // Custom TokenCredential 사용하여 GraphServiceClient 생성
        com.nexus.backend.security.AccessTokenCredential credential =
                new com.nexus.backend.security.AccessTokenCredential(
                        user.getOutlookAccessToken(),
                        user.getOutlookTokenExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                );

        return new GraphServiceClient(credential);
    }
}
