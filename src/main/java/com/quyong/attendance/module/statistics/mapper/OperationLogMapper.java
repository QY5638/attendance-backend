package com.quyong.attendance.module.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.statistics.entity.OperationLog;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM operationLog ol",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countByQuery(@Param("userId") Long userId,
                      @Param("type") String type,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ol.id, ol.userId, ol.type, ol.content, ol.operationTime",
            "FROM operationLog ol",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "ORDER BY ol.operationTime DESC, ol.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<OperationLogVO> selectPageByQuery(@Param("userId") Long userId,
                                           @Param("type") String type,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT ol.id, ol.userId, ol.type, ol.content, ol.operationTime",
            "FROM operationLog ol",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "ORDER BY ol.operationTime DESC, ol.id DESC",
            "</script>"
    })
    List<OperationLogVO> selectAllByQuery(@Param("userId") Long userId,
                                          @Param("type") String type,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}
