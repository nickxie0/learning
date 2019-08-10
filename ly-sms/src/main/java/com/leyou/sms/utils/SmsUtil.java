package com.leyou.sms.utils;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.constants.SmsConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SmsUtil {
    @Autowired
    private SmsProperties prop;

    @Autowired
    private IAcsClient client;

    public void sendMessage(String phone, String signName, String template, String param) {
        CommonRequest request = new CommonRequest();
        request.setProtocol(ProtocolType.HTTPS);
        request.setMethod(MethodType.POST);
        request.setDomain(prop.getDomain());
        request.setVersion(prop.getVersion());
        request.setAction(prop.getAction());
        request.putQueryParameter(SmsConstants.SMS_PARAM_KEY_PHONE, phone);
        request.putQueryParameter(SmsConstants.SMS_PARAM_KEY_SIGN_NAME, signName);
        request.putQueryParameter(SmsConstants.SMS_PARAM_KEY_TEMPLATE_CODE, template);
        request.putQueryParameter(SmsConstants.SMS_PARAM_KEY_TEMPLATE_PARAM, param);

        try {
            CommonResponse response = client.getCommonResponse(request);
            if (response.getHttpStatus() >= 300) {
                log.error("【SMS服务】发送短信失败。响应信息：{}", response.getData());
            }
            // 获取响应体
            Map<String, String> resp = JsonUtils.toMap(response.getData(), String.class, String.class);
            // 判断是否是成功
            if (!StringUtils.equals(SmsConstants.OK, resp.get(SmsConstants.SMS_RESPONSE_KEY_CODE))) {
                // 不成功，
                log.error("【SMS服务】发送短信失败，原因{}", resp.get(SmsConstants.SMS_RESPONSE_KEY_MESSAGE));
                throw new LyException(ExceptionEnum.SEND_MESSAGE_ERROR);
            }
            log.info("【SMS服务】发送短信成功，手机号：{}, 响应：{}", phone, response.getData());
        } catch (ServerException e) {
            log.error("【SMS服务】发送短信失败，服务端异常。", e);
            throw new LyException(ExceptionEnum.SEND_MESSAGE_ERROR);
        } catch (ClientException e) {
            log.error("【SMS服务】发送短信失败，客户端异常。", e);
            throw new LyException(ExceptionEnum.SEND_MESSAGE_ERROR);
        }
    }
}
