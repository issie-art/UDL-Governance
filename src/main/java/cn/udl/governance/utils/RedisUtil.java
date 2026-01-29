package cn.udl.governance.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setByString(String key, Object value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, time, timeUnit);
    }

    public void setBySet(String key, Object value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForSet().add(key, value);
        redisTemplate.expire(key, time, timeUnit);
    }

    public Object getByString(String key) {
        return redisTemplate.opsForValue().get(key);
    }
     public Set<Object> getBySet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Boolean expire(String key, long time, TimeUnit timeUnit) {
        return redisTemplate.expire(key, time, timeUnit);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}