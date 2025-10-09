package com.zhao;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


//单元测试,生成jwt令牌,验证jwt令牌
public class JwtTest {

//    @Test
//    public void testGen() {
//        Map<String, Object> claims = new HashMap<String, Object>();
//        claims.put("id", "1");
//        claims.put("username", "赵四");
//        //生成jwt的代码
//        String token = JWT.create()
//                .withClaim("user", claims)//添加载荷
//                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))//添加过期时间
//                .sign(Algorithm.HMAC256("zhaoshuyang"));//指定算法,配置密钥
//        System.out.println(token);
//
//    }

    @Test
    public void testParseToken() throws Exception {
//        //定义字符串,模拟用户传递过来的token
//        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGFpbXMiOnsiaWQiOjMsInVzZXJuYW1lIjoid2FuZ2JhZGFuIn0sImV4cCI6MTc0NTk4MDA0N30.AOVmwWzuikdeJd36urynuPY6BgcNHgnwgljiXU9EQnA";
//        //生成验证器
//        JWTVerifier jwtVerify = JWT.require(Algorithm.HMAC256("zhaoshuyang")).build();
//        //调用验证器,验证token,生成一个解析后的JWT对象
//        DecodedJWT decodedJWT = jwtVerify.verify(token);
//        Map<String, Claim> map = decodedJWT.getClaims();
//        System.out.println(map.get("claims"));

        Algorithm algorithm = Algorithm.HMAC256("zhaoshuyang");
        String token = JWT.create()
                .withSubject("test-user")
                .withClaim("id", 3)
                .withClaim("username", "wangbadan")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000)) // 1小时后过期
                .sign(algorithm);

        // 验证令牌
        JWTVerifier verifier = JWT.require(algorithm).build();
        assertNotNull(verifier.verify(token)); // 验证成功则不会抛出异常
    }
        //验证失败的情况
        //如果篡改了头部和载荷部分的数据,那么验证失败
        //如果密钥改了,验证是失败的
        //token过期了,也验证失败

    @Test
    public void testParseExpiredToken() {
        // 生成一个已过期的令牌（1小时前）
        Algorithm algorithm = Algorithm.HMAC256("zhaoshuyang");
        String expiredToken = JWT.create()
                .withSubject("test-user")
                .withClaim("id", 3)
                .withClaim("username", "wangbadan")
                .withExpiresAt(new Date(System.currentTimeMillis() - 3600_000)) // 1小时前
                .sign(algorithm);

        // 断言验证过期令牌时抛出 TokenExpiredException
        assertThrows(TokenExpiredException.class, () -> {
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(expiredToken);
        });
    }

    }




