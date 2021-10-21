package org.nkjmlab.util.webui.jsonrpc;

public class JsonRpcResponse {
  private String id;
  private Object result;
  private JsonRpcError error;

  public boolean hasError() {
    return error != null;
  }


  public JsonRpcError getError() {
    return error;
  }

  public void setError(JsonRpcError error) {
    this.error = error;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    this.result = result;
  }


  @Override
  public String toString() {
    return "JsonRpcResponse [id=" + id + ", result=" + result + ", error=" + error + "]";
  }


}
