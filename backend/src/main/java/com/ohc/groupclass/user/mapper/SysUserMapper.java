package com.ohc.groupclass.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ohc.groupclass.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM sys_user WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);
}
