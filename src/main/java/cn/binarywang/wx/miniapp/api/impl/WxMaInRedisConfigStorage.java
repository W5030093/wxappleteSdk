package cn.binarywang.wx.miniapp.api.impl;

import cn.binarywang.wx.miniapp.api.WxMaConfigStorage;
import cn.binarywang.wx.miniapp.api.WxMaInMemoryConfigStorage;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.WxMaInMemoryConfig;
import cn.binarywang.wx.miniapp.enums.TicketType;
import me.chanjar.weixin.common.bean.WxAccessToken;
import me.chanjar.weixin.common.util.http.apache.ApacheHttpClientBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.locks.Lock;

/**
 * 基于Redis的微信配置provider.
 *
 * <pre>
 *    使用说明：本实现仅供参考，并不完整，
 *    比如为减少项目依赖，未加入redis分布式锁的实现，如有需要请自行实现。
 * </pre>
 *
 * @author nickwong
 */
@SuppressWarnings("hiding")
public class WxMaInRedisConfigStorage implements WxMaConfigStorage, Serializable {

  private static final String ACCESS_TOKEN_KEY = "wx:access_token:";

  private static final int expiresInSeconds=7200;
  /**
   * 使用连接池保证线程安全.
   */
  protected final JedisPool jedisPool;

  private String accessTokenKey="wx:access_token:";

  private WxMaConfig wxMaConfig;

  public WxMaInRedisConfigStorage(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }


  @Override
  public String getAccessToken() {
    try (Jedis jedis = this.jedisPool.getResource()) {
      return jedis.get(this.accessTokenKey+wxMaConfig.getAppid());
    }
  }

  @Override
  public Lock getAccessTokenLock() {
    return null;
  }

  @Override
  public boolean isAccessTokenExpired() {
    try (Jedis jedis = this.jedisPool.getResource()) {
      return jedis.ttl(accessTokenKey+wxMaConfig.getAppid()) < 2;
    }
  }

  @Override
  public void expireAccessToken() {
    try (Jedis jedis = this.jedisPool.getResource()) {
      jedis.expire(this.accessTokenKey+wxMaConfig.getAppid(), 0);
    }
  }

  @Override
  public void updateAccessToken(WxAccessToken accessToken) {
    updateAccessToken(accessToken.getAccessToken(),accessToken.getExpiresIn());
  }

  @Override
  public void updateAccessToken(String accessToken, int expiresInSeconds) {
    try (Jedis jedis = this.jedisPool.getResource()) {
      jedis.setex(this.accessTokenKey+wxMaConfig.getAppid(), expiresInSeconds - 200, accessToken);
    }
  }

  @Override
  public String getTicket(TicketType type) {
    return null;
  }

  @Override
  public Lock getTicketLock(TicketType type) {
    return null;
  }

  @Override
  public boolean isTicketExpired(TicketType type) {
    return false;
  }

  @Override
  public void expireTicket(TicketType type) {

  }

  @Override
  public void updateTicket(TicketType type, String ticket, int expiresInSeconds) {

  }

  @Override
  public String getAppId() {
    return null;
  }

  @Override
  public String getSecret() {
    return null;
  }

  @Override
  public String getToken() {
    return null;
  }

  @Override
  public String getAesKey() {
    return null;
  }

  @Override
  public String getTemplateId() {
    return null;
  }

  @Override
  public long getExpiresTime() {
    return 0;
  }

  @Override
  public String getOauth2redirectUri() {
    return null;
  }

  @Override
  public String getHttpProxyHost() {
    return null;
  }

  @Override
  public int getHttpProxyPort() {
    return 0;
  }

  @Override
  public String getHttpProxyUsername() {
    return null;
  }

  @Override
  public String getHttpProxyPassword() {
    return null;
  }

  @Override
  public File getTmpDirFile() {
    return null;
  }

  @Override
  public ApacheHttpClientBuilder getApacheHttpClientBuilder() {
    return null;
  }

  @Override
  public boolean autoRefreshToken() {
    return true;
  }

  @Override
  public WxMaConfig getWxMaConfig() {
    return wxMaConfig;
  }

  @Override
  public void setWxMaConfig(WxMaConfig wxMaConfig) {
    this.wxMaConfig = wxMaConfig;
  }
}
