$(function () {
  $("#btn-logout").on("click", function () {
    firebaseSignOut();
  });

  const uiConfig = {
    callbacks: {
      signInSuccessWithAuthResult: function (authResult, redirectUrl) {
        signin(authResult.user);
        return false;
      }
    },
    signInFlow: SIGN_IN_FLOW,
    signInOptions: [firebase.auth.GoogleAuthProvider.PROVIDER_ID],
    tosUrl: 'https://www.nakajimalab.is.sci.toho-u.ac.jp/site-policy.html',
    privacyPolicyUrl: 'https://www.nakajimalab.is.sci.toho-u.ac.jp/site-policy.html'
  };

  const ui = new firebaseui.auth.AuthUI(firebase.auth());
  ui.start('#fb-auth-container', uiConfig);

  firebase.auth().onAuthStateChanged(currentUser => {
    if (currentUser) {
      if (!currentUser.emailVerified) {
        console.log(currentUser.email + " is not verified");
        currentUser.sendEmailVerification().then(() => {
          console.log("send email");
          flashError("メールアドレスの確認が必要です．メールボックスを確認し，メールアドレスの確認をして下さい．");
        }).catch((error) => {
          console.log("Fail to send email");
          flashError("メールアドレスの確認が必要です．メールボックスを確認し，メールアドレスの確認をして下さい．");
        });
      } else {
        const req = new JsonRpcRequest(getAuthRpcServiceUrl(), "isSigninToFirebase", [],
          function (data) {
            if (data.result) {
              $("#fb-loading").hide();
              $("#fb-auth-container").hide();
              $(".already-logged-in").show();
              $("#user-email").text(currentUser.email);
            } else {
              $("#fb-loading").hide();
              $("#fb-auth-container").show();
              signin(currentUser);
            }
          });
        const client = new JsonRpcClient(req);
        client.rpc();
      }
    } else {
      $("#fb-loading").hide();
      $("#fb-auth-container").show();
      $(".already-logged-in").hide();
    }
  });
});

function signin(user) {
  setTimeout(() => signinAux(user), 500);
}

function signinAux(user) {
  swalInput('座席番号 <i class="fas fa-chair"></i>', "座席番号を半角数字で入力して下さい．<br>座席番号がない場合は0を入力して下さい．",
    getSeatId() ? getSeatId() : 0, "", function (seatId) {
      if (!seatId || seatId === 'false') {
        signin(user);
        return;
      }
      setSeatId(seatId);
      user.getIdToken(true).then(function (idToken) {
        const req = new JsonRpcRequest(getAuthRpcServiceUrl(), "signinWithFirebase", [idToken, seatId],
          function (data) {
            const u = data.result;
            if (u) {
              setIdToken(idToken);
              setUserId(u.userId);
              setUserName(u.userName);
              setRank(u.rank);
              setGameId(u.userId);
              setGoogleLoginDate(getCurrentDate());
              setLoginDate(getCurrentDate());
              location.href = "play.html";
            } else {
              flashError("受講登録されたアカウントでしかログインできません．受講登録がない場合は「ゲスト」を使って下さい．");
            }
          }, function (data, textStatus, errorThrown) {
            flashError("受講登録されたアカウントでしかログインできません．受講登録がない場合は「ゲスト」を使って下さい．");
          });
        req.timeout = 30 * 1000;
        const client = new JsonRpcClient(req);
        client.rpc();
      }).catch(function (error) {
        console.error(error);
      });
    });
}