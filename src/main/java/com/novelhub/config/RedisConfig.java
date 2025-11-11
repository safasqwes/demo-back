package com.novelhub.config;

import com.alibaba.fastjson2.JSON;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类 - 使用Fastjson2进行序列化
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置key序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置value序列化方式 - 使用Fastjson2
        template.setValueSerializer(new FastJsonRedisSerializer<>(Object.class));
        template.setHashValueSerializer(new FastJsonRedisSerializer<>(Object.class));
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Fastjson2 Redis序列化器
     */
    public static class FastJsonRedisSerializer<T> implements RedisSerializer<T> {
        
        private final Class<T> clazz;
        
        public FastJsonRedisSerializer(Class<T> clazz) {
            this.clazz = clazz;
        }
        
        @Override
        public byte[] serialize(T t) {
            if (t == null) {
                return new byte[0];
            }
            return JSON.toJSONString(t).getBytes();
        }
        
        @Override
        public T deserialize(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            String json = new String(bytes);
            return JSON.parseObject(json, clazz);
        }
    }
}
