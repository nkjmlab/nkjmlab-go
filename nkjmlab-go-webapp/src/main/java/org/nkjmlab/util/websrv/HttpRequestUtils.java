package org.nkjmlab.util.websrv;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestUtils {

  public static Optional<String> getXForwardedFor(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader("X-Forwarded-For"));
  }


}
