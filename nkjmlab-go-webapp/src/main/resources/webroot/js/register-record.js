$(function () {
  function getOpponentId() {
    const ret = getBlackPlayerId() == getUserId() ? getWhitePlayerId() : getBlackPlayerId();
    return !ret ? getUserId() : ret;
  }
  $('#modal-record').on('hidden.bs.modal', function () {
    $("#register-record-opponent-stdid").val(getOpponentId());
    $("#register-record-memo").val("");
    $('input:radio[name="jadge"]').prop("checked", false);
  });

  $('#modal-record').on('show.bs.modal', function () {
    $("#register-record-opponent-stdid").val(getOpponentId());
  });

  $("#btn-register-record").on(
    'click',
    function () {
      for (let i = 0; i < $('#register-record-form input').length; i++) {
        if (!$('#register-record-form input')[i].checkValidity()) {
          $('#register-record-form .submit-for-validation').trigger("click");
          return;
        }
      }

      const opponentUserId = getFormattedStdId($("#register-record-opponent-stdid").val());
      const memo = $("#register-record-memo").val() ? $("#register-record-memo").val() : "";
      const jadge = $('input:radio[name="jadge"]:checked').val();

      if (!opponentUserId) {
        swalAlert("入力エラー",
          "対局相手に無効な値が入力されました", "error");
        return;
      }
      swalConfirm("確認", "<strong>[" + jadge + "]</strong> を送信します", null, function () {
        setTimeout(function () {
          new JsonRpcClient(new JsonRpcRequest(getBaseUrl(), "registerRecord", [
            getUserId(), opponentUserId, jadge, memo], function (data) {
              const rank = data.result;
              swalAlert("登録完了", "<strong>[" + jadge + "]</strong> を登録しました", "info", function () {
                if (rank == -1) {
                  $('#modal-records').modal('show');
                } else {
                  setTimeout(function () {
                    swalAlert("", "<strong>[" + rank + "級]</strong>に昇級しました <i class='fas fa-trophy'></i>", "success",
                      function () {
                        $('#modal-records').modal('show');
                      })
                  }, 800);
                }
              });
            })).rpc();
        }, 800);
        $('#modal-record').modal('hide');
      });
    });
});
