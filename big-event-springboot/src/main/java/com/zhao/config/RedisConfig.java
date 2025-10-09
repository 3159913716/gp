package com.zhao.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * Redis配置类
 * 这个类的作用是配置Redis模板，让我们能够更方便地在Java代码中操作Redis数据库
 */
@Configuration
public class RedisConfig {


    /**
     * 创建RedisTemplate对象
     * RedisTemplate就像是Java程序和Redis数据库之间的"翻译官"
     * 它负责把Java对象转换成Redis能理解的格式，也把Redis的数据转换成Java对象
     *
     * @param connectionFactory Redis连接工厂，Spring会自动给我们提供这个参数
     * @return 配置好的RedisTemplate对象
     */
    @Bean // 告诉Spring这个方法会创建一个对象，并交给Spring容器管理
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        // 创建一个新的RedisTemplate实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂，相当于告诉RedisTemplate如何连接到Redis数据库
        template.setConnectionFactory(connectionFactory);

        // 设置key的序列化方式（序列化就是把对象转换成字节流的过程）
        // 使用StringRedisSerializer意味着Redis中的key会以普通字符串的形式存储
        template.setKeySerializer(new StringRedisSerializer());
        // 设置hash数据结构的key的序列化方式，同样使用字符串序列化
        template.setHashKeySerializer(new StringRedisSerializer());

        // 设置value的序列化方式
        // 使用GenericJackson2JsonRedisSerializer意味着value会以JSON格式存储
        // 这样做的好处是：我们可以在Redis中直接看到可读的JSON数据，而且能存储复杂的Java对象
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 设置hash数据结构的value的序列化方式，同样使用JSON序列化
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 完成所有设置后，初始化模板
        template.afterPropertiesSet();
        // 返回配置好的RedisTemplate，Spring会把它加入到容器中供其他地方使用
        return template;
    }
}