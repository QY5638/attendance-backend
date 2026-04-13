package com.quyong.attendance.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.notification.entity.NotificationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM notificationRecord",
            "WHERE recipientUserId = #{recipientUserId}",
            "<if test='readStatus != null'> AND readStatus = #{readStatus}</if>",
            "<if test='category != null and category != \"\"'> AND category = #{category}</if>",
            "<if test='categories != null and categories.size() > 0'>",
            "AND category IN",
            "<foreach collection='categories' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</if>",
            "</script>"
    })
    long countByQuery(@Param("recipientUserId") Long recipientUserId,
                      @Param("readStatus") Integer readStatus,
                      @Param("category") String category,
                      @Param("categories") List<String> categories);

    @Select({
            "<script>",
            "SELECT id, recipientUserId, senderUserId, businessType, businessId, category, title, content, level, actionCode, readStatus, deadline, extraJson, createTime, readTime",
            "FROM notificationRecord",
            "WHERE recipientUserId = #{recipientUserId}",
            "<if test='readStatus != null'> AND readStatus = #{readStatus}</if>",
            "<if test='category != null and category != \"\"'> AND category = #{category}</if>",
            "<if test='categories != null and categories.size() > 0'>",
            "AND category IN",
            "<foreach collection='categories' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</if>",
            "ORDER BY CASE WHEN readStatus = 0 THEN 0 ELSE 1 END ASC,",
            "CASE WHEN deadline IS NULL THEN 1 ELSE 0 END ASC, deadline ASC, createTime DESC, id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<NotificationRecord> selectPageByQuery(@Param("recipientUserId") Long recipientUserId,
                                               @Param("readStatus") Integer readStatus,
                                               @Param("category") String category,
                                               @Param("categories") List<String> categories,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM notificationRecord WHERE recipientUserId = #{recipientUserId} AND readStatus = 0",
            "<if test='categories != null and categories.size() > 0'>",
            "AND category IN",
            "<foreach collection='categories' item='item' open='(' separator=',' close=')'>#{item}</foreach>",
            "</if>",
            "</script>"
    })
    long countUnread(@Param("recipientUserId") Long recipientUserId,
                     @Param("categories") List<String> categories);

    @Select("SELECT COUNT(*) FROM notificationRecord WHERE recipientUserId = #{recipientUserId} AND category = #{category} AND businessId = #{businessId} AND createTime >= #{since}")
    long countRecentByCategory(@Param("recipientUserId") Long recipientUserId,
                               @Param("category") String category,
                               @Param("businessId") Long businessId,
                               @Param("since") LocalDateTime since);
}
