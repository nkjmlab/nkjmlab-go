const ON_BOARD = "onBoard";
const REMOVE_FROM_BOARD = "removeFromBoard";
const AGEHAMA = "agehama";
const FROM_AGEHAMA = "fromAgehama";
const FROM_POD = "fromPod";

const HAND_TYPE_PUT_ON_BOARD = "putOnBoard"
const HAND_TYPE_MESSAGE = "message";

const GAME_MODES = {
  play: "対局",
  practice: "練習",
}

const BLANK_STONE = 0;
const BLACK_STONE = 1;
const WHITE_STONE = 2;

const RECTANGLE_STONE = 10;
const TRIANGLE_STONE = 20;
const X_STONE = 30;
const A_STONE = 40;
const B_STONE = 50;
const C_STONE = 60;
const CIRCLE_STONE = 70;

const STONE_SELECTOR_MAP = {
  "0": "blank",
  "1": "black",
  "2": "white",
  "10": "rectangle",
  "20": "triangle",
  "30": "x",
  "40": "a",
  "50": "b",
  "60": "c",
  "70": "CIRCLE"
}

const SELECTOR_STONE_MAP = {
  "blank": 0,
  "black": 1,
  "white": 2,
  "rectangle": 10,
  "triangle": 20,
  "x": 30,
  "a": 40,
  "b": 50,
  "c": 60,
  "circle": 70
}

const STONES_MAP = {
  "0": "空白",
  "1": "黒",
  "2": "白",
  "10": "□",
  "20": "△",
  "30": "X",
  "40": "A",
  "50": "B",
  "60": "C",
  "70": "○"
}

const VOTE_MAP = {
  "□": "10",
  "△": "20",
  "X": "30",
  "A": "40",
  "B": "50",
  "C": "60",
  "○": "70"
}

function createGameState() {
  return {
    gameId: getGameId(),
    blackPlayerId: "",
    whitePlayerId: "",
    handHistory: []
  };
}

function getFormattedSeatId(text) {
  const fText = toInt(text);
  if (fText == 0) { return new String(fText); }
  if (!fText) { return false; }
  if ((-1 <= fText && fText <= 999)) { return new String(fText); }
  return false;
}

function getFormattedStdId(text) {
  const fText = toInt(text);
  if (!fText) { return false; }
  if ((1000000 <= fText && fText <= 9999999)) { return new String(fText); }
  return false;
}

function getFormattedRank(text) {
  const fText = toInt(text);
  if (!fText) { return false; }
  if ((1 <= fText && fText <= 30)) { return fText; }
  return false;
}

function promptInputUserId(title, text, ph, uid, callback) {
  swalInput(title, text, uid, ph, function (inputValue) {
    if (!inputValue || inputValue === "") {
      location.href = "play.html";
      return;
    }
    const uid = getFormattedStdId(inputValue);
    if (!uid) {
      setTimeout(function () {
        swalAlert("入力エラー", "無効な値が入力されました", "error", function () {
          setTimeout(function () {
            promptInputUserId(title, text, ph, null, callback);
          }, 300);
        })
      }, 300);
      return;
    }
    callback(uid);
  });
}

function setGameStateOptions(gameState) {
  const gameId = getGameId();
  if (gameState.gameId == null) {
    gameState.gameId = gameId;
  }
}


function sendGameStateByWs(connection, gameState) {
  connection.send(JSON.stringify(gameState));
}

function sendGameStateByJsonRpc(gameState) {
  new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "sendGameState", [
    getGameId(), gameState], function (data) {
    }, function (data, textStatus, errorThrown) {
      swalAlert("ページを再読み込みします", "", "info", e => location.reload());
    })).rpc();
}


function sendGameStateWithLastHand(connection, gameState, hand) {
  if (getGameId() == null) { return; }
  if (gameState.handHistory == null) {
    gameState.handHistory = new Array();
  }
  gameState.lastHand = hand;
  gameState.lastHand.number = gameState.handHistory.length;
  gameState.handHistory.push(gameState.lastHand);
  gameState.blackPlayerId = getGameId().split("-vs-")[0] ? getGameId().split(
    "-vs-")[0] : getUserId();
  gameState.whitePlayerId = getGameId().split("-vs-")[1] ? getGameId().split(
    "-vs-")[1] : getUserId();

  sendGameStateByJsonRpc(gameState);
  //sendGameStateByWs(connection, gameState);
}

function sendNewGame(gameState, ro, callback) {
  if (!callback) {
    callback = function (data) {
    };
  }
  setGameStateOptions(gameState);
  new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "newGame", [getGameId(),
    gameState], callback)).rpc();
}

function notifyLoginToBoard() {
  new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "notifyLoginToBoard", [
    getGameId(), getUserId()], function (data) {
    })).rpc();
}

function goBack(gameState, callback) {
  new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "goBack", [getGameId(),
    gameState], function (data) {
    })).rpc();
}

function getStoneColor(stone) {
  return stone % 10;
}

function getStoneSymbol(stone) {
  return parseInt(stone / 10) * 10;
}

function stoneToSelector(stone) {
  return "#moving-" + STONE_SELECTOR_MAP[stone] + "-stone";
}

function selectorToStone(selector) {
  const stone = selector.split("-")[1];
  return SELECTOR_STONE_MAP[stone];
}

function getGoRpcServiceUrl() {
  const u = new URL(document.URL);
  const urlPrefix = u.protocol + "//" + u.host + "/"  + u.pathname.split("/")[1] + "/";
  return urlPrefix + "json/GoJsonRpcService";
}

function getAuthRpcServiceUrl() {
  const u = new URL(document.URL);
  const urlPrefix = u.protocol + "//" + u.host + "/"  + u.pathname.split("/")[1] + "/";
  return urlPrefix + "json/AuthRpcService";
}


function isStudent() {
  if (!getUserMode()) { return true; }
  return getUserMode() == STUDENT;
}

function isTeacher() {
  if (!getUserMode()) { return false; }
  return getUserMode() == TEACHER;
}

function isCreator() {
  if (!getUserMode()) { return false; }
  return getUserMode() == CREATOR;
}

function isAdminUid(uid) {
  return uid.startsWith("5566");
}

function getProblemIdFromUrl() {
  return new URL(location).searchParams.get("problem_id");
}

function getGameIdFromUrl() {
  return new URL(location).searchParams.get("game_id");
}

function getMyStoneColor() {
  if (getGameMode() != PLAY) { return BLACK_STONE; }
  return getGameId().split("-vs-").indexOf(getUserId()) + 1;
}

function _setGameIdWithPlayers(p1, p2) {
  setGameId(p1 + "-vs-" + p2);
}

function syncGameState(sessionId) {
  new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "syncGameState", [
    sessionId, getGameId(), getUserId()], function (data) {
      initView();
      refreshProblemInfo();
    })).rpc();

}

function createImageTag(uid) {
  const fallback = "this.onerror=null;this.src='../img/icon/no-player-icon.png'";
  const imgTag = '<img class="player-icon rounded" src="../img/icon/' + uid
    + '.png?' + dateNow + '" onerror=' + fallback + '>';
  return imgTag;
}

function loadProblemOnMyBoard(problemId, callback) {
  callLoadProblem(getUserId(), problemId, callback);
}

function loadProblemOnCurrentBoard(problemId, callback) {
  callLoadProblem(getGameId(), problemId, callback);
}

function callLoadProblem(gameId, problemId, callback) {
  if (problemId == null) { return; }
  callback = callback != null ? callback : function (data) {
  };
  new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "loadProblem", [gameId,
    problemId], callback)).rpc();
}
