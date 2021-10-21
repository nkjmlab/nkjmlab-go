package org.nkjmlab.util.webui.jsonrpc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.nkjmlab.util.json.JsonMapper;

public class JsonRpcService {

  private JsonMapper mapper;

  public JsonRpcService(JsonMapper mapper) {
    this.mapper = mapper;
  }

  public JsonRpcResponse callJsonRpc(Object service, JsonRpcRequest jreq) {
    return JsonRpcUtils.callJsonRpc(mapper, service, jreq);
  }

  public JsonRpcResponse callHttpJsonRpc(Object service, HttpServletRequest req,
      HttpServletResponse res) {
    JsonRpcRequest jreq = JsonRpcUtils.toJsonRpcRequest(mapper, req);
    return callHttpJsonRpc(service, jreq, res);
  }

  public JsonRpcResponse callHttpJsonRpc(Object service, JsonRpcRequest jreq,
      HttpServletResponse res) {

    JsonRpcUtils.setContentTypeToJson(res);
    JsonRpcUtils.setAccessControlAllowOriginToWildCard(res);
    JsonRpcUtils.setAccessControlAllowMethodsToWildCard(res);
    JsonRpcUtils.setAccessControlAllowHeadersToWildCard(res);

    JsonRpcResponse result = JsonRpcUtils.callJsonRpc(mapper, service, jreq);
    if (result.hasError()) {
      res.setStatus(500);
    } else {
      res.setStatus(200);
    }
    return result;
  }

}
