package com.hyz.douyin.basic.service;

import com.hyz.douyin.basic.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hyz.douyin.basic.model.vo.UserRegisterVO;

/**
* @author heguande
* @description 针对表【tb_user(用户表)】的数据库操作Service
* @createDate 2023-07-25 12:43:16
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @return {@link UserRegisterVO}
     */
    UserRegisterVO userRegister(String username, String password);

}
