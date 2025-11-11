package com.novelhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novelhub.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 更新最后登录信息
     *
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @return 影响行数
     */
    @Update("UPDATE tb_user SET last_login_time = NOW(), last_login_ip = #{loginIp} WHERE id = #{userId}")
    int updateLastLogin(@Param("userId") Long userId, @Param("loginIp") String loginIp);
}

