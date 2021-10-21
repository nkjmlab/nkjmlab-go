package org.nkjmlab.util.webui.jsonrpc;

import java.util.Arrays;

public class JsonRpcRequest {

  private String id;
  private String method;
  private Object[] params = new Object[0];
  public String callback;
  public String headers;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Object[] getParams() {
    return params;
  }

  public void setParams(Object[] params) {
    if (params == null) {
      this.params = new Object[0];
      return;
    }
    this.params = params;
  }

  @Override
  public String toString() {
    return "JsonRpcRequest [id=" + id + ", method=" + method + ", params=" + Arrays.toString(params)
        + ", callback=" + callback + ", headers=" + headers + "]";
  }


}
