$(function () {
  swalInput("完全初期化", "初期化するIDを入れてください", getUserId(), "5511000", function (
    inputVal) {
    if (!inputVal) {
      alert("初期化を中止しました．");
      location.href = "play.html";
      return;
    }
    setTimeout(function () {
      setGameId(inputVal);
      setUserId(inputVal);
      sendNewGame(createGameState(), 9, function (data) {
        swalAlert("初期化完了", "", "success", function (data) {
          localStorage.clear();
          location.href = "play.html";
          return;
        });
      });
    }, 1000);
  });
});
