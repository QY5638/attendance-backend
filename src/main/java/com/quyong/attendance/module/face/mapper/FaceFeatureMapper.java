package com.quyong.attendance.module.face.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quyong.attendance.module.face.entity.FaceFeature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FaceFeatureMapper extends BaseMapper<FaceFeature> {

    @Select("SELECT id, userId, featureData, featureHash, encryptFlag, createTime "
            + "FROM faceFeature "
            + "WHERE userId = #{userId} "
            + "ORDER BY createTime DESC, id DESC "
            + "LIMIT 1")
    FaceFeature selectLatestByUserId(@Param("userId") Long userId);
}
