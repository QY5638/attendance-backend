package com.quyong.attendance.module.face.service.impl;

import com.quyong.attendance.common.api.ResultCode;
import com.quyong.attendance.common.exception.BusinessException;
import com.quyong.attendance.module.face.config.FaceEngineProperties;
import com.quyong.attendance.module.face.dto.FaceLivenessCompleteDTO;
import com.quyong.attendance.module.face.service.FaceLivenessService;
import com.quyong.attendance.module.face.support.FaceLivenessProof;
import com.quyong.attendance.module.face.support.FaceLivenessSession;
import com.quyong.attendance.module.face.support.FaceLivenessStore;
import com.quyong.attendance.module.face.vo.FaceLivenessCompleteVO;
import com.quyong.attendance.module.face.vo.FaceLivenessSessionVO;
import com.quyong.attendance.module.statistics.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FaceLivenessServiceImpl implements FaceLivenessService {

    private static final List<String> ACTION_POOL = Collections.unmodifiableList(Arrays.asList("BLINK", "TURN_LEFT", "TURN_RIGHT", "MOUTH_OPEN"));

    private final FaceEngineProperties properties;
    private final FaceLivenessStore faceLivenessStore;
    private final OperationLogService operationLogService;

    public FaceLivenessServiceImpl(FaceEngineProperties properties,
                                   FaceLivenessStore faceLivenessStore,
                                   OperationLogService operationLogService) {
        this.properties = properties;
        this.faceLivenessStore = faceLivenessStore;
        this.operationLogService = operationLogService;
    }

    @Override
    public FaceLivenessSessionVO createSession(Long userId) {
        Instant now = Instant.now();
        Duration ttl = sessionTtl();
        FaceLivenessSession session = new FaceLivenessSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setActions(buildActions());
        session.setIssuedAt(now);
        session.setExpireAt(now.plus(ttl));
        faceLivenessStore.storeSession(session, ttl);
        operationLogService.save(userId, "FACE_LIVENESS_SESSION", "创建活体挑战，会话=" + session.getSessionId());

        FaceLivenessSessionVO vo = new FaceLivenessSessionVO();
        vo.setSessionId(session.getSessionId());
        vo.setActions(session.getActions());
        vo.setIssuedAt(session.getIssuedAt().toEpochMilli());
        vo.setExpiresAt(session.getExpireAt().toEpochMilli());
        return vo;
    }

    @Override
    public FaceLivenessCompleteVO complete(Long userId, FaceLivenessCompleteDTO completeDTO) {
        FaceLivenessSession session = faceLivenessStore.getSession(safeText(completeDTO == null ? null : completeDTO.getSessionId()));
        if (session == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战会话不存在或已失效，请重新开始");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权提交其他用户的活体挑战结果");
        }

        validateCompletionPayload(session, completeDTO);
        BigDecimal overallScore = calculateOverallScore(completeDTO.getActionScores(), session.getActions());

        FaceLivenessProof proof = new FaceLivenessProof();
        proof.setToken(UUID.randomUUID().toString().replace("-", ""));
        proof.setUserId(userId);
        proof.setImageHash(hashImage(completeDTO.getImageData()));
        proof.setLivenessScore(overallScore);
        proof.setExpireAt(Instant.now().plus(proofTtl()));
        faceLivenessStore.storeProof(proof, proofTtl());
        faceLivenessStore.deleteSession(session.getSessionId());
        operationLogService.save(userId, "FACE_LIVENESS_PASS", "完成活体挑战，分值=" + overallScore.toPlainString());

        FaceLivenessCompleteVO vo = new FaceLivenessCompleteVO();
        vo.setSessionId(session.getSessionId());
        vo.setLivenessToken(proof.getToken());
        vo.setExpiresAt(proof.getExpireAt().toEpochMilli());
        vo.setLivenessScore(overallScore);
        vo.setMessage("活体挑战通过");
        return vo;
    }

    @Override
    public FaceLivenessProof requireValidProof(Long userId, String livenessToken, String imageData, boolean consume) {
        if (!Boolean.TRUE.equals(properties.getRequireLiveness())) {
            return null;
        }
        if (!StringUtils.hasText(livenessToken)) {
            operationLogService.save(userId, "FACE_LIVENESS_REJECT", "活体证明缺失");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前操作需要先完成活体校验，请使用摄像头完成挑战");
        }
        FaceLivenessProof proof = faceLivenessStore.getProof(livenessToken.trim());
        if (proof == null || proof.getUserId() == null || !proof.getUserId().equals(userId) || proof.getExpireAt() == null || !proof.getExpireAt().isAfter(Instant.now())) {
            operationLogService.save(userId, "FACE_LIVENESS_REJECT", "活体证明无效或已过期");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体校验证明已失效，请重新完成挑战");
        }
        if (!proof.getImageHash().equals(hashImage(imageData))) {
            operationLogService.save(userId, "FACE_LIVENESS_REJECT", "活体图像与提交图像不一致");
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体校验图像与当前提交图像不一致，请重新完成挑战");
        }
        if (consume) {
            faceLivenessStore.deleteProof(proof.getToken());
            operationLogService.save(userId, "FACE_LIVENESS_CONSUME", "消费活体证明，token=" + proof.getToken());
        }
        return proof;
    }

    private void validateCompletionPayload(FaceLivenessSession session, FaceLivenessCompleteDTO completeDTO) {
        if (completeDTO == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战结果不能为空");
        }
        if (!StringUtils.hasText(safeText(completeDTO.getImageData()))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战结果缺少抓拍图像，请重新完成挑战");
        }

        List<String> completedActions = normalizeActions(completeDTO.getCompletedActions());
        List<String> expectedActions = normalizeActions(session.getActions());
        if (!expectedActions.equals(completedActions)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战动作不完整或顺序不正确，请重新挑战");
        }

        long startedAt = completeDTO.getStartedAt() == null ? 0L : completeDTO.getStartedAt().longValue();
        long completedAt = completeDTO.getCompletedAt() == null ? 0L : completeDTO.getCompletedAt().longValue();
        if (startedAt <= 0L || completedAt <= 0L || completedAt <= startedAt) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战时间戳无效，请重新挑战");
        }
        long durationMs = completedAt - startedAt;
        if (durationMs < minDurationMs() || durationMs > maxDurationMs()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战时长异常，请重新挑战");
        }

        int sampleCount = completeDTO.getSampleCount() == null ? 0 : completeDTO.getSampleCount().intValue();
        if (sampleCount < minSampleCount()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体检测采样不足，请保持正对摄像头后重试");
        }

        int stableFaceFrames = completeDTO.getStableFaceFrames() == null ? 0 : completeDTO.getStableFaceFrames().intValue();
        if (stableFaceFrames < expectedActions.size()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未稳定识别到人脸，请保持单人正对摄像头");
        }

        Map<String, BigDecimal> actionScores = completeDTO.getActionScores();
        if (actionScores == null || actionScores.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "活体挑战缺少动作评分，请重新挑战");
        }

        for (String action : expectedActions) {
            BigDecimal score = actionScores.get(action);
            if (score == null || score.compareTo(thresholdFor(action)) < 0) {
                operationLogService.save(session.getUserId(), "FACE_LIVENESS_FAIL", "活体挑战失败，动作=" + action + "，分值不足");
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), labelFor(action) + "动作未达到活体校验要求，请重试");
            }
        }
    }

    private BigDecimal calculateOverallScore(Map<String, BigDecimal> actionScores, List<String> actions) {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (String action : normalizeActions(actions)) {
            BigDecimal score = actionScores == null ? null : actionScores.get(action);
            if (score == null) {
                continue;
            }
            total = total.add(score.multiply(new BigDecimal("100")));
            count++;
        }
        if (count == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private String hashImage(String imageData) {
        String normalized = safeText(imageData);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "人脸图像不能为空");
        }
        int separatorIndex = normalized.indexOf(',');
        if (separatorIndex >= 0) {
            normalized = normalized.substring(separatorIndex + 1);
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = messageDigest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digestBytes.length * 2);
            for (byte digestByte : digestBytes) {
                builder.append(String.format("%02x", digestByte));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("活体图像摘要生成失败", exception);
        }
    }

    private List<String> buildActions() {
        ArrayList<String> pool = new ArrayList<String>(ACTION_POOL);
        Collections.shuffle(pool, ThreadLocalRandom.current());
        int actionCount = Math.min(pool.size(), Math.max(2, properties.getLivenessActionCount() == null ? 3 : properties.getLivenessActionCount().intValue()));
        return Collections.unmodifiableList(new ArrayList<String>(pool.subList(0, actionCount)));
    }

    private List<String> normalizeActions(List<String> actions) {
        if (actions == null) {
            return Collections.emptyList();
        }
        ArrayList<String> normalized = new ArrayList<String>();
        for (String action : actions) {
            if (StringUtils.hasText(action)) {
                normalized.add(action.trim().toUpperCase());
            }
        }
        return normalized;
    }

    private BigDecimal thresholdFor(String action) {
        if ("BLINK".equals(action)) {
            return defaultThreshold(properties.getLivenessBlinkScoreThreshold(), "0.65");
        }
        if ("MOUTH_OPEN".equals(action)) {
            return defaultThreshold(properties.getLivenessMouthOpenScoreThreshold(), "0.65");
        }
        return defaultThreshold(properties.getLivenessTurnScoreThreshold(), "0.70");
    }

    private String labelFor(String action) {
        if ("BLINK".equals(action)) {
            return "眨眼";
        }
        if ("TURN_LEFT".equals(action)) {
            return "向左转头";
        }
        if ("TURN_RIGHT".equals(action)) {
            return "向右转头";
        }
        if ("MOUTH_OPEN".equals(action)) {
            return "张嘴";
        }
        return action;
    }

    private BigDecimal defaultThreshold(BigDecimal value, String fallback) {
        return value == null ? new BigDecimal(fallback) : value;
    }

    private Duration sessionTtl() {
        Long seconds = properties.getLivenessSessionTtlSeconds();
        return Duration.ofSeconds(seconds == null || seconds.longValue() < 30L ? 180L : seconds.longValue());
    }

    private Duration proofTtl() {
        Long seconds = properties.getLivenessProofTtlSeconds();
        return Duration.ofSeconds(seconds == null || seconds.longValue() < 30L ? 180L : seconds.longValue());
    }

    private long minDurationMs() {
        Long value = properties.getLivenessMinDurationMs();
        return value == null || value.longValue() < 500L ? 3000L : value.longValue();
    }

    private long maxDurationMs() {
        Long value = properties.getLivenessMaxDurationMs();
        return value == null || value.longValue() < 5000L ? 45000L : value.longValue();
    }

    private int minSampleCount() {
        Integer value = properties.getLivenessMinSampleCount();
        return value == null || value.intValue() < 3 ? 12 : value.intValue();
    }

    private String safeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
