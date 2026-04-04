package com.quyong.attendance.module.review.service;

import com.quyong.attendance.module.review.dto.ReviewFeedbackDTO;
import com.quyong.attendance.module.review.dto.ReviewSubmitDTO;
import com.quyong.attendance.module.review.vo.ReviewRecordVO;
import com.quyong.attendance.module.review.vo.ReviewAssistantVO;

public interface ReviewService {

    ReviewRecordVO getLatestByExceptionId(Long exceptionId);

    ReviewAssistantVO getAssistant(Long exceptionId);

    ReviewRecordVO submit(ReviewSubmitDTO dto, Long reviewUserId);

    void feedback(ReviewFeedbackDTO dto);
}
