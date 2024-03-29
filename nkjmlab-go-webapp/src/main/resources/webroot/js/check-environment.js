function flashError(msg) {
  $("#alert-error").show();
  $("#alert-error").html(msg);
}

$(function () {
  let enableWebsocket = false;

  $("#btn-login").attr("disabled", true);
  $("#btn-login").on('click', function () {
    if (!isEnableLocalStorage() || !enableWebsocket) {
      swalAlert("非対応環境です", "現在の環境ではこのシステムを利用できません", "error");
      return;
    }
  });

  checkWebsocket();
  checkDevice();
  if (isEnableLocalStorage()) {
    $("#localStorage").html(
      $('<div class="alert alert-success">').html(
        '<span class="badge bg-success">OK</span> '
        + "Webブラウザ設定は正常です．"));
    $("#btn-login").html(
      '<span>ログイン <i class="fas fa-caret-circle-right"></i></span>');
    $("#btn-login").attr("disabled", null);
  } else {
    $("#localStorage")
      .html(
        $('<div class="alert alert-danger">')
          .html(
            '<span class="badge bg-danger">ERROR</span> '
            + 'ローカルストレージを利用できません．Webブラウザがプライベートモードになっているならば，オフにして下さい．'
            + '<a class="alert-link" href="https://support.apple.com/ja-jp/HT203036">プライベートブラウズをオフにする  - Apple サポート<i class="fa fa-external-link"></i></a> を見る'));
    $("#btn-login").text("現在の環境では利用できません");
    $("#btn-login").prop("disabled", true);
  }

  function checkWebsocket() {
    const wsUrl = getWebSocketBaseUrl()+"/checkcon";
    const connection = new WebSocket(wsUrl);
    connection.onopen = function (e) {
      $("#websocket").html(
        $('<div class="alert alert-success">').html(
          '<span class="badge bg-success">OK</span> '
          + "ネットワーク環境は正常です．"));
      enableWebsocket = true;
      $("#btn-login").attr("disabled", null);
      connection.close();
    };
    connection.onerror = function (e) {
      $("#websocket").html(
        $('<div class="alert alert-danger">').html(
          '<span class="badge bg-danger">WARN</span> '
          + "Websocketによる通信が出来ません．"));
      enableWebsocket = false;
      $("#btn-login").attr("disabled", true);
    };
  }

  function checkDevice() {
    const uaParser = new UAParser();
    let unrecommended = false;

    if (!uaParser.getOS() || !uaParser.getBrowser()) {
      unrecommended = true;
    }
    if (uaParser.getBrowser().name === "Chrome"
      && Number(uaParser.getBrowser().version.split(".")[0]) >= 54) {

    } else if (uaParser.getOS().name === "Windows"
      || uaParser.getOS().name === "Linux") {
      if (uaParser.getBrowser().name === "Chrome"
        && Number(uaParser.getBrowser().version.split(".")[0]) >= 54) {
      } else if (uaParser.getBrowser().name === "Firefox"
        && Number(uaParser.getBrowser().version.split(".")[0]) >= 47) {
      } else if (uaParser.getBrowser().name === "IE"
        && Number(uaParser.getBrowser().version.split(".")[0]) >= 10) {
      } else {
        unrecommended = true;
      }
    } else if (uaParser.getOS().name === "Android") {
      if (Number(uaParser.getOS().version) < 5.0) {
        unrecommended = true;
      }

      if (uaParser.getBrowser().name === "Chrome"
        && Number(uaParser.getBrowser().version.split(".")[0]) >= 54) {
      } else {
        unrecommended = true;
      }
    } else if (uaParser.getOS().name === "iOS") {
      if (uaParser.getOS().version < 9.0) {
        unrecommended = true;
      }

      if (uaParser.getBrowser().name === "Chrome"
        && Number(uaParser.getBrowser().version.split(".")[0]) >= 54) {
      } else if (uaParser.getBrowser().name.indexOf("Safari") != -1
        && Number(uaParser.getBrowser().version) >= 9.0) {
      } else if (uaParser.getBrowser().name === "Firefox"
        && Number(uaParser.getBrowser().version.split(".")[0]) >= 47) {

      } else {
        unrecommended = true;
      }
    } else {
      unrecommended = true;
    }
    const osAndBrowser = (uaParser.getBrowser() ? uaParser.getBrowser().name
      + " " + uaParser.getBrowser().version : "unkown browser")
      + " ("
      + (uaParser.getOS() ? uaParser.getOS().name + " "
        + uaParser.getOS().version : "unkown OS") + ")";
    if (unrecommended) {
      $("#os-browser").html(
        $('<div class="alert alert-warning">').html(
          '<span class="badge bg-warning">WARN</span> '
          + osAndBrowser
          + "は推奨Webブラウザ/OSではないため，正しく動作しない可能性があります．"));
    } else {
      $("#os-browser").html(
        $('<div class="alert alert-success">').html(
          '<span class="badge bg-success">OK</span> '
          + osAndBrowser + "は推奨環境です．"));
    }
  }
});
