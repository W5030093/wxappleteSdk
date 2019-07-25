# wxappleteSdk
原文地址:https://github.com/wechat-group/WxJava
引用了他的小程序sdk，发现对于access_token的保存没有持久化到缓存只能中，进行了扩展

使用方法:


package com.wt.park.bus.config;

import cn.binarywang.wx.miniapp.api.WxMaConfigStorage;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaInRedisConfigStorage;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.bean.WxMaKefuMessage;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateData;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import cn.binarywang.wx.miniapp.config.WxMaInRedisConfig;
import cn.binarywang.wx.miniapp.message.WxMaMessageHandler;
import cn.binarywang.wx.miniapp.message.WxMaMessageRouter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.chanjar.weixin.common.bean.result.WxMediaUploadResult;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import redis.clients.jedis.JedisPool;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@Configuration
public class WxMaConfiguration {


    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private WxMaProperties properties;

    private static Map<String, WxMaMessageRouter> routers = Maps.newHashMap();
    private static Map<String, WxMaService> maServices = Maps.newHashMap();

    public static Map<String, WxMaMessageRouter> getRouters() {
        return routers;
    }


    public static WxMaService getMaService(String appid) {
        WxMaService wxService = maServices.get(appid);
        if (wxService == null) {
            throw new IllegalArgumentException(String.format("未找到对应appid=[%s]的配置，请核实！", appid));
        }

        return wxService;
    }

    @PostConstruct
    public void init() {
        List<WxMaProperties.Config> configs = this.properties.getConfigs();
        if (configs == null) {
            throw new RuntimeException("大哥，拜托先看下项目首页的说明（readme文件），添加下相关配置，注意别配错了！");
        }

        maServices = configs.stream()
                .map(a -> {
                    WxMaConfigStorage wxMaConfigStorage = new WxMaInRedisConfigStorage(jedisPool);
                    WxMaInRedisConfig config = new WxMaInRedisConfig();
                    config.setAppid(a.getAppid());
                    config.setSecret(a.getSecret());
                    config.setAesKey(a.getAesKey());
                    config.setMsgDataFormat(a.getMsgDataFormat());
                    config.setWxMaConfigStorage(wxMaConfigStorage);
                    wxMaConfigStorage.setWxMaConfig(config);
                    config.setWxMaConfigStorage(wxMaConfigStorage);
                    WxMaService service = new WxMaServiceImpl();
                    service.setWxMaConfig(config);
                    routers.put(a.getAppid(), this.newRouter(service));
                    return service;
                }).collect(Collectors.toMap(s -> s.getWxMaConfig().getAppid(), a -> a));
    }

    private WxMaMessageRouter newRouter(WxMaService service) {
        final WxMaMessageRouter router = new WxMaMessageRouter(service);
        router
            .rule().handler(logHandler).next()
            .rule().async(false).content("模板").handler(templateMsgHandler).end()
            .rule().async(false).content("文本").handler(textHandler).end()
            .rule().async(false).content("图片").handler(picHandler).end()
            .rule().async(false).content("二维码").handler(qrcodeHandler).end();
        return router;
    }

    private final WxMaMessageHandler templateMsgHandler = (wxMessage, context, service, sessionManager) ->
        service.getMsgService().sendTemplateMsg(WxMaTemplateMessage.builder()
            .templateId("此处更换为自己的模板id")
            .formId("自己替换可用的formid")
            .data(Lists.newArrayList(
                new WxMaTemplateData("keyword1", "339208499", "#173177")))
            .toUser(wxMessage.getFromUser())
            .build());

    private final WxMaMessageHandler logHandler = (wxMessage, context, service, sessionManager) -> {
        System.out.println("收到消息：" + wxMessage.toString());
        service.getMsgService().sendKefuMsg(WxMaKefuMessage.newTextBuilder().content("收到信息为：" + wxMessage.toJson())
            .toUser(wxMessage.getFromUser()).build());
    };

    private final WxMaMessageHandler textHandler = (wxMessage, context, service, sessionManager) ->
        service.getMsgService().sendKefuMsg(WxMaKefuMessage.newTextBuilder().content("回复文本消息")
            .toUser(wxMessage.getFromUser()).build());

    private final WxMaMessageHandler picHandler = (wxMessage, context, service, sessionManager) -> {
        try {
            WxMediaUploadResult uploadResult = service.getMediaService()
                .uploadMedia("image", "png",
                    ClassLoader.getSystemResourceAsStream("tmp.png"));
            service.getMsgService().sendKefuMsg(
                WxMaKefuMessage
                    .newImageBuilder()
                    .mediaId(uploadResult.getMediaId())
                    .toUser(wxMessage.getFromUser())
                    .build());
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
    };

    private final WxMaMessageHandler qrcodeHandler = (wxMessage, context, service, sessionManager) -> {
        try {
            final File file = service.getQrcodeService().createQrcode("123", 430);
            WxMediaUploadResult uploadResult = service.getMediaService().uploadMedia("image", file);
            service.getMsgService().sendKefuMsg(
                WxMaKefuMessage
                    .newImageBuilder()
                    .mediaId(uploadResult.getMediaId())
                    .toUser(wxMessage.getFromUser())
                    .build());
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
    };

}










package com.wt.park.bus.config;

import cn.binarywang.wx.miniapp.api.WxMaConfigStorage;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class WxMaModelConfig {

    @Autowired
    private ApplicationContextHelper applicationContextHelper;


    @Value("${APPID}")
    private String APPID;

    @Value("${APPLETES_SECRET}")
    private String APPLETES_SECRET;

    @Bean
    public WxMaProperties getWxMaProperties(){
        WxMaProperties wxMaProperties = WxMaProperties.builder().configs(
                Lists.newArrayList(WxMaProperties.Config.builder().appid(APPID).secret(APPLETES_SECRET).build())).build();

        return wxMaProperties;
    }
}




package com.wt.park.bus.config;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */

@Data
@Builder
public class WxMaProperties {

    private List<Config> configs;

    @Data
    @Builder
    public static class Config {
        /**
         * 设置微信小程序的appid
         */
        private String appid;

        /**
         * 设置微信小程序的Secret
         */
        private String secret;

        /**
         * 设置微信小程序消息服务器配置的token
         */
        private String token;

        /**
         * 设置微信小程序消息服务器配置的EncodingAESKey
         */
        private String aesKey;

        /**
         * 消息格式，XML或者JSON
         */
        private String msgDataFormat;
    }

}



