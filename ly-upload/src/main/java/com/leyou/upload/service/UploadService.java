package com.leyou.upload.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.upload.config.OSSProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class UploadService {
    private static final String IMAGE_DIR = "D:\\java\\nginx-1.14.0\\html\\images";
    private static final String IMAGE_URL = "http://image.leyou.com/images/";
    private static final List<String> ALLOW_IMAGE_TYPE = Arrays.asList("image/jpeg","image/png","image/bmp");

    public String uploadImage(MultipartFile file) {
        //文件的校验
        //校验后缀名
        String contentType = file.getContentType();
        if (!ALLOW_IMAGE_TYPE.contains(contentType)) {
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }
        //内容校验
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }

        //拿到文件的原始名称
        String filename = file.getOriginalFilename();
        //获取文件的后缀名
        String extension = StringUtils.substringAfterLast(filename, ".");
        String uuid = UUID.randomUUID().toString();
        //随机生成文件名
        filename = uuid + "." + extension;
        File imagePath = new File(IMAGE_DIR, filename);

        //保存文件
        try {
            file.transferTo(imagePath);
        } catch (IOException e) {
            log.error("文件上传失败！原因：{}", e.getMessage(), e);
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR, e);
        }

        return IMAGE_URL + filename;
    }

    @Autowired
    private OSS ossClient;

    @Autowired
    private OSSProperties prop;

    public Map<String,Object> getSignature() {

        try {
            //上传文件的限制策略
            long expireTime = prop.getExpireTime();
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, prop.getMaxFileSize());
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, prop.getDir());

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            Map<String, Object> respMap = new LinkedHashMap<>();
            respMap.put("accessId", prop.getAccessKeyId());
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", prop.getDir());
            respMap.put("host", prop.getHost());
            respMap.put("expire", expireEndTime);
            return respMap;
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }
    }
}
