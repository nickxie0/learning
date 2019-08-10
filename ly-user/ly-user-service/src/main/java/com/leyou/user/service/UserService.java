package com.leyou.user.service;

import com.leyou.common.constants.MQConstants;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.utils.RegexUtils;
import com.leyou.user.dto.UserDTO;
import com.leyou.user.entity.User;
import com.leyou.user.mapper.UserMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "verify:code:phone";
    /**
     * 校验数据的唯一性
     *
     * @param data
     * @param type
     * @return
     */
    public Boolean checkData(String data, Integer type) {

        User user = new User();
        //确定type的值 为1 ：用户名 2：手机号
        switch (type) {
            case 1:   //1 ：用户名
                user.setUsername(data);
                break;
            case 2:   //2：手机号
                user.setPhone(data);
                break;
            default: //否则参数有误，抛出异常
                throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        //判断数据是否唯一
        int count = userMapper.selectCount(user);

        return count == 0;

    }

    /**
     * 发送验证码
     *
     * @param phone
     */
    public void sendCode(String phone) {
        //接受并验证手机号
        if (!RegexUtils.isPhone(phone)) {
            throw new LyException(ExceptionEnum.INVALID_PHONE_NUMBER);
        }
        //随机生成验证码
        String code = RandomStringUtils.randomNumeric(6);
        //利用MQ把消息发送给ly-sms
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        amqpTemplate.convertAndSend(MQConstants.Exchange.SMS_EXCHANGE_NAME,
                MQConstants.RoutingKey.VERIFY_CODE_KEY, msg);
        //保存验证码到redis
        redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);

    }

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 注册
     *
     * @param user
     * @param code
     */
    @Transactional
    public void register(User user, String code) {
        //验证短信验证码
        String key = KEY_PREFIX + user.getPhone();
        //获取存储在redis中的验证码
        String cacheCode = redisTemplate.opsForValue().get(key);
        if (!code.equals(cacheCode)) {
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
        //校验用户数据

        //对密码进行加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //写入数据库中
        int count = userMapper.insertSelective(user);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    /**
     * 根据用户名和密码查询用户
     *
     * @param username
     * @param password
     * @return
     */
    public UserDTO queryUserByUserNameAndPassword(String username, String password) {

        //根据用户名查询
        User record = new User();
        record.setUsername(username);
//        record.setPassword(passwordEncoder.encode(password));
        //查询用户
        User user = userMapper.selectOne(record);
        if (user == null) {
            //用户名错误
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        //校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        return BeanHelper.copyProperties(user, UserDTO.class);
    }
}
