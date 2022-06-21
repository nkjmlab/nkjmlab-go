const KEY_CELL_NUM = "cell_num";
const KEY_BLACK_PLAYER_ID = "black_player_id";
const KEY_WHITE_PLAYER_ID = "white_player_id";
const KEY_GAME_ID = "game_id";
const KEY_VISITED_GAME_IDS = "game_visited_ids";
const KEY_USER_ID = "user_id";
const KEY_USER_NAME = "user_name";
const KEY_SEAT_ID = "seat_id";
const KEY_PROBLEM = "problem";
const KEY_RANK = "rank";
const KEY_LOGIN_DATE = "login_date";
const KEY_GOOGLE_LOGIN_DATE = "google_login_date";
const KEY_GUEST = "guest";
const PLAY = "play";
const PRACTICE = "practice";
const KEY_USER_MODE = "user_mode";
const TEACHER = "TEACHER";
const CREATOR = "CREATOR";
const STUDENT = "STUDENT";
const KEY_ID_TOKEN = "id_token";
const KEY_DRAW_STONE_NUMBER = "draw_stone_number";

function getReveseGameMode() {
  if (getGameMode() == PLAY) {
    return PRACTICE;
  } else {
    return PLAY;
  }
}

function isDrawStoneNumber() {
  return JSON.parse(getItem(KEY_DRAW_STONE_NUMBER));
}

function setDrawStoneNumber(flag) {
  setItem(KEY_DRAW_STONE_NUMBER, JSON.stringify(flag));
}


function getIdToken() {
  return JSON.parse(getItem(KEY_ID_TOKEN));
}

function setIdToken(idToken) {
  setItem(KEY_ID_TOKEN, JSON.stringify(idToken));
}


function getRank() {
  return JSON.parse(getItem(KEY_RANK));
}

function setRank(rank) {
  setItem(KEY_RANK, JSON.stringify(rank));
}

function getProblem() {
  return JSON.parse(getItem(KEY_PROBLEM));
}

function setProblem(problem) {
  setItem(KEY_PROBLEM, JSON.stringify(problem));
}


function getGameMode() {
  if (getGameId() && getGameId().split("-").includes("vs")) {
    return PLAY;
  }

  return PRACTICE;
}

function getUserMode() {
  return getItem(KEY_USER_MODE);
}

function setUserMode(mode) {
  setItem(KEY_USER_MODE, mode);
}


function getUserId() {
  return getItem(KEY_USER_ID);
}

function setUserId(uid) {
  setItem(KEY_USER_ID, uid);
}

function getUserName() {
  return getItem(KEY_USER_NAME);
}

function setUserName(uname) {
  setItem(KEY_USER_NAME, uname);
}

function getSeatId() {
  return getItem(KEY_SEAT_ID);
}

function setSeatId(sid) {
  setItem(KEY_SEAT_ID, sid);
}

function clearVisitedGameIds() {
  return setItems(KEY_VISITED_GAME_IDS, [])
}

function getVisitedGameIds() {
  return getItems(KEY_VISITED_GAME_IDS)
}
function addVisitedGameId(id) {
  if (id == null) {
    return;
  }
  if (id.indexOf("-vs-") == -1) {
    return;
  }

  const arr = getVisitedGameIds();

  if (arr[arr.length - 1] && arr[arr.length - 1].indexOf(id) != -1) {
    return;
  }
  addItems(KEY_VISITED_GAME_IDS, [id]);
}

function getLoginDate() {
  return getItem(KEY_LOGIN_DATE);
}

function setLoginDate(date) {
  setItem(KEY_LOGIN_DATE, date);
}

function isGuest() {
  const t = getItem(KEY_GUEST);
  if (t == null) {
    return true;
  } else {
    return t;
  }
}

function setGuest(flag) {
  setItem(KEY_GUEST, flag);
}


function getGoogleLoginDate() {
  return getItem(KEY_GOOGLE_LOGIN_DATE);
}

function setGoogleLoginDate(date) {
  setItem(KEY_GOOGLE_LOGIN_DATE, date);
}

function getGameId() {
  return getItem(KEY_GAME_ID);
}

function setGameId(gid) {
  addVisitedGameId(gid);
  setItem(KEY_GAME_ID, gid);
}

function getBlackPlayerId() {
  let p = getGameId().split("-vs-")[0];
  return p ? p : getUserId();
}

function getWhitePlayerId() {
  let p = getGameId().split("-vs-")[1];
  return p ? p : getUserId();
}

function getCellNum() {
  return parseInt(getItem(KEY_CELL_NUM, 9));
}

function setCellNum(cellNum) {
  setItem(KEY_CELL_NUM, cellNum);
}

function getWebSocketBaseUrl() {
  function createWebSocketUrl(protocol) {
    const u = parseUri(document.URL);
    const urlPrefix = protocol + "://" + u.authority + "/";
    return urlPrefix + "websocket/play";
  }
  if (parseUri(location).protocol === "https") {
    return createWebSocketUrl("wss");
  } else {
    return createWebSocketUrl("ws");
  }
}


function getCurrentX(e) {
  return e.type.indexOf("touch") == -1 ? e.pageX : e.changedTouches[0].pageX;
}

function getCurrentY(e) {
  return e.type.indexOf("touch") == -1 ? e.pageY : e.changedTouches[0].pageY;
}

function addTempItems(itemsName, items) {
  setTempItems(itemsName, getTempItems(itemsName).concat(items))
}

function setTempItem(key, val) {
  window.sessionStorage.setItem(key, JSON.stringify(val));
}

function getTempItem(key) {
  const val = window.sessionStorage.getItem(key);
  if (val == null) { return null; }
  if (val === "undefined") { return null; }
  return JSON.parse(val);
}

function getTempItems(itemsName) {
  const items = getTempItem(itemsName);
  return items ? JSON.parse(items) : [];
}

function setTempItems(itemsName, items) {
  setTempItem(itemsName, JSON.stringify(items ? items : []));
}

function setItem(key, val) {
  window.localStorage.setItem(key, JSON.stringify(val));
}

function getItem(key) {
  const val = window.localStorage.getItem(key);
  if (val == null) { return null; }
  if (val === "undefined") { return null; }
  return JSON.parse(val);
}

function getItems(itemsName) {
  const items = getItem(itemsName);
  return items ? JSON.parse(items) : [];
}

function setItems(itemsName, items) {
  setItem(itemsName, JSON.stringify(items ? items : []));
}

function addItems(itemsName, items) {
  setItems(itemsName, getItems(itemsName).concat(items))
}

// localStorageに保存されている，あるkeyの値を削除する
function removeItem(key) {
  window.localStorage.removeItem(key);
}
// localStorageに保存されているすべての値を削除する
function clearLocalStorage() {
  window.localStorage.clear();
}

function isEnableLocalStorage() {
  try {
    window.localStorage.setItem("enableLocalStorage", "true");
    window.localStorage.getItem("enableLocalStorage");
    return true;
  } catch (e) {
    return false;
  }
}


function swalConfirm(title, text, type, callback) {
  Swal.fire({
    animation: false,
    title: title,
    html: text ? text : null,
    type: type ? type : null,
    showCancelButton: true
  }).then((e) => {
    if (e.dismiss) {
      return;
    }
    callback(e);
  });
}


const swalToast = Swal.mixin({
  toast: true,
  type: 'info',
  timer: 3000,
})


function swalAlert(title, text, type, callback, confirmButtonText) {
  Swal.fire({
    animation: true,
    title: title,
    html: text ? text : null,
    type: type ? type : null,
    confirmButtonText: confirmButtonText ? confirmButtonText : "OK"
  }).then((result) => {
    if (!callback) {
      return;
    }
    callback(result);
  })
}

function swalInput(title, text, inputValue, inputPlaceholder, callback) {
  Swal.fire({
    animation: false,
    title: title,
    input: 'text',
    html: text ? text : null,
    inputPlaceholder: inputPlaceholder,
    inputValue: inputValue,
    inputAttributes: {
      autocapitalize: 'off'
    },
    showCancelButton: true,
  }).then((result) => {
    callback(result.value);
  })
}

function swalTextArea(title, text, inputValue, inputPlaceholder, callback) {
  Swal.fire({
    animation: false,
    title: title,
    input: 'textarea',
    html: text ? text : null,
    inputPlaceholder: inputPlaceholder,
    inputValue: inputValue,
    inputAttributes: {
      autocapitalize: 'off'
    },
    showCancelButton: true,
  }).then((result) => {
    callback(result.value);
  })
}

function numToCode(num) {
  switch (num) {
    case 0:
      return "A";
    case 1:
      return "B";
    case 2:
      return "C";
    case 3:
      return "D";
    case 4:
      return "E";
    case 5:
      return "F";
    case 6:
      return "G";
    case 7:
      return "H";
    case 8:
      return "I";
    case 9:
      return "J";
    case 10:
      return "K";
    case 11:
      return "L";
    case 12:
      return "M";
    case 13:
      return "N";
    case 14:
      return "O";
    case 15:
      return "P";
    case 16:
      return "Q";
    case 17:
      return "R";
    case 18:
      return "S";
    default:
      console.error(num);
  }
}

function numToKansuji(num) {
  switch (num) {
    case 0:
      return "一";
    case 1:
      return "二";
    case 2:
      return "三";
    case 3:
      return "四";
    case 4:
      return "五";
    case 5:
      return "六";
    case 6:
      return "七";
    case 7:
      return "八";
    case 8:
      return "九";
    case 9:
      return "十";
    case 10:
      return "十一";
    case 11:
      return "十二";
    case 12:
      return "十三";
    case 13:
      return "十四";
    case 14:
      return "十五";
    case 15:
      return "十六";
    case 16:
      return "十七";
    case 17:
      return "十八";
    case 18:
      return "十九";
    default:
      console.error(num);
  }
}


function toFormattedDate(milliseconds) {
  const date = new Date(milliseconds);
  const str = [date.getFullYear(), padding(date.getMonth() + 1), padding(date.getDate())].join('-');
  return str;
}

function toFormattedTime(milliseconds) {
  const date = new Date(milliseconds);
  const str = [padding(date.getHours()), padding(date.getMinutes()), padding(date.getSeconds())]
    .join(':');
  return str;
}

function padding(str) {
  return ('0' + str).slice(-2);
}

function toFormattedDateAndTime(milliseconds) {
  const str = toFormattedDate(milliseconds);
  str += ' ';
  str += toFormattedTime(milliseconds);
  return str;
}

function getServiceUrl() {
  return getBaseUrl(1);
}

function getBaseUrl(depth) {
  const u = parseUri(document.URL);
  const urlPrefix = u.protocol + "://" + u.authority + "/" + u.directory.split("/")[depth] + "/";
  return urlPrefix;
}


/*
window.onerror = function (msg, file, line, col, error) {
  StackTrace.fromError(error)
    .then(function (stackFrames) {
      const errorMsg = msg + '\n' + stackFrames.map(function (sf) {
        return sf.toString();
      }).join('\n');
      console.error(errorMsg);
      sendLogAux(msg, "ERROR", stackFrames[0]);
    })
    .catch(function (stackFrames) {
      const errorMsg = msg + "\n" + stackFrames.toString();
      console.log(errorMsg);
    });
};
*/

function sendError(msg) {
  sendLog(msg, "ERROR", 4);
}

function sendWarn(msg) {
  sendLog(msg, "WARN", 4);
}

function sendInfo(msg) {
  sendLog(msg, "INFO", 4);
}

function sendDebug(msg) {
  sendLog(msg, "DEBUG", 4);
}

function getDeviceInfo() {
  const ua = new UAParser().getResult();
  return { browser: ua.browser, os: ua.os, device: ua.device };
}

function sendLog(msg, logLevel, stackNum) {
  const st = StackTrace.getSync();
  sendLogAux(msg, logLevel, st[4]);
}

function sendLogAux(msg, logLevel, stackTrace) {
  setTimeout(function () {
    new JsonRpcClient(new JsonRpcRequest(getServiceUrl(), "sendLog",
      [logLevel, stackTrace, { message: msg, device: getDeviceInfo() }, ""], function (data) {
      })).rpc();
  }, 10);
}


function swapView(cells) {
  const swap = new Array(cells.length);
  for (let i = 0; i < cells.length; i++) {
    swap[cells.length - 1 - i] = cells[i].reverse();
  }
  return swap;
}

function getCurrentDate() {
  return new Date().toJSON().slice(0, 10);
}


function stringifyEvent(e) {
  const obj = {};
  for (let k in e) {
    obj[k] = e[k];
  }
  return JSON.stringify(obj, (k, v) => {
    if (v instanceof Node) return 'Node';
    if (v instanceof Window) return 'Window';
    return v;
  }, ' ');
}

function toInt(text) {
  const sTxt = new String(text);
  const rText = text.replace(/\s+/g, "").replace(/[－ ー]/g, "-").replace(
    /[Ａ-Ｚａ-ｚ０-９]/g, function (s) {
      return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
    });
  try {
    const iText = parseInt(rText);
    if (Number.isNaN(iText)) { return false; }
    return iText;
  } catch (e) {
    return false;
  }
}
