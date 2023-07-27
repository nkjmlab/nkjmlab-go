$(function () {
    $("form input").on('keypress', function (ev) {
        if ((ev.which && ev.which === 13) || (ev.keyCode && ev.keyCode === 13)) {
            ev.preventDefault();
        }
    });

    $('#signup-modal').on('hidden.bs.modal', function () {
        location.reload();
    });
    setGuest(false);

    $("#btn-guest-login").on(
        'click',
        function () {
            $("#signup-modal-title").html('ゲスト <i class="far fa-address-card"></i>');
            $(".signin-guest").show();
            $(".signin-user").hide();
        });

    $("#btn-user-login").on(
        'click',
        function () {
            $("#signup-modal-title").html('ユーザ <i class="far fa-address-card"></i>');
            $(".signin-user").show();
            $(".signin-guest").hide();
        });

    $("#btn-guest-signin").on(
        'click',
        function () {
            setGuest(true);
            setUserMode(STUDENT);

            for (let i = 0; i < $('#signup-form input:visible').length; i++) {
                if (!$('#signup-form input:visible')[i].checkValidity()) {
                    $('#signup-form .submit-for-validation').trigger("click");
                    return;
                }
            }

            const stdId = getFormattedStdId($("#signup-username-stdid").val());
            const seatId = getFormattedSeatId($("#signup-seatid").val());

            if (!seatId || !stdId) {
                swalAlert("入力エラー", (!seatId ? "座席番号" : "") + (!stdId ? " 学籍番号" : "") + "に無効な値が入力されました", "error");
                return;
            }
            const uname = $("#signup-username-name").val();

            new JsonRpcClient(new JsonRpcRequest(getAuthRpcServiceUrl(), "signupAsGuest", [
                stdId, uname, seatId], function () {
                    setUserId(stdId);
                    setUserName(uname);
                    setSeatId(seatId);
                    setLoginDate(getCurrentDate());
                    location = "play.html";
                })).rpc();
        });

    $("#btn-user-signin").on(
        'click',
        function () {
            setUserMode(STUDENT);

            for (let i = 0; i < $('#signup-form input:visible').length; i++) {
                if (!$('#signup-form input:visible')[i].checkValidity()) {
                    $('#signup-form .submit-for-validation').trigger("click");
                    return;
                }
            }

            const stdId = getFormattedStdId($("#signup-username-stdid").val());
            const seatId = getFormattedSeatId($("#signup-seatid").val());

            if (!seatId || !stdId) {
                swalAlert("入力エラー", (!seatId ? "座席番号" : "") + (!stdId ? " 学籍番号" : "") + "に無効な値が入力されました", "error");
                return;
            }

            const password = $("#signup-password").val();
            new JsonRpcClient(new JsonRpcRequest(getAuthRpcServiceUrl(), "signinWithoutFirebase", [
                stdId, password, seatId], function (data) {
                    if (data.result == null) {
                        swalAlert("エラー", "ログイン失敗", "error");
                        return;
                    }
                    setUserId(stdId);
                    setUserName(data.result.userName);
                    setSeatId(seatId);
                    setLoginDate(getCurrentDate());
                    location = "play.html";
                }, e => {
                    swalAlert("ログイン失敗", e.responseJSON.error.message, "error");
                })).rpc();
        });

});
