class PlayWebSocket {

  constructor() {
    this.connection = null;
    this.sessionId = null;
    this.initialized = false;
  }

  getSessionId() {
    return this.sessionId;
  }

  getConnection() {
    return this.connection;
  }


  startNewWsConnection(gameBoard) {
    const self = this;

    let connection = createConnection(getUserId(), getGameId(), getIdToken());
    this.connection = connection;

    $(window).on('unload', function () {
      if (connection) {
        connection.onclose = function () {
        }
        connection.close();
      }
    });


    connection.onmessage = e => {
      const json = JSON.parse(e.data);
      switch (json.method) {
        case "REQUEST_TO_LOGIN":
          _requestToLogin(json.content);
          break;
        case "INIT_SESSION":
          _initSession(json.content);
          _updateQuestionsTable();
          break;
        case "UPDATE_HAND_UP_TABLE":
          _updateQuestionsTable();
          break;
        case "UPDATE_WAITING_REQUEST_STATUS":
          _updateWaitingRequestStatus();
          break;
        case "GAME_STATE":
          _gameState(json.content);
          break;
        case "GLOBAL_MESSAGE":
          _globalMessage(json.content);
          break;
        case "ENTRIES":
          _entries(json.content);
          break;
        case "HAND_UP":
          _handUp(json.content);
          break;
        case "HAND_UP_ORDER":
          _handUpCount(json.content);
          break;
        default:
          console.error("invalid method name =>" + json.method);
      }
      function _handUpCount(handUpOrder) {
        $("#order-of-question").text(handUpOrder);
      }
      function _handUp(handUp) {
        if (handUp.handUp) {
          $("#btn-hand-up").hide();
          $(".btn-hand-down").show();
          $("#hand-question-msg").show();
          $("#order-of-question").text(handUp.order);
        } else {
          $("#btn-hand-up").show();
          $(".btn-hand-down").hide();
          $("#hand-question-msg").hide();
        }
      }
      function _globalMessage(json) {
        if (json == 0) { return; }
        const area = $("#hand-history-log-global");
        json.forEach(function (msg) {
          area.prepend($('<li>').append(
            $('<span>').html(
              '<i class="far fa-comments"></i> '
              + msg)));
        });
        if ($("#btn-hand-history-global-compress").css('display') == 'none') {
          compressHandGlobalHistory();
        } else {
          expandHandGlobalHistory();
        }
      }

      function _initSession(json) {
        self.sessionId = json.sessionId;
        const u = json.user;
        setUserId(u.userId);
        setSeatId(u.seatId);
        setUserName(u.userName);
        setRank(u.rank);
        $(".current-user-icon").html(createImageTag(u.userId));
        $(".current-user-info").html(u.userId + " (" + '<i class="fas fa-chair"></i> ' + u.seatId + ")");
      }

      function _requestToLogin(userId) {
        if (getUserId() == userId) {
          requireSignin();
        }
      }

      function _updateQuestionsTable() {
        refreshQuestionFragment();
      }
      function _updateWaitingRequestStatus() {
        refreshWaitingRequestFragment();
      }

      function _entries(entries) {
        const loginUsers = [];
        const ulimit = 5;
        const us = Math.min(entries.length, ulimit);
        for (let i = 0; i < us; i++) {
          const u = entries[i];
          const tmpu = $('<span>').append(
            $(createImageTag(u.userId))).append(
              $('<span>').addClass("player-info").text(u.userId));
          if (u.userId == getUserId()) {
            tmpu.addClass("font-weight-bold font-italic");
          }
          loginUsers.push(tmpu);
        }
        if (entries.length > ulimit) {
          loginUsers.push('<span class="player-info"> and '
            + (entries.length - ulimit) + ' players </span>')
        }
        $("#login-users").empty();
        loginUsers.forEach(function (e) {
          $("#login-users").append(e);
        });

      }

      function _gameState(json) {
        gameState = json;
        $(".btn-vote").removeClass("btn-danger active").addClass("btn-primary");
        $("#btn-vote-none").removeClass("btn-primary").addClass("btn-light");
        if (gameState.options["vote"] && gameState.options["voteId"]) {
          $("#" + gameState.options["voteId"]).removeClass("btn-primary").addClass(
            "btn-danger active");
        }

        if (gameState.cells[0].length != getCellNum() || gameBoard.CELL_NUM != getCellNum()) {
          setCellNum(gameState.cells[0].length);
          initView();
          refreshProblemInfo();
          gameBoard.prepareAndRepaint(getCellNum());
        }

        if (!self.initialized) {
          initView();
          refreshProblemInfo();
          gameBoard.prepareAndRepaint(getCellNum());
          gameBoard.bindEventOnCanvas(connection);
          $("#spinner-loading-wrapper").hide();
          self.initialized = true;
        }

        if (gameState.problemId == -1) {
          setProblem(null);
          refreshProblemInfo();
        } else {
          if (getGameMode() == PLAY) {
            setProblem(null);
            refreshProblemInfo();
          } else if (getProblem() == null
            || getProblem().problemId != gameState.problemId) {
            new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "getProblem",
              [gameState.problemId], function (data) {
                setProblem(data.result);
                refreshProblemInfo();
              })).rpc();
          }
        }
        gameBoard.repaintBoard();
        self.repaintHandHistoryLog();
      }

    };

    connection.onopen = e => {
      console.log("connection is open.");
      // console.log(stringifyEvent(e));
    };

    connection.onerror = e => {
      console.error("connection has an error.");
      swalAlert("ページを再読み込みします", "", "info", e => location.reload());
    };


    connection.onclose = e => {
      console.warn("connection is closed.");
      setTimeout(() => self.startNewWsConnection(gameBoard), 500);
    };
  }


  _getCommentClass(msg) {
    if (msg.startsWith("黒")) {
      return "log-comment-black";
    } else if (msg.startsWith("白")) {
      return "log-comment-white";
    } else {
      return "log-comment-others";
    }
  }

  _getPrefix(hand) {
    const BPRE = '黒● ';
    const WPRE = '白○ ';

    switch (hand.type) {
      case AGEHAMA:
      case FROM_AGEHAMA: {
        return hand.stone == BLACK_STONE ? WPRE : (hand.stone == WHITE_STONE ? BPRE : '')
      }
      default: {
        return hand.stone == BLACK_STONE ? BPRE : (hand.stone == WHITE_STONE ? WPRE : '')
      }
    }
  }
  _getMessage(hand) {
    switch (hand.type) {
      case HAND_TYPE_MESSAGE:
        return " " + hand.options;
      case "vote":
        return hand.stone == "0" ? "投票を削除" : STONES_MAP[hand.stone] + "に投票";
      case "pass":
        return "パス";
      case "giveUp":
        return "投了";
      case AGEHAMA:
        return "アゲハマ (+1)";
      case FROM_AGEHAMA:
        return "アゲハマ (-1)";
      case "onBoard":
      case HAND_TYPE_PUT_ON_BOARD:
        return "(" + (hand.x + 1) + "," + numToKansuji(hand.y) + ")";
      case REMOVE_FROM_BOARD:
        return "トル (" + (hand.x + 1) + "," + numToKansuji(hand.y) + ")";
      default:
        console.log(hand.type);
        return "";
    }
  }

  repaintHandHistoryLog() {

    $("#hand-history-log").empty();
    const area = [];

    const handHistorySize = gameState.handHistory.length;

    let i = 0;



    const limit = $("#btn-hand-history-compress").css('display') == 'none' ? Math.max(handHistorySize - 14, 0) : 0;

    for (let j = 0; j < handHistorySize; j++) {
      const hand = gameState.handHistory[j];

      if (hand.type == "loadGameState") { continue; }

      if (hand.type == HAND_TYPE_PUT_ON_BOARD || hand.type == "pass" || hand.type == "giveUp") {
        i++;
      }

      if (j < limit) {
        continue;
      }

      const prefix = this._getPrefix(hand);
      const msg = this._getMessage(hand);

      if (hand.type == REMOVE_FROM_BOARD || hand.type == AGEHAMA || hand.type == FROM_AGEHAMA) {
        area.push('<li class="text-left"><span>' + i + ". " + prefix + msg + '</span></li>');
      } else if (hand.type == HAND_TYPE_PUT_ON_BOARD || hand.type == "onBoard" || hand.type == "pass" || hand.type == "giveUp") {
        area.push('<li class="text-right"><span>' + i + ". " + prefix + msg + '</span></li>');
      } else {
        const _msg = msg.replace('\n', '<br>');
        const msg1 = _msg.substring(0, _msg.indexOf("：") + 1);
        const msg2 = _msg.substring(_msg.indexOf("：") + 1);
        area.push($('<li>').addClass("text-left")
          .append(
            $('<span>').html(
              '<i class="far fa-comments"></i> ' + prefix + msg1))
          .append(
            $('<span>').addClass("log-comment").addClass(
              this._getCommentClass(msg)).html(msg2)));
      }

    }
    $("#hand-history-log").append(area.reverse());

  }
}

function createConnection(userId, gameId, idToken) {
  const wsUrl = getWebSocketBaseUrl() + "?userId=" + userId + "&gameId=" + gameId + "&idToken=" + idToken;
  //console.log("start open websocket = [" + wsUrl + "]");
  return new WebSocket(wsUrl);
}
