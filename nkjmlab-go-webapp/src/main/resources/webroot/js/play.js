const dateNow = Date.now();

let gameState = createGameState();
let gameBoard;
let ws;

function getConnection() {
  return ws.getConnection();
}

function getSessionId() {
  return ws.getSessionId();
}

$(function () {
  if (!getUserMode()) {
    setUserMode(STUDENT);
  }

  if (getGameIdFromUrl() != null) {
    setGameId(getGameIdFromUrl());
    setProblem(null);
    refreshProblemInfo();
    history.replaceState('', '', "play.html");
  }

  if (getProblemIdFromUrl() != null) {
    loadProblemOnMyBoard(getProblemIdFromUrl(), function (data) {
      refreshWithProblemOnMyBoard(data.result);
      history.replaceState('', '', "play.html");
    });
  }

  if (!getGameId() || getGameId() == null) {
    setGameId(getUserId());
  }

  if (!getRank()) {
    setRank(30);
  }

  const userId = getUserId();

  if (getSeatId() == null) {
    requireSignin();
  }


  gameBoard = new GameBoard();
  ws = new PlayWebSocket();
  ws.startNewWsConnection(gameBoard);
  updateCurrentUrl();
  const noScroll = function (e) {
    e.preventDefault();
  }

  $('[data-bs-toggle="popover"]').popover();

  if (isStudent()) {
    $(".teacher, .creator").hide();
    $(".student, .student-only").show();
  } else if (isTeacher()) {
    $(".student-only, .creator").hide();
    $(".student, .teacher").show();
  } else if (isCreator()) {
    $(".student, .student-only, .teacher").hide();
    $(".creator").show();
  }
});

function refreshWaitingRequestFragment() {
  if (!isStudent()) { return; }
  refreshWaitingRequestFragmentAux();
  function refreshWaitingRequestFragmentAux() {
    $.get("./fragment/waiting-request-table-small.html?userId=" + getUserId(),
      function (data) {
        $('#btn-find-opponent').prop("disabled", true);
        $("#tbl-waiting-requests-wrapper").html(data);
        const gameLink = $(data).find(".game-link").attr('data-url');
        if (!gameLink) {
          return;
        }
        swalAlert("次の対局", gameLink.split("=")[1], "info",
          function (e) {
            new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
              "exitWaitingRoom", [getUserId()], function (data) {
                location.href = gameLink;
              })).rpc();
          });

      });
  }
}

function refreshQuestionFragment() {
  if (!isTeacher()) { return; }
  refreshQuestionFragmentAux();
  function refreshQuestionFragmentAux() {
    $.get("./fragment/question-table-small.html", function (data) {
      $("#tbl-q-requests-wrapper").html(data);
    });
  }
}

function requireSignin() {
  setLoginDate(null);
  setGoogleLoginDate(null);
  firebaseSignOut(function () {
    location.href = "index.html";
  });
}

$(function () {
  const userId = getUserId();

  $("#signup-seatid").val(getSeatId() ? getSeatId() : null);
  $("#signup-username-stdid").val(getUserId() ? getUserId() : 5518000);
  $("#signup-username-stdid").prop("disabled", true);
  $("#signup-username-name").val(getUserName());
  $("#signup-username-name").prop("disabled", true);
  $("#signup-rank").val(getRank());
  $(".last-google-login-date").text(getGoogleLoginDate() ? getGoogleLoginDate() : "なし");
  $("#span-current-user-id").text(getUserId());
  $("#span-current-user-icon").html(createImageTag(getUserId()));

  $('#modal-records').on('show.bs.modal', function () {
    $("#current-komi-wrapper").hide();
    $.get("./fragment/game-record-table.html?userId=" + getUserId(),
      function (data) {
        $("#tbl-game-record-wrapper").html(data);
      });
  });

  $("#btn-calc-komi").on('click', function (e) {
    if ($("#current-komi-wrapper").is(':visible')) {
      $("#current-komi-wrapper").hide();
    } else {
      const client = new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
        "getKomi", [getGameId()], function (data) {
          $("#current-komi").html(data.result);
          $("#current-komi-wrapper").show();
        })).rpc();
    }
  });

  $("#btn-attendance").on('click', function (e) {
    swalInput('座席番号 <i class="fas fa-chair"></i>', "座席番号を半角数字で入力して下さい．<br>座席番号がない場合は0を入力して下さい．", getSeatId() ? getSeatId() : 0, "", function (_seatId) {
      if (!_seatId) {
        return;
      }
      const seatId = getFormattedSeatId(_seatId);
      if (!seatId) {
        swalAlert("入力エラー", (!seatId ? "座席番号" : "") + "に無効な値が入力されました", "error");
        return;
      }
      new JsonRpcClient(new JsonRpcRequest(getAuthRpcServiceUrl(), "registerAttendance", [getUserId(), seatId], function () {
        setSeatId(seatId);
        location = "play.html";
      })).rpc();
    });
  });

  $("#btn-close-komi").on('click', function (e) {
    $("#current-komi-wrapper").hide();
  });
  $("#btn-draw-stone-number").on('click', function (e) {
    setDrawStoneNumber(!isDrawStoneNumber());
    gameBoard.prepareAndRepaint(getCellNum());
  });

  $("#btn-page-reload").on('click', function (e) {
    location.reload();
  });

  $("#btn-logout").on('click', function (e) {
    requireSignin();
  });

  $("form input").on('keypress', function (ev) {
    if ((ev.which && ev.which === 13) || (ev.keyCode && ev.keyCode === 13)) {
      ev.preventDefault();
    }
  });

  $(window).on('mousedown touchstart', e => {
    document.getElementById("audio-tap").muted = false;
  });

  $(window).on('resize', function (e) {
    let resizeTimer;
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function () {
      gameBoard.prepareAndRepaint(getCellNum());
    }, 250);

  });

  $('#to-student-mode').on('click', function (e) {
    setUserMode(STUDENT);
    location.reload();
  });
  $('#to-teacher-mode').on('click', function (e) {
    setUserMode(TEACHER);
    location.reload();
  });
  $('#to-creator-mode').on('click', function (e) {
    setUserMode(CREATOR);
    location.reload();
  });

  $('.link-open-practice-board').on(
    'click',
    function (e) {
      promptInputUserId("碁盤を開く", "碁盤IDを入力して下さい", getGameId(),
        getGameId(), function (uid) {
          setGameId(uid);
          setProblem(null);
          location.reload();
          return;
        });
    });

  $('.link-open-my-practice-board').on('click', function (e) {
    loadMyBoard();
    return;
  });

  $('#btn-go-back').on('click', function () {
    goBack(gameState);
  });

  $('#btn-initialize-board, #btn-initialize-board-creator').on('click',
    function (e) {
      $('#new-game-modal').modal('hide');
      const ro = $("input[name=ro]:checked").val();
      const roLabel = $("input[name=ro]:checked+label").text();
      setCellNum(ro);
      gameState.cells = new Array(ro);
      for (let i = 0; i < ro; i++) {
        gameState.cells[i] = new Array(ro).fill(0);
      }
      sendNewGame(gameState, ro, function () {
        setProblem(null);
        initView();
        refreshProblemInfo();
        gameBoard.prepareAndRepaint(getCellNum());
      });
    });

  $('#btn-initialize-problem').on(
    'click',
    function (e) {
      const CELL_NUM = getCellNum();
      swalConfirm("", "盤面の石を全て取り除きますか？", "warning", function () {
        gameState.cells = new Array(CELL_NUM);
        for (let i = 0; i < CELL_NUM; i++) {
          gameState.cells[i] = new Array(CELL_NUM).fill(0);
        }
        gameState.handHistory = null;
        gameState.symbols = {};
        new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
          "sendGameState", [getGameId(), gameState],
          function (data) {
            loadMyBoard();
          })).rpc();
      });
    });

  $("#btn-hand-history-expand").on('click', function () {
    $("#btn-hand-history-expand").hide();
    $("#btn-hand-history-compress").show();
    ws.repaintHandHistoryLog();
  });
  $("#btn-hand-history-compress").on('click', function () {
    $("#btn-hand-history-expand").show();
    $("#btn-hand-history-compress").hide();
    ws.repaintHandHistoryLog();
  });

  $("#btn-hand-history-global-expand").on('click', function () {
    $("#btn-hand-history-global-expand").hide();
    $("#btn-hand-history-global-compress").show();
    expandHandGlobalHistory();
  });
  $("#btn-hand-history-global-compress").on('click', function () {
    $("#btn-hand-history-global-expand").show();
    $("#btn-hand-history-global-compress").hide();
    compressHandGlobalHistory();
  });

  $("#btn-waiting-room").on('click', function () {
    if (isStudent()) {
      swalConfirm("対局待ち部屋 (教員向け)", "受講者は「募集」ボタンを使って下さい", "", function () {
        location.href = "waiting-room.html?userId=" + getUserId();
      });
    } else {
      location.href = "waiting-room.html";
    }
  });

  {
    const problem = getProblem();
    if (problem != null) {
      refreshProblemInfo();
      new ClipboardJS('#btn-problem-url');
      $("#input-problem-url").val(
        "[Web碁盤へ " + location.href + "&problem_id=" + problem.problemId
        + "]");
    }
  }

  $("#link-reset-localdata").on('click', function () {
    swalConfirm("初期化", "ローカルデータを初期化しますか？", "warning", function () {
      clearLocalStorage();
      location.href = "play.html";
    });
  });

  $("#btn-load-next-std, #btn-load-prev-std").on(
    'click',
    function () {
      const mName = $(this).attr('id') == "btn-load-next-std"
        ? "getNextUser" : "getPrevUser";
      new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), mName,
        [getGameId()], function (data) {
          if (data.result.length == 0) { return; }
          setGameId(data.result);
          refreshWindow();
        })).rpc();
    });

  $("#btn-load-next-game, #btn-load-prev-game").on(
    'click',
    function () {
      const bid = $(this).attr('id');
      new JsonRpcClient(
        new JsonRpcRequest(getGoRpcServiceUrl(),
          bid == "btn-load-next-game" ? "getNextGame"
            : "getPrevGame", [getGameId()], function (
              data) {
          if (data.result.length == 0) { return; }
          setGameId(data.result);
          refreshWindow();
        })).rpc();
    });

  $('#btn-del-problem-on-server').on(
    'click',
    function (event, data) {
      if (!getProblem()) {
        swalAlert('', '問題が選ばれていません', "info", function () {
        });
        return;
      }

      swalConfirm("削除", "「" + getProblem().groupId + " "
        + getProblem().name + "」を削除してよろしいですか？", "error",
        function (e) {
          new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
            "deleteProblem", [getProblem().problemId],
            function () {
              setProblem(null);
              location.reload();
              return;
            })).rpc();
        });
    });

  $(".btn-load-next-problem, .btn-load-prev-problem").on('click', function () {
    if (gameState.problemId == -1) {
      $('#load-problem-modal').modal('show');

    }
    const o = $(this).prop("class").split("-").includes("next") ? 1 : -1;
    for (let i = 0; i < problems.length; i++) {
      if (o == 1 && i == problems.length - 1) {
        break;
      }
      if (problems[i].problemId == gameState.problemId) {
        loadProblemOnMyBoard(problems[i + o].problemId, function (data) {
          refreshWithProblemOnMyBoard(data.result);
        });
        return;
      }
    }
    $('#load-problem-modal').modal('show');
  });

  $('#problem-tree').on('nodeSelected', function (event, data) {
    if ($('input:radio[name="problem-tree-radio"]:checked').val() == 0) {
      loadProblemOnMyBoard(data.problemId, function (data) {
        $('#load-problem-modal').modal('hide');
        refreshWithProblemOnMyBoard(data.result);
      });
    } else {
      loadProblemOnCurrentBoard(data.problemId, function (data) {
        $('#load-problem-modal').modal('hide');
        refreshWindow();
      });
    }
  });
});

$(function () {
  function sendTalk(stone, prefix, suffix) {
    swalTextArea('トーク送信 <i class="fas fa-code ms-2"></i>', "HTMLタグが利用できます", "",
      "", function (input) {
        if (!input || input === 'false') { return; }
        setTimeout(function () {
          sendGameStateWithLastHand(getConnection(), gameState, {
            type: "message",
            stone: stone,
            options: prefix + input + suffix
          });
        }, 250);
      });
  }

  $('#btn-send-msg').on('click', function (e) {
    if (isStudent()) {
      sendTalk(getMyStoneColor(), getUserName() + "： ", "");
    } else {
      sendTalk(0, getUserName() + "： ", "");
    }
  });

  $('#btn-pass').on('click', function (e) {
    swalConfirm('<i class="far fa-comment"></i>', "パスします", "", function (e) {
      setTimeout(function () {
        sendGameStateWithLastHand(getConnection(), gameState, {
          type: "pass",
          stone: getMyStoneColor()
        });
      }, 800);
    });
  });

  $('#btn-give-up').on('click', function (e) {
    swalConfirm('<i class="far fa-comment"></i>', "投了します", "", function (e) {
      setTimeout(function () {
        sendGameStateWithLastHand(getConnection(), gameState, {
          type: "giveUp",
          stone: getMyStoneColor()
        });
      }, 800);
    });
  });
});

$(function () {
  $('#btn-vote-calc').on(
    'click',
    function (e) {
      new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "getVoteResult",
        [getProblem() == null ? -1 : getProblem().problemId,
        getGameId()], function (data) {
          $(".vote-result").text("");
          data.result.forEach(function (e) {
            $("#" + e.voteId + ">.vote-result").text(e.numOfVote);
          });
          $("#btn-vote-title").trigger("click");
        })).rpc();
    });

  $('.btn-vote').on(
    'click',
    function (e) {
      let vote;
      let voteId;

      function getVote() {
        return vote;
      }

      function getVoteId() {
        return voteId;
      }
      function sendGameStateAndVote(connection, gameState) {
        if (!gameState.options) {
          gameState.options = {
            "vote": getVote(),
            "voteId": getVoteId()
          };
        } else {
          gameState.options.vote = getVote();
          gameState.options.voteId = getVoteId();
        }
        sendGameStateWithLastHand(connection, gameState, {
          type: "vote",
          stone: VOTE_MAP[getVote()]
        });
      }

      let btn = $(this);
      let val = $(this).text().trim();
      let valId = $(this).attr("id");
      if (valId == "btn-vote-none") {
        vote = "blank";
        voteId = "blank";
        new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "vote", [
          getGameId(), getUserId(),
          getProblem() == null ? -1 : getProblem().problemId,
          getVote(), getVoteId()], function (data) {
            sendGameStateAndVote(getConnection(), gameState);
          })).rpc();
        return;
      }

      swalConfirm("", val + "に投票しますか？", "", function (data) {
        vote = val;
        voteId = valId;
        new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "vote", [
          getGameId(), getUserId(),
          getProblem() == null ? -1 : getProblem().problemId,
          getVote(), getVoteId()], function (data) {
            sendGameStateAndVote(getConnection(), gameState);
          })).rpc();
      });
    });
});

$(function () {
  $(".btn-hand-down").on(
    'click',
    function () {
      swalConfirm('<i class="fas fa-check"></i>', "質問を対応済みにします", "",
        function (e) {
          handDown();
        });
    });

  function handDown() {
    $("#btn-hand-up").show();
    $(".btn-hand-down").hide();
    new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "handUp", [getGameId(),
      false, ""], function (data) {
        let html = getUserName() + "： "
          + '質問を <span class="hanko">済</span> にしました';
        sendGameStateWithLastHand(getConnection(), gameState, {
          type: "message",
          stone: isStudent() ? getMyStoneColor() : 0,
          options: html
        });
      })).rpc();
  }
});

$(function () {
  $("#btn-copy-url").on('click', function (e) {
    let text = copyBoardUrl();
    alert("盤面URLをクリップボードにコピーしました");
  });
  function copyBoardUrl() {
    $("#input-current-gameboard-url").select();
    document.execCommand("copy");
    return $("#input-current-gameboard-url").val();
  }
});

$(function () {
  $('#btn-open-game').on(
    'click',
    function (e) {
      setupPlayBoard($("#open-game-modal .input-black-player-id").val(),
        $("#open-game-modal .input-white-player-id").val(), null);
      refreshWindow();
      $("#open-game-modal").modal('hide');
    });

  function setupPlayBoard(blackPlayerId, whitePlayerId, problem) {
    if (!getFormattedStdId(blackPlayerId)) {
      swalAlert("入力エラー", "先手にID(半角数字)が入力されていません", "error");
    }
    if (!getFormattedStdId(whitePlayerId)) {
      swalAlert("入力エラー", "後手にID(半角数字)が入力されていません", "error");
    }
    if (getFormattedStdId(blackPlayerId) && getFormattedStdId(whitePlayerId)) {
      _setGameIdWithPlayers(blackPlayerId, whitePlayerId);
    }
    setProblem(problem);
    refreshProblemInfo();
    return;
  }

});

$(function () {
  $("#btn-hand-up").on('click', function () {
    startQuestion();
  });

  function handUp(msg) {
    new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "handUp", [getGameId(),
      true, msg], function (data) { })).rpc();
  }

  function startQuestion() {
    swalInput('質問 <i class="far fa-comment ms-2"></i>', "質問内容を記入して下さい", "",
      "例: 終局しているか教えて下さい．", function (input) {
        if (!input || input === 'false') { return; }
        let qt = getUserName() + '： 質問  <i class="far fa-hand-paper"></i>  「'
          + input + "」";

        $("#btn-hand-up").hide();
        $(".btn-hand-down").show();

        setTimeout(function () {
          handUp(qt);
        }, 400);

        setTimeout(function () {
          sendGameStateWithLastHand(getConnection(), gameState, {
            type: "message",
            stone: getMyStoneColor(),
            options: qt
          }, 300);
        }, 800);
      });
  }
});

$(function () {
  const emojiArea = $("#input-short-talk").emojioneArea(
    {
      shortcuts: false,
      autocomplete: false,
      tones: false,
      filtersPosition: "bottom",
      search: false,
      inline: true,
      hideSource: true,
      placeholder: "この碁盤を見ている人にメッセージを送ります",
      events: {
        emojibtn_click: function (button, event) {
          emojiArea.hidePicker();
        },
        keyup: function (editor, event) {
          if (event.which == 13
            && ($.trim(editor.text()).length > 0 || $.trim(editor
              .html()).length > 0)) {
            $('#btn-short-talk').trigger('click');
            event.preventDefault();
            event.stopPropagation();
            editor.focus();
          }
        }
      }
    }).data("emojioneArea");

  $('#btn-short-talk').on('click', function (e) {
    let input = emojiArea.getText();

    if (!input || input.length == 0) { return; }

    setTimeout(function () {
      sendGameStateWithLastHand(getConnection(), gameState, {
        type: "message",
        stone: isStudent() ? getMyStoneColor() : 0,
        options: getUserName() + "： " + input
      });
      emojiArea.setText("");
    }, 100);

  });
});

$(function () {
  const emojiArea = $("#input-short-talk-global").emojioneArea(
    {
      shortcuts: false,
      autocomplete: false,
      tones: false,
      filtersPosition: "bottom",
      search: false,
      inline: true,
      hideSource: true,
      placeholder: "全体にメッセージを送ります",
      events: {
        emojibtn_click: function (button, event) {
          emojiArea.hidePicker();
        },
        keyup: function (editor, event) {
          if (event.which == 13
            && ($.trim(editor.text()).length > 0 || $.trim(editor
              .html()).length > 0)) {
            $('#btn-short-talk-global').trigger('click');
            event.preventDefault();
            event.stopPropagation();
            editor.focus();
          }
        }
      }
    }).data("emojioneArea");

  $('#btn-short-talk-global').on(
    'click',
    function (e) {
      const input = emojiArea.getText();
      if (!input || input.length == 0) { return; }

      setTimeout(function () {
        const msg = getUserId() + " (" + getUserName() + ") " + input;
        new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
          "sendGlobalMessage", [getUserId(), msg], function () {
          })).rpc();
        emojiArea.setText("");
      }, 100);
    });
});

$(function () {
  $("#btn-find-opponent").on(
    'click',
    function () {
      swalConfirm("募集", '対局相手を探します', null, function () {
        new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
          "enterWaitingRoom", [getUserId()],
          function (data) {
            $("#btn-find-opponent").prop("disabled", true);
            refreshWaitingRequestFragment();
          })).rpc();
      });
    });
  $("#btn-exit-room").on(
    'click',
    function () {
      swalConfirm("警告", "募集を取り消します．教員の指示がある時のみ使って下さい", "warning",
        function () {
          new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
            "exitWaitingRoom", [getUserId()], function (data) {
              location.reload();
            })).rpc();
        });
    });
});
function initView() {
  let initViewTimer;

  document.title = getUserId() + " | Go";

  $("#current-komi-wrapper").hide();
  $("." + PRACTICE + "-mode" + ", ." + PLAY + "-mode").hide();
  $("." + getGameMode() + "-mode").show();
  $("input[name=ro]").val([getCellNum()]);

  let gameId = getGameId();
  if (getGameMode() == PRACTICE) {
    updatePlayerLabel("practice", getGameId());
  } else {
    updatePlayerLabel("black", getBlackPlayerId());
    updatePlayerLabel("white", getWhitePlayerId());
  }
  if (getGameMode() != PLAY) {
    $("#btn-calc-komi").hide();
  }
  $(".input-black-player-id").val(
    getBlackPlayerId() ? getBlackPlayerId() : gameId);
  $(".input-white-player-id").val(
    getWhitePlayerId() ? getWhitePlayerId() : gameId);

  const vGames = [];
  for (let i = getVisitedGameIds().length - 1; i > getVisitedGameIds().length - 4; i--) {
    if (i < 0) {
      break;
    }
    vGames.push($("<span>")
      .append(
        $("<a>").addClass("btn btn-sm btn-light").attr("href",
          "play.html?game_id=" + getVisitedGameIds()[i])
          .html(
            ' <i class="far fa-handshake"></i> '
            + getVisitedGameIds()[i])).html());
  }
  $("#list-game-history").empty();
  vGames.forEach(function (e) {
    $("#list-game-history").append(e);
  });

  clearTimeout(initViewTimer);

  initViewTimer = setTimeout(function () {
    initView();
  }, 60 * 60 * 1000);

  function updatePlayerLabel(selector, uid) {
    $("." + selector + "-player-id").val(uid);
    $("#" + selector + "-player-icon").html(createImageTag(uid));

    $("." + selector + "-player-id-label").html(uid);

    new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "getUser", [uid],
      function (data) {
        let target = $("." + selector + "-player-id-label");
        target.html(uid
          + " ("
          + (data.result.seatId ? '<i class="fas fa-chair"></i> ' + data.result.seatId + ". " : "")
          + (data.result.userName ? data.result.userName + " " : "") + (data.result.rank ? data.result.rank + "級 " : "")
          + (data.result.attendance ? '<span class="badge bg-info">出</span>' : '<span class="badge bg-danger">欠</span>') + ")");
      })).rpc();

  }
}

function refreshProblemInfo() {
  let problem = getProblem();
  if (problem == null || getGameMode() == PLAY || problem.problemId == 0) {
    $("#div-problem-id").text("");
    $("#div-problem-group").text("");
    $("#div-problem-name").text("");
    $(".problem-info").hide();
    $(".insert-problem .modal-problem-id").val("");
    $(".insert-problem .modal-problem-group").val("");
    $(".insert-problem .modal-problem-name").val("");
    $(".insert-problem .modal-problem-message").text("");
    $("#game-state-message").text("");
    return;
  }

  $(".problem-info").show();

  $(".insert-problem .modal-problem-id").val(problem.problemId);
  $(".insert-problem .modal-problem-group").val(problem.groupId);
  $(".insert-problem .modal-problem-name").val(problem.name);
  $(".insert-problem .modal-problem-message").text(problem.message);

  $("#div-problem-id").text(problem.problemId);
  $("#div-problem-group").text(problem.groupId);
  $("#div-problem-name").text(problem.name);
  $("#game-state-message").html(problem.message);
}

function updateCurrentUrl() {
  let url = new URL(location).href;
  url = url.indexOf("?") == -1 ? url : url.slice(0, url.indexOf("?"));
  $("#input-current-gameboard-url").val(url + "?game_id=" + getGameId());
}

function refreshWithProblemOnMyBoard(p) {
  const userId = getUserId();
  setProblem(p);
  refreshProblemInfo();
  if (getGameId() == userId) {
    setGameId(userId);
  } else {
    loadMyBoard();
  }

}

function loadMyBoard() {
  setGameId(getUserId());
  updateCurrentUrl();
  syncGameState(getSessionId());
  vote = null;
  voteId = null;
}

function refreshWindow() {
  setProblem(null);
  refreshProblemInfo();
  syncGameState(getSessionId());
  updateCurrentUrl();
}

function expandHandGlobalHistory() {
  $("#hand-history-log-global li").each(function (i, e) {
    $(e).show();
  });
}
function compressHandGlobalHistory() {
  $("#hand-history-log-global li").each(function (i, e) {
    if (i < $("#game-board").width() / 60) {
      $(e).show();
    } else {
      $(e).hide();
    }
  });
}
