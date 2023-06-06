$(function() {
  saveOnServer();

  function saveOnServer() {
    var btnSelector = '#btn-save-game-state-on-server, #btn-save-new-game-state-on-server';
    var modalSelector = "#save-game-state-on-server-modal";
    $(btnSelector).on(
            'click',
            function(e) {
              var modal = modalSelector + " ";
              if ($(modal + ".modal-problem-name").val().length == 0
                      || $(modal + ".modal-problem-group").val().length == 0) {
                alert("必須項目が入力されていません");
                return;
              }

              var btnId = $(this).attr('id');
              swalConfirm("確認", "保存してよろしいですか？", "warning", function(data) {
                var problemId = btnId == 'btn-save-new-game-state-on-server'
                        ? -1 : $(modal + ".modal-problem-id").val();
                new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(),
                        "saveProblem", [getGameId(), problemId,
                            $(modal + ".modal-problem-group").val(),
                            $(modal + ".modal-problem-name").val(),
                            $(modal + ".modal-problem-message").val()],
                        function(data) {
                          setProblem(data.result);
                          loadProblemOnMyBoard(data.result.problemId,
                                  function() {
                                    $(modalSelector).modal('hide');
                                    location.reload();
                                  })
                          return;
                        })).rpc();
              });
            });
  }
});

$(function() {
  $('#open-game-state-file').on('change', function() {
    loadJsonFile(this.files);
  });
  $('#btn-open-game-state-file').on('click', function() {
    $('#open-game-state-file').trigger('click');
  });

  function loadJsonFile(files) {
    if (files == null || files.length == 0 || files[0] == null) { return; }
    var file = files[0];
    var fileReader = new FileReader();
    fileReader.onload = function(e) {
      gameState = JSON.parse(e.target.result);
      var problem = {
        problemId: gameState.problemId,
        groupId: gameState.groupId,
        name: gameState.name,
        message: gameState.message
      };
      setProblem(problem);

      setGameStateOptions(gameState);
      sendGameStateWithLastHand(getConnection(), gameState, {
        type: "loadGameState"
      });
      location.reload();
      return;
    };
    fileReader.readAsText(file);
  }
});

$(function() {
  $('#btn-save-game-state-file').on(
          'click',
          function() {
            var modal = "#save-game-state-on-local-modal ";
            gameState.problemId = $(modal + ".modal-problem-id").val();
            gameState.name = $(modal + ".modal-problem-name").val();
            gameState.groupId = $(modal + ".modal-problem-group").val();
            gameState.message = $(modal + ".modal-problem-message").val();
            var content = JSON.stringify(gameState).split("},").join("},\n")
                    .split("],").join("],\n").split(":[").join(":[\n");
            var url = window.URL || window.webkitURL;
            var blob = new Blob([content], {
              "type": "application/json"
            });
            if (window.navigator.msSaveBlob) {
              window.navigator.msSaveBlob(blob, gameState.groupId + "-"
                      + gameState.name + ".json");
            } else {
              $(this).attr("href", url.createObjectURL(blob));
              // $(this).attr("href",
              // 'data:application/octet-stream,' +
              // encodeURIComponent(content));
              $(this).attr("download",
                      gameState.groupId + "-" + gameState.name + ".json");
            }
            $(modal).modal("hide")

            new JsonRpcClient(new JsonRpcRequest(getGoRpcServiceUrl(), "saveProblem", [
                getGameId(), gameState.problemId, gameState.groupId,
                gameState.name, gameState.message], function(data) {
              setProblem(data.result);
              location.reload();
            })).rpc();
          });
});
