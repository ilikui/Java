package info.likui.CH001;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;


public class SignatureDemo {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        //提供的accessKey和secretKey
        String accessKey = "Qe6zq0hFgDQmmu6AdbjL";
        String secretKey = "K3H3DcA1OK6ncq9cULsvugqchcD4gzPA";
        //epoch时间戳（毫秒数）
        long timestamp = 1660652107554L;
        //API ID
        String apiId = "bwsQnTb0";
        //待签名原文（HEX编码字母小写）
        String originalText = "8d969eef6ecaecad3c29a3a629280e6cf0c3f5d5a86aff3ca1202c923adc6c92";
        //无key时使用自有协议签名，key=CCC时支持CCC蓝牙钥匙签名接口
        //String key = "CCC";

        //将参数放到treeMap中
        TreeMap<String, Object> treeMap = new TreeMap<>();
        treeMap.put("apiId", apiId);
        treeMap.put("originalText", originalText);
        //key为空时不用填写
        //treeMap.put("key", key);
        treeMap.put("timestamp", timestamp);
        //将参数按照 accessKey + apiId + key + originalText + timestamp + secretKey顺序排列
        StringBuilder sb = new StringBuilder();
        sb.append(accessKey);


//        treeMap.forEach((k, v) -> {
//            if (v != null) {
//                sb.append(v);
//            }
//        });
        sb.append(secretKey);

        //SHA256哈希算法
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(sb.toString().getBytes(StandardCharsets.UTF_8));
        byte[] digest = messageDigest.digest();
        //HEX编码（小写）
        String signature = new BigInteger(1, digest).toString(16);
        System.out.println(signature);
//key为null ：eae480618e6621eba5f2d74a8dbd29aa8856606152e2c8a05bf691a588111e98
//key为CCC  ：356528cbfe9fb6995e6e4106f8244ffa08ca5aec28ff2806116e9a5bf236414b
    }
}
