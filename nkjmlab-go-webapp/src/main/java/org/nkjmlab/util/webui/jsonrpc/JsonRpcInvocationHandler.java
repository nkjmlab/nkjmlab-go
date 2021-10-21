package org.nkjmlab.util.webui.jsonrpc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import org.apache.logging.log4j.LogManager;
import org.nkjmlab.util.io.IOStreamUtils;
import org.nkjmlab.util.json.JsonMapper;

class JsonRpcInvocationHandler implements InvocationHandler {
  private static final org.apache.logging.log4j.Logger log = LogManager.getLogger();

  private final JsonMapper mapper;
  private final URL url;

  public JsonRpcInvocationHandler(URL url, JsonMapper mapper) {
    this.url = url;
    this.mapper = mapper;
  }


  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    try {
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      try {
        con.setUseCaches(false);
        con.setDoOutput(true);
        writeRequest(con, method, args);
        int sts = con.getResponseCode();
        if (sts == 404) {
          throw new FileNotFoundException(sts + " " + url.toString());
        } else if (sts == 405) {
          throw new RuntimeException(sts + " Method Not Allowed " + url);
        } else if (sts == 500) {
          throw new RuntimeException(sts + " Internal Server Error: " + url);
        }
        // return readResponse(con, method.getGenericReturnType());
        Object result = readResponse(con, method.getReturnType());
        if (result != null) {
          log.error("{},{},{}", url, method, args);
        }

        return result;
      } finally {
        con.disconnect();
      }
    } catch (Exception e) {
      log.error(e, e);
      throw new RuntimeException(e);
    }
  }



  private void writeRequest(HttpURLConnection con, final Method method, final Object[] args) {
    try {
      con.setRequestProperty("Accept", "application/json-rpc");
      con.setRequestProperty("Content-type", "application/json-rpc");
      OutputStream os = con.getOutputStream();
      mapper.toJsonAndWrite(JsonRpcUtils.createRequest(method, args), os, false);
      os.flush();
    } catch (Exception e) {
      log.error(e, e);
      throw new RuntimeException(e);
    }
  }

  private Object readResponse(HttpURLConnection con, Class<?> returnType) {
    String str = "";
    try {
      InputStream is = getResponseStream(con);
      str = IOStreamUtils.readAsString(is, StandardCharsets.UTF_8);
      JsonRpcResponse ret = mapper.toObject(str, JsonRpcResponse.class);
      Object result = mapper.convertValue(ret.getResult(), returnType);
      return result;
    } catch (Exception e) {
      System.out.println(str);
      throw new RuntimeException("Parse error for URL:[" + con.getURL() + "],", e);
    }
  }

  private static InputStream getResponseStream(HttpURLConnection con) throws IOException {
    InputStream is = null;
    try {
      is = con.getInputStream();
    } catch (IOException e) {
      is = con.getErrorStream();
    }
    if (is != null) {
      String ce = con.getContentEncoding();
      if (ce != null) {
        if (ce.equals("gzip")) {
          is = new GZIPInputStream(is);
        } else if (ce.equals("deflate")) {
          is = new DeflaterInputStream(is);
        }
      }
    }
    return is;
  }
}
