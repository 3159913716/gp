package com.zhao.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    /**
     * 对明文密码进行加密
     * @param plainPassword 明文密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * 验证输入的明文密码与加密后的密码是否匹配
     * @param plainPassword 明文密码
     * @param hashedPassword 加密后的密码
     * @return 如果匹配返回 true，否则返回 false
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

//    public static void main(String[] args) {
//        String plainPassword = "user1234";
//        String hashedPassword = encryptPassword(plainPassword);
//        System.out.println("明文密码: " + plainPassword);
//        System.out.println("加密后的密码: " + hashedPassword);
//
//        boolean isMatch = verifyPassword(plainPassword, hashedPassword);
//        System.out.println("密码是否匹配: " + isMatch);
//    }


}


