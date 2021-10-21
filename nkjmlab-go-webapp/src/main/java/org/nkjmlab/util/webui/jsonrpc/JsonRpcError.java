package org.nkjmlab.util.webui.jsonrpc;

import org.nkjmlab.util.lang.ExceptionUtils;

public class JsonRpcError {
  private String faultCode;
  private String faultString;
  private String detail;

  public JsonRpcError() {}

  public JsonRpcError(String faultCode, String faultString, String detail) {
    this.faultCode = faultCode;
    this.faultString = faultString;
    this.detail = detail;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("faultCode: ").append(faultCode).append(", faultString: ")
        .append(faultString).append(", detail: ").append(detail).toString();
  }

  public String getFaultCode() {
    return faultCode;
  }

  public void setFaultCode(String faultCode) {
    this.faultCode = faultCode;
  }

  public String getFaultString() {
    return faultString;
  }

  public void setFaultString(String faultString) {
    this.faultString = faultString;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public static JsonRpcError createRpcFault(String faultCode, String faultString, Throwable t) {
    JsonRpcError f = new JsonRpcError();
    f.setFaultCode(faultCode);
    f.setFaultString(t.getClass().getName() + ":" + faultString);
    f.setDetail(ExceptionUtils.getMessageWithStackTrace(t));
    return f;
  }


}
