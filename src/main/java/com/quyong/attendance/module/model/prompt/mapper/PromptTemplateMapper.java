package com.quyong.attendance.module.model.prompt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.model.prompt.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {

    @Select({
            "SELECT id, code, name, sceneType, version, content, status, remark, createTime, updateTime",
            "FROM promptTemplate",
            "WHERE sceneType = #{sceneType} AND status = 'ENABLED'",
            "ORDER BY updateTime DESC, id DESC",
            "LIMIT 1"
    })
    PromptTemplate selectEnabledBySceneType(@Param("sceneType") String sceneType);
}
