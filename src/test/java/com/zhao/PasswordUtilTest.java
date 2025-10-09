package com.zhao;


import org.junit.jupiter.api.Test;

import static com.zhao.utils.PasswordUtil.encryptPassword;
import static com.zhao.utils.PasswordUtil.verifyPassword;

/**
 * 验证 PasswordUtil（密码加密类）类是否有用
 */
public class PasswordUtilTest {


    @Test
    public  void passwordUtil() {
        String plainPassword = "user1234";
        String hashedPassword = encryptPassword(plainPassword);
        System.out.println("明文密码: " + plainPassword);
        System.out.println("加密后的密码: " + hashedPassword);

        boolean isMatch = verifyPassword(plainPassword, hashedPassword);
        System.out.println("密码是否匹配: " + isMatch);
    }
}
