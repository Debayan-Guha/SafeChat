package com.safechat.userservice.utility;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UrlEncoderUtil {
    
    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    
    public static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}