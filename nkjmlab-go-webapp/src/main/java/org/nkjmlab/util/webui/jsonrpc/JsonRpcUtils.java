
package org.nkjmlab.util.webui.jsonrpc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ClassUtils;
import org.apache.logging.log4j.LogManager;
import org.nkjmlab.util.io.IOStreamUtils;
import org.nkjmlab.util.json.JsonMapper;


public class JsonRpcUtils {
  private static final org.apache.logging.log4j.Logger log = LogManager.getLogger();

  private static final AtomicInteger id = new AtomicInteger();


  public static JsonRpcRequest createRequest(Method method, Object... args) {
    JsonRpcRequest req = new JsonRpcRequest();
    req.setId(Integer.toString(id.incrementAndGet()));
    req.setMethod(method.getName());
    req.setParams(args);
    return req;
  }



  public static JsonRpcResponse callJsonRpc(JsonMapper mapper, Object service,
      JsonRpcRequest jreq) {
    try {
      Object result = invokeMethod(service, jreq, mapper);
      JsonRpcResponse jres = toJsonRpcResponse(result, jreq);
      return jres;
    } catch (Exception e) {
      JsonRpcResponse jres = toJsonRpcErrorResponse(mapper.toJson(e), e, jreq);
      return jres;
    }
  }



  public static Object invokeMethod(Object service, JsonRpcRequest req, JsonMapper mapper) {
    Method method = findMethod(service, req);
    if (method == null) {
      String emsg = "method is invalid. "
          + (req.getMethod() != null ? "methodName=" + req.getMethod() : "no method name")
          + ", params=" + Arrays.deepToString(req.getParams());
      log.error(emsg);
      throw new RuntimeException(emsg);
    }
    try {
      Object result = invokeMethod(service, method, req.getParams(), mapper);
      return result;
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
      log.error("method={}, params={}", method, req.getParams());
      log.error(e1, e1);
      throw new RuntimeException(e1);
    }
  }

  private static Map<String, Method> methodTable = new ConcurrentHashMap<>();

  private static Method findMethod(Object service, JsonRpcRequest req) {
    int paramLength = req.getParams().length;

    String key =
        String.valueOf(service.getClass().getName() + "-" + req.getMethod() + "-" + paramLength);

    return methodTable.computeIfAbsent(key, k -> {
      for (Class<?> clz : service.getClass().getInterfaces()) {
        Method method = findMethod(clz, req.getMethod(), paramLength);
        if (method != null) {
          log.info("[{}] is binded to [{}]", key, method);
          return method;
        }
      }
      return null;
    });
  }

  public static Method findMethod(Class<?> clazz, String methodName, int paramCount) {
    for (Method m : clazz.getMethods()) {
      if (m.getName().equals(methodName)) {
        Class<?>[] t = m.getParameterTypes();
        if (t.length == paramCount) {
          return m;
        }
      }
    }
    return null;
  }

  public static JsonRpcRequest toJsonRpcRequest(JsonMapper mapper, HttpServletRequest req) {
    try {
      return toJsonRpcRequest(mapper, getInputStream(req));
    } catch (Throwable e) {
      log.error(e, e);
      throw new RuntimeException(e);
    }

  }

  public static JsonRpcRequest toJsonRpcRequest(JsonMapper mapper, InputStream is) {
    try {
      String str = IOStreamUtils.readAsString(is, StandardCharsets.UTF_8);
      JsonRpcRequest jreq = mapper.toObject(str, JsonRpcRequest.class);
      return jreq;
    } catch (Throwable e) {
      log.error(e, e);
      throw new RuntimeException(e);
    }
  }


  private static InputStream getInputStream(HttpServletRequest req) throws IOException {
    InputStream is = req.getInputStream();
    String contentEncoding = req.getHeader("Content-Encoding");

    if (contentEncoding == null) {
      return is;
    }
    if (contentEncoding.equals("deflate")) {
      return new InflaterInputStream(is);
    } else if (contentEncoding.equals("gzip")) {
      return new GZIPInputStream(is);
    } else {
      return is;
    }
  }



  public static JsonRpcResponse toJsonRpcResponse(Object result, JsonRpcRequest req) {
    JsonRpcResponse res = new JsonRpcResponse();
    res.setId(req.getId());
    res.setResult(result);
    return res;
  }

  private static JsonRpcResponse toJsonRpcErrorResponse(String faultString, Throwable t,
      JsonRpcRequest jreq) {
    JsonRpcResponse jres = new JsonRpcResponse();
    jres.setId(jreq.getId());
    jres.setError(JsonRpcError.createRpcFault("Server.userException", faultString, t));
    return jres;
  }



  private static Object invokeMethod(Object instance, Method method, Object[] params,
      JsonMapper mapper)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    // Type[] pTypes = method.getGenericParameterTypes();
    Class<?>[] pClasses = method.getParameterTypes();
    Object[] args = new Object[pClasses.length];
    for (int i = 0; i < args.length; i++) {
      Object param = params[i];
      Class<?> pClass = pClasses[i];
      if (param == null) {
        args[i] = null;
      } else if (ClassUtils.isAssignable(param.getClass(), pClass)) {
        args[i] = param;
      } else {
        if (String.class.equals(pClass) && (param instanceof Map || param instanceof List)) {
          args[i] = mapper.convertValue(param.toString(), String.class);
        } else {
          args[i] = mapper.convertValue(param, pClass);
        }
      }
    }

    return method.invoke(instance, args);
  }



  public static void setContentTypeToJson(HttpServletResponse res) {
    res.setContentType("application/json;charset=UTF-8");
  }

  public static void setAccessControlAllowOriginToWildCard(HttpServletResponse res) {
    res.setHeader("Access-Control-Allow-Origin", "*");
  }

  public static void setAccessControlAllowMethodsToWildCard(HttpServletResponse res) {
    res.setHeader("Access-Control-Allow-Methods", "*");

  }

  public static void setAccessControlAllowHeadersToWildCard(HttpServletResponse res) {
    res.setHeader("Access-Control-Allow-Headers", "*");

  }

}
