var JsonRpcRequest = (function() {
  var JsonRpcRequest = function(url, method, params, done, fail) {
    this.url = url;
    this.method = method;
    this.params = params;
    this.delay = 1000;
    this.initialDelay = 0;
    this.timeout = 20 * 1000;
    this.done = getDefaultDoneIfAbsent(done);
    this.fail = getDefaultFailIfAbsent(fail, url, method, params);
  };
  return JsonRpcRequest;
})();

var JsonRpcClient = (function() {

  var JsonRpcClient = function(request) {
    this.jqXHR = null;
    this.isFinish = false;
    // Does the request is success at least once;
    this.isSuccess = false;
    this.request = request;
  };

  var p = JsonRpcClient.prototype;

  p.setRequest = function(request) {
    this.request = request;
    return this;
  }

  p.rpc = function() {
    var req = JSON.stringify(this.request);
    var client = this;
    this.jqXHR = client.createAjaxObj().done(function(data, status, jqxhr) {
      client.request.done(data, status, jqxhr);
      client.isSuccess = true
    }).fail(this.request.fail);
    return this;
  }

  p.schedule = function(success, unsuccess) {
    var client = this;

    var success = getDefaultDoneIfAbsent(success);
    var unsuccess = getDefaultFailIfAbsent(unsuccess, client.request.url,
            client.request.method, client.request.params);

    var scheduleTimer;

    function refresh() {
      client.jqXHR = client.createAjaxObj().done(function(data, status, jqxhr) {
        client.request.done(data, status, jqxhr);
        client.isSuccess = true;
      }).fail(
              function(data, textStatus, errorThrown) {
                printError(data, textStatus, errorThrown, client.request.url,
                        client.request.method, client.request.params);
                client.request.fail(data, textStatus, errorThrown);
              }).always(function(data, textStatus, errorThrown) {
        if (client.isFinish) {
          if (client.isSuccess) {
            success(data);
          } else {
            unsuccess(data, textStatus, errorThrown);
          }
          if (client.jqXHR != null) {
            client.jqXHR.abort();
          }
          return;
        }
        client.jqXHR = null;
        clearTimeout(scheduleTimer);
        scheduleTimer = setTimeout(refresh, client.request.delay);
      });
    }
    scheduleTimer = setTimeout(refresh, client.request.initialDelay);
    return client;
  }

  p.repeat = function(times, success, unsuccess) {
    var client = this;
    var callback = this.request.done;
    client.counter = times;
    this.request.done = function(data, status, jqxhr) {
      if (client.counter === 0) {
        client.isFinish = true;
      } else {
        callback(data, status, jqxhr);
        client.isSuccess = true;
        client.counter--;
      }
    }
    return this.schedule(success, unsuccess)
  }

  /**
   * times: times of retry. If you set 1, call the method up to twice.
   */
  p.retry = function(times, success, unsuccess) {
    var client = this;
    var callback = this.request.done;
    var errorCallBack = this.request.fail;

    client.counter = times;

    this.request.done = function(data, status, jqxhr) {
      if (client.counter === 0) {
        client.isFinish = true;
      } else {
        callback(data, status, jqxhr);
        client.isSuccess = true;
        client.isFinish = true;
      }
    }

    this.request.fail = function(data, status, jqxhr) {
      if (client.counter === 0) {
        errorCallBack(data, status, jqxhr);
        client.isFinish = true;
      }
      client.counter--;
    }

    return this.schedule(success, unsuccess);
  }

  p.abort = function() {
    if (this.jqXHR != null) {
      this.jqXHR.abort();
    }
    this.isFinish = true;
  }

  p.createAjaxObj = function() {
    var client = this;
    return $.ajax({
      type: "POST",
      dataType: "json",
      url: client.request.url,
      data: JSON.stringify({
        method: client.request.method,
        params: client.request.params
      }),
      timeout: client.request.timeout,
    })
  }

  return JsonRpcClient;
})();

function getDefaultDoneIfAbsent(done) {
  return (done != null) ? done : function(data) {
    // console.log(data)
  };
}

function getDefaultFailIfAbsent(fail, url, method, params) {
  return (fail != null) ? fail : function(data, textStatus, errorThrown) {
    printError(data, textStatus, errorThrown, url, method, params);
  }
}
// jQueryのfailの第一引数はjqxhrだった．doneと違うことに注意．
function printError(data, textStatus, errorThrown, url, method, params) {
  var msg = textStatus + ', ' + errorThrown + '. response: '
          + JSON.stringify(data) + '. request: ' + url + ": " + method + "("
          + JSON.stringify(params) + ")";
  console.error(msg);
}
