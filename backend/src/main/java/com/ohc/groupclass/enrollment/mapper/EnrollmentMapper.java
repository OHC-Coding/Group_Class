package com.ohc.groupclass.enrollment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohc.groupclass.enrollment.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EnrollmentMapper extends BaseMapper<Enrollment> {

    @Select("SELECT COUNT(*) FROM enrollment "
            + "WHERE class_id = #{classId} AND status IN ('PENDING_PAYMENT', 'CONFIRMED')")
    long countOccupied(@Param("classId") Long classId);

    @Select("SELECT member_id FROM enrollment "
            + "WHERE class_id = #{classId} AND status IN ('PENDING_PAYMENT', 'CONFIRMED')")
    List<Long> selectOccupiedMemberIds(@Param("classId") Long classId);
}
