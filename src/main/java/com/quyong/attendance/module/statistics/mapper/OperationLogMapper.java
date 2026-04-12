package com.quyong.attendance.module.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.statistics.entity.OperationLog;
import com.quyong.attendance.module.statistics.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM operationLog ol",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='types != null and types.size > 0'> AND ol.type IN <foreach collection='types' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "</script>"
    })
    long countByQuery(@Param("userId") Long userId,
                      @Param("type") String type,
                      @Param("types") List<String> types,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ol.id, ol.userId, u.username, u.realName, ol.type, ol.content, ol.operationTime",
            "FROM operationLog ol LEFT JOIN `user` u ON ol.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='types != null and types.size > 0'> AND ol.type IN <foreach collection='types' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "ORDER BY ol.operationTime DESC, ol.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<OperationLogVO> selectPageByQuery(@Param("userId") Long userId,
                                           @Param("type") String type,
                                           @Param("types") List<String> types,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT ol.id, ol.userId, u.username, u.realName, ol.type, ol.content, ol.operationTime",
            "FROM operationLog ol LEFT JOIN `user` u ON ol.userId = u.id",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='types != null and types.size > 0'> AND ol.type IN <foreach collection='types' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "ORDER BY ol.operationTime DESC, ol.id DESC",
            "</script>"
    })
    List<OperationLogVO> selectAllByQuery(@Param("userId") Long userId,
                                          @Param("type") String type,
                                          @Param("types") List<String> types,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT ol.type AS label, COUNT(*) AS total",
            "FROM operationLog ol",
            "WHERE 1 = 1",
            "<if test='userId != null'> AND ol.userId = #{userId}</if>",
            "<if test='type != null and type != \"\"'> AND ol.type = #{type}</if>",
            "<if test='types != null and types.size > 0'> AND ol.type IN <foreach collection='types' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>",
            "<if test='startTime != null'> AND ol.operationTime &gt;= #{startTime}</if>",
            "<if test='endTime != null'> AND ol.operationTime &lt;= #{endTime}</if>",
            "GROUP BY ol.type",
            "ORDER BY total DESC, ol.type ASC",
            "</script>"
    })
    List<Map<String, Object>> selectTypeSummaryByQuery(@Param("userId") Long userId,
                                                       @Param("type") String type,
                                                       @Param("types") List<String> types,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);
}
