package com.axonivy.connector.metaproc.auth;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;

import com.axonivy.connector.metaproc.login.LoginData;
import com.axonivy.connector.metaproc.login.LoginUser;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.environment.Ivy;

public class AuthTokenFeature implements Feature {

  private static final String TOKEN_HEADER = "token-8080";
  private static final String LOGIN_PATH = "/metarest/user/login";

  public static String TOKEN;
  public static long TOKEN_EXPIRED_TIME = 0;

  @Override
  public boolean configure(FeatureContext context) {
    context.register(new AuthTokenFilter());
    return false;
  }

  public class AuthTokenFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext context) throws IOException {
      if (context.getUri().getPath().contains(LOGIN_PATH)) {
        // already in auth request
        return;
      }
      if (TOKEN != null && TOKEN_EXPIRED_TIME > System.currentTimeMillis()) {
        context.getHeaders().add(TOKEN_HEADER, TOKEN);
        return;
      }
      var login = new LoginUser();
      login.setUsername(Ivy.var().get("metaproc.Username"));
      login.setPassword(Ivy.var().get("metaproc.Password"));
      var data = new LoginData();
      data.setLogin(login);
      var entity = Entity.entity(data, MediaType.APPLICATION_JSON);
      Ivy.log().info("Create new token for " + login.getUsername());
      try (var response = context.getClient()
              .target(Ivy.var().get("metaproc.Url") + LOGIN_PATH).request()
              .accept(MediaType.APPLICATION_JSON).post(entity)) {
        var node = response.readEntity(ObjectNode.class);
        TOKEN = node.get("loginResult").get("token").asText();
        TOKEN_EXPIRED_TIME = System.currentTimeMillis() + Long.valueOf(Ivy.var().get("metaproc.TokenExpiredDuration"));
        context.getHeaders().add(TOKEN_HEADER, TOKEN);
      } catch (Exception e) {
        Ivy.log().error(e.getMessage());
      }
    }
  }
}
