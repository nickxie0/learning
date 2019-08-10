package com.leyou.common.auth;

import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.RsaUtils;
import com.leyou.common.utils.JsonUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.UUID;

public class RsaUtilsTest {

    /**
     * 公钥地址
     */
    private String publicKeyFilename = "D:\\IntelliJ IDEA WorkSpace\\ssh\\id_rsa.pub";

    /**
     * 私钥地址
     */
    private String privateFilename = "D:\\IntelliJ IDEA WorkSpace\\ssh\\id_rsa";


    @Test
    public void generateKey() throws Exception {
        //RSA算法运算的种子
        String secret = "Helloworld";
        RsaUtils.generateKey(publicKeyFilename, privateFilename, secret, 2048);
    }

    @Test
    public void readKey() throws Exception {
        PublicKey publicKey = RsaUtils.getPublicKey(publicKeyFilename);
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateFilename);
    }

    @Test
    public void testJwt() throws Exception {
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateFilename);

        //创建载荷中的用户信息
        UserInfo userInfo = new UserInfo(1L, "Jack", "guest");
        //生成token
        String jwt = generateToken(userInfo, privateKey, 5);
        System.out.println("jws = " + jwt);
    }

    public String generateToken(Object userInfo, PrivateKey key, int expireMinutes) {
        return Jwts.builder()
                .setExpiration(new Date(System.currentTimeMillis() + expireMinutes*60*1000))
                .setId(UUID.randomUUID().toString())
                .claim("user", JsonUtils.toString(userInfo))
                .signWith(key, SignatureAlgorithm.RS256)
                .compact();
    }
}