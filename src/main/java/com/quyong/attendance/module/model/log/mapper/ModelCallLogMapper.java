package com.quyong.attendance.module.model.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.model.log.entity.ModelCallLog;
import com.quyong.attendance.module.model.log.vo.ModelCallLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ModelCallLogMapper extends BaseMapper<ModelCallLog> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM modelCallLog ml",
            "WHERE 1 = 1",
            "<if test='businessType != null and businessType != \"\"'> AND ml.businessType = #{businessType}</if>",
            "<if test='businessId != null'> AND ml.businessId = #{businessId}</if>",
            "<if test='promptTemplateId != null'> AND ml.promptTemplateId = #{promptTemplateId}</if>",
            "<if test='status != null and status != \"\"'> AND ml.status = #{status}</if>",
            "<if test='startTime != null'> AND ml.createTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ml.createTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countByQuery(@Param("businessType") String businessType,
                      @Param("businessId") Long businessId,
                      @Param("promptTemplateId") Long promptTemplateId,
                      @Param("status") String status,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ml.id, ml.businessType, ml.businessId, ml.promptTemplateId, ml.inputSummary, ml.outputSummary, ml.status, ml.latencyMs, ml.errorMessage, ml.createTime",
            "FROM modelCallLog ml",
            "WHERE 1 = 1",
            "<if test='businessType != null and businessType != \"\"'> AND ml.businessType = #{businessType}</if>",
            "<if test='businessId != null'> AND ml.businessId = #{businessId}</if>",
            "<if test='promptTemplateId != null'> AND ml.promptTemplateId = #{promptTemplateId}</if>",
            "<if test='status != null and status != \"\"'> AND ml.status = #{status}</if>",
            "<if test='startTime != null'> AND ml.createTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ml.createTime &lt;= #{endTime}</if>",
            "ORDER BY ml.createTime DESC, ml.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<ModelCallLogVO> selectPageByQuery(@Param("businessType") String businessType,
                                           @Param("businessId") Long businessId,
                                           @Param("promptTemplateId") Long promptTemplateId,
                                           @Param("status") String status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);
}
