class JsonRpcRequest {
  constructor(url, method, params, done, fail) {
    this.url = url;
    this.method = method;
    this.params = params;
    this.delay = 1000;
    this.initialDelay = 0;
    this.timeout = 20 * 1000;
    this.done = this.getDefaultDoneIfAbsent(done);
    this.fail = this.getDefaultFailIfAbsent(fail, url, method, params);
  };

  getDefaultDoneIfAbsent = (done) => {
    return (done != null) ? done : (data) => {
      // console.log(data)
    };
  }

  getDefaultFailIfAbsent = (fail, url, method, params) => {
    return (fail != null) ? fail : (data, textStatus, errorThrown) => {
      printError(data, textStatus, errorThrown, url, method, params);
    }
  }
}

class JsonRpcClient {


  constructor(request) {
    this.jqXHR = null;
    this.isFinish = false;
    // Does the request is success at least once;
    this.isSuccess = false;
    this.request = request;
  };

  setRequest = (request) => {
    this.request = request;
    return this;
  }

  rpc = () => {
    this.jqXHR = this.createAjaxObj().done((data, status, jqxhr) => {
      this.request.done(data, status, jqxhr);
      this.isSuccess = true
    }).fail(this.request.fail);
    return this;
  }

  schedule = (success, unsuccess) => {

    let scheduleTimer;

    const refresh = () => {
      this.jqXHR = this.createAjaxObj().done((data, status, jqxhr) => {
        this.request.done(data, status, jqxhr);
        this.isSuccess = true;
      }).fail(
        (data, textStatus, errorThrown) => {
          printError(data, textStatus, errorThrown, this.request.url,
            this.request.method, this.request.params);
          this.request.fail(data, textStatus, errorThrown);
        }).always((data, textStatus, errorThrown) => {
          if (this.isFinish) {
            if (this.isSuccess && success) {
              success(data);
            } else if (!this.isSuccess && unseccess) {
              unsuccess(data, textStatus, errorThrown);
            }
            if (this.jqXHR != null) {
              this.jqXHR.abort();
            }
            return;
          }
          this.jqXHR = null;
          clearTimeout(scheduleTimer);
          scheduleTimer = setTimeout(refresh, this.request.delay);
        });
    }
    scheduleTimer = setTimeout(refresh, this.request.initialDelay)
    return this;
  }

  repeat = (times, success, unsuccess) => {
    const callback = this.request.done;
    this.counter = times;
    this.request.done = (data, status, jqxhr) => {
      if (this.counter === 0) {
        this.isFinish = true;
      } else {
        callback(data, status, jqxhr);
        this.isSuccess = true;
        this.counter--;
      }
    }
    return this.schedule(success, unsuccess)
  }

  /**
   * times: times of retry. If you set 1, call the method up to twice.
   */
  retry = (times, success, unsuccess) => {
    const callback = this.request.done;
    const errorCallBack = this.request.fail;

    this.counter = times;

    this.request.done = (data, status, jqxhr) => {
      if (this.counter === 0) {
        this.isFinish = true;
      } else {
        callback(data, status, jqxhr);
        this.isSuccess = true;
        this.isFinish = true;
      }
    }

    this.request.fail = (data, status, jqxhr) => {
      if (this.counter === 0) {
        errorCallBack(data, status, jqxhr);
        this.isFinish = true;
      }
      this.counter--;
    }

    return this.schedule(success, unsuccess);
  }

  abort = () => {
    if (this.jqXHR != null) {
      this.jqXHR.abort();
    }
    this.isFinish = true;
  }

  createAjaxObj = () => {
    return $.ajax({
      type: "POST",
      dataType: "json",
      url: this.request.url,
      data: JSON.stringify({
        jsonrpc: "2.0",
        id: Math.floor(Math.random() * Date.now()),
        method: this.request.method,
        params: this.request.params
      }),
      timeout: this.request.timeout,
    })
  }
}


function printError(data, textStatus, errorThrown, url, method, params) {
  console.error(data.responseJSON.error.code + ' ' + data.responseJSON.error.message);
  console.error(url + ": "
    + method + "(" + JSON.stringify(params) + ")");
}