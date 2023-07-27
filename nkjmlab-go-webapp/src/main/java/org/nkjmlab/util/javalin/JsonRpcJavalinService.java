package org.nkjmlab.util.javalin;

import org.nkjmlab.util.jakarta.servlet.jsonrpc.BasicJsonRpcServletService;
import org.nkjmlab.util.jakarta.servlet.jsonrpc.JsonRpcServletResponse;
import org.nkjmlab.util.java.json.JsonMapper;
import io.javalin.http.Context;

public class JsonRpcJavalinService extends BasicJsonRpcServletService {

  public JsonRpcJavalinService(JsonMapper mapper) {
    super(mapper);
  }

  /**
   * Handle context with JSON RPC service.
   *
   * @param ctx
   * @param service
   * @return
   */
  public JsonRpcServletResponse handle(Context ctx, Object service) {
    JsonRpcServletResponse jsres = super.handle(service, ctx.req(), ctx.res());
    ctx.result(jsres.getJson());
    return jsres;
  }

}
