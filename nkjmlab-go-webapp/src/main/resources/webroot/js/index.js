
$(function () {
    $('#signup-modal').on('hidden.bs.modal', function () {
        location.reload();
    });
    $("#signup-modal-title").html('ゲストサインアップ <i class="far fa-address-card"></i>');
    setGuest(false);
    $("#btn-signup").on(
        'click',
        function () {
            setGuest(true);
            setUserMode(STUDENT);

            for (let i = 0; i < $('#signup-form input').length; i++) {
                if (!$('#signup-form input')[i].checkValidity()) {
                    $('#signup-form .submit-for-validation').trigger("click");
                    return;
                }
            }

            let stdId = getFormattedStdId($("#signup-username-stdid").val());
            let seatId = getFormattedSeatId($("#signup-seatid").val());

            if (!seatId || !stdId) {
                swalAlert("入力エラー", (!seatId ? "座席番号" : "") + (!stdId ? " 学籍番号" : "") + "に無効な値が入力されました", "error");
                return;
            }
            let uname = $("#signup-username-name").val();

            new JsonRpcClient(new JsonRpcRequest(getBaseUrl(), "signupAsGuest", [
                stdId, uname, seatId], function () {
                    setUserId(stdId);
                    setUserName(uname);
                    setSeatId(seatId);
                    setLoginDate(getCurrentDate());
                    location = "play.html";
                })).rpc();
        });
});
