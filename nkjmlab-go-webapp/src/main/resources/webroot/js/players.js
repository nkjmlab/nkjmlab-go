$(function () {
  $('#modify-rank-modal').on('show.bs.modal', function (event) {
    const button = $(event.relatedTarget);

    const stdid = button.data('stdid');
    const rank = button.data('rank');
    const point = button.data('point');

    const modal = $(this);
    modal.find('#modify-rank-stdid').val(stdid);
    modal.find('#modify-rank-rank').val(rank);
    modal.find('#modify-rank-point').val(point);
  });

  $("#btn-modify-rank-send").on('click', function () {
    for (let i = 0; i < $('#form-modify-rank input').length; i++) {
      if (!$('#form-modify-rank input')[i].checkValidity()) {
        $('#form-modify-rank .submit-for-validation').trigger("click");
        return;
      }
    }

    const stdid = $("#modify-rank-stdid").val();
    const rank = $("#modify-rank-rank").val();
    const point = $("#modify-rank-point").val();

    swalConfirm("確認", "ランク情報を更新しますか？", "info", function () {
      setTimeout(function () {
        const request = new JsonRpcRequest(
          getGoRpcServiceUrl(),
          "modifyRankAndPoint",
          [stdid, rank, point],
          data => {
            swalAlert("更新完了", "", "success", () => location.reload());
          }
        );
        new JsonRpcClient(request).rpc();
      }, 800);
      $('#modify-rank-modal').modal('hide');
    });
  });
});
