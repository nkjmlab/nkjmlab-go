$(function() {

  $('#modal-icon-upload').on('show.bs.modal', function(e) {
    $("#modal-icon-stdid").val(getUserId());
  })

  $("#btn-icon-select").on('click', function() {
    $('#file-icon-image').click();
  });

  // この写真を送信しますか？
  $("#btn-icon-upload").on(
          'click',
          function(e) {
            for (let i = 0; i < $('#form-icon-upload input').length; i++) {
              if (!$('#form-icon-upload input')[i].checkValidity()) {
                $('#form-icon-upload .submit-for-validation').trigger("click");
                return;
              }
            }

            const stdId = getFormattedStdId($("#modal-icon-stdid").val());

            if (!stdId) {
              swalAlert("入力エラー", (!stdId ? " 学籍番号" : "") + "に無効な値が入力されました",
                      "error");
              return;
            }

            swalConfirm("", "アイコン用にこの画像をアップロードしますか？", "info", function(e) {
              $("#modal-icon-upload").modal('hide');
              const imgData = $("#img-icon-preview").attr('src');
              new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
                      "uploadImage", [stdId, imgData], function(data) {
                        setTimeout(function() {
                          swalAlert("成功", "画像がアップロードされました", "info",
                                  function(e) {
                                    location.reload();
                                  });
                        }, 300);
                      })).rpc();
            });
          });
});

function handleFiles(files) {
  if (files == null || files.length == 0 || files[0] == null) {
    alert("Fail to get image."); // 画像を取得できませんでした
    return;
  }
  const file = files[0];
  loadImage.parseMetaData(file, function(data) {
    var option = {
      canvas: true
    };
    if (data.exif && data.exif.get('Orientation')) {
      option.orientation = data.exif.get('Orientation');
    }
    loadImage(file, function(canvas) {
      $("#img-icon-preview").attr('src', canvas.toDataURL("image/png"));
    }, option);
  });

  $("#btn-icon-upload").prop("disabled", false);
}
