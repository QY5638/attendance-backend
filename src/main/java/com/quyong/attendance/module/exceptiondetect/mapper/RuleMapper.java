package com.quyong.attendance.module.exceptiondetect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.exceptiondetect.entity.Rule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RuleMapper extends BaseMapper<Rule> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM rule",
            "WHERE 1 = 1",
            "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test='status != null'> AND status = #{status}</if>",
            "</script>"
    })
    long countByQuery(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select({
            "<script>",
            "SELECT id, name, startTime, endTime, lateThreshold, earlyThreshold, repeatLimit, status",
            "FROM rule",
            "WHERE 1 = 1",
            "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test='status != null'> AND status = #{status}</if>",
            "ORDER BY id ASC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Rule> selectPageByQuery(@Param("keyword") String keyword,
                                 @Param("status") Integer status,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);
}
