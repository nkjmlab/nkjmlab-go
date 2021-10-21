
class GameBoard {

  constructor() {
    this.BOARD_WIDTH = 0;
    this.BOARD_HEIGHT = 0;
    this.STONE_SIZE = 0;
    this.STONES_SIZE = 0;
    this.CELL_NUM = 0;
    this.CELL_SIZE = 0;
    this.TOP_OFFSET = 0;
    this.LEFT_OFFSET = 0;
    this.ctx = null;
    this.drawStoneCounter = 0;
    this.lastHandNum = -1;

    const BLACK_STONE_IMAGE = new Image();
    BLACK_STONE_IMAGE.src = '../img/play/black-stone.png';
    const WHITE_STONE_IMAGE = new Image();
    WHITE_STONE_IMAGE.src = '../img/play/white-stone.png';
    const A_STONE_IMAGE = new Image();
    A_STONE_IMAGE.src = '../img/play/a-stone.png';
    const B_STONE_IMAGE = new Image();
    B_STONE_IMAGE.src = '../img/play/b-stone.png';
    const C_STONE_IMAGE = new Image();
    C_STONE_IMAGE.src = '../img/play/c-stone.png';
    const X_STONE_IMAGE = new Image();
    X_STONE_IMAGE.src = '../img/play/x-stone.png';
    const CIRCLE_STONE_IMAGE = new Image();
    CIRCLE_STONE_IMAGE.src = '../img/play/circle-stone.png';
    const RECTANGLE_STONE_IMAGE = new Image();
    RECTANGLE_STONE_IMAGE.src = '../img/play/rectangle-stone.png';
    const TRIANGLE_STONE_IMAGE = new Image();
    TRIANGLE_STONE_IMAGE.src = '../img/play/triangle-stone.png';

    this.STONE_IMAGES = {
      [BLANK_STONE]: null,
      [BLACK_STONE]: BLACK_STONE_IMAGE,
      [WHITE_STONE]: WHITE_STONE_IMAGE,
      [RECTANGLE_STONE]: RECTANGLE_STONE_IMAGE,
      [TRIANGLE_STONE]: TRIANGLE_STONE_IMAGE,
      [X_STONE]: X_STONE_IMAGE,
      [A_STONE]: A_STONE_IMAGE,
      [B_STONE]: B_STONE_IMAGE,
      [C_STONE]: C_STONE_IMAGE,
      [CIRCLE_STONE]: CIRCLE_STONE_IMAGE
    }
  }

  _isValidCellIndex(x, y) {
    return 0 <= x && x < this.CELL_NUM && 0 <= y && y < this.CELL_NUM;
  }

  _putOn(x, y, stone) {
    if (!stone) { return; }
    if (stone == BLANK_STONE) { return; }
    if (!this._isValidCellIndex(x, y)) { return; }
    gameState.cells[x][y] = stone;
  }

  _putSymbolOnBoard(x, y, symbol) {
    if (!this._isValidCellIndex(x, y)) { return; }
    gameState.symbols[x + "-" + y] = symbol;
  }

  _putOff(gameState, x, y) {
    if (getStoneColor(gameState.cells[x][y]) == BLANK_STONE) {
      gameState.symbols[x + "-" + y] = BLANK_STONE;
    }
    gameState.cells[x][y] = BLANK_STONE;
  }


  prepareAndRepaint(cellNum) {
    this.CELL_NUM = cellNum;
    this._prepare();
    this.repaintBoard();
  }

  _prepare() {
    let BOARD_WIDTH = Math.min($("#game-board-wrapper").width() * 0.9, $(window).height() * 0.75);
    let BOARD_HEIGHT = BOARD_WIDTH;
    let STONES_SIZE = $("#white-stones-wrapper").width();
    $(['#black-stones-cap', '#white-stones-cap'].join(',')).css(
      'background-size', STONES_SIZE + 'px ' + STONES_SIZE + 'px').css(
        'width', STONES_SIZE).css('height', STONES_SIZE);

    let CELL_SIZE = BOARD_WIDTH / (this.CELL_NUM + 1);
    let STONE_SIZE = CELL_SIZE;

    $('#game-board').attr('width', BOARD_WIDTH * 2).attr('height',
      BOARD_HEIGHT * 2).css('width', BOARD_WIDTH + "px").css('height',
        BOARD_HEIGHT + "px");

    $('#moving-black-stone' + "," + '#moving-white-stone').attr(
      "width", CELL_SIZE * 1.05);

    $(['#moving-black-stone', '#moving-white-stone'].join(",")).css({
      "background-size": CELL_SIZE * 0.8 + " " + CELL_SIZE * 0.8,
      "width": CELL_SIZE * 0.8,
      "height": CELL_SIZE * 0.8
    });

    $(['#black-stones-cap', '#white-stones-cap'].join(',')).css(
      'background-size', STONES_SIZE + 'px ' + STONES_SIZE * 0.6 + 'px')
      .css('width', STONES_SIZE).css('height', STONES_SIZE * 0.6);

    this.TOP_OFFSET = CELL_SIZE;
    this.LEFT_OFFSET = CELL_SIZE;
    this.CELL_SIZE = CELL_SIZE;
    this.BOARD_WIDTH = BOARD_WIDTH;
    this.BOARD_HEIGHT = BOARD_HEIGHT;
    this.STONE_SIZE = STONE_SIZE;
    this.STONES_SIZE = STONES_SIZE;
  }

  repaintBoard() {

    const self = this;
    const canvas = $('#game-board')[0];
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.scale(2, 2);
    this.drawStoneCounter = 0;


    this._repaintStonePods(ctx);
    this._repaintBoardArea(ctx);
    this._repaintAxisLabel(ctx);
    this._repaintStars(ctx);
    this._repaintSymbols(ctx);
    this._repaintStones(ctx);
    this._repaintLastHand(ctx);
    this._repaintAgehama();
    if (isDrawStoneNumber()) {
      this._repaintHandNumber(ctx);
    }
    ctx.restore();
    if (this.lastHandNum != gameState.lastHand.number && !document.getElementById("audio-tap").muted) {
      document.getElementById("audio-tap").play();
    }
    this.lastHandNum = gameState.lastHand.number;
  }

  _repaintHandNumber(ctx) {
    ctx.textBaseline = 'middle';
    gameState.handHistory.filter(e => e.type == "putOnBoard" && (e.stone == BLACK_STONE || e.stone == WHITE_STONE)).forEach(e => {
      this.drawStoneCounter++;
      ctx.fillStyle = e.stone == BLACK_STONE ? "#FFFFFF" : "#000000";
      ctx.fillText(this.drawStoneCounter, this.LEFT_OFFSET + this.CELL_SIZE * e.x + this.CELL_SIZE / 5, this.TOP_OFFSET + this.CELL_SIZE * e.y);
    });
  }

  _repaintStonePods(ctx) {
    const stonesSize = this.STONES_SIZE;
    $('#black-stones').css({ 'width': stonesSize, 'height': stonesSize * 0.895 });
    $('#white-stones').css({ 'width': stonesSize, 'height': stonesSize * 0.895 });
  }


  _drawStoneAux(ctx, x, y, stone) {
    if (stone == BLANK_STONE) {
      return;
    }
    const img = this.STONE_IMAGES[stone];
    if (!img) {
      console.error("invalid: " + stone);
    }
    ctx.drawImage(img, this.LEFT_OFFSET / 2 + this.CELL_SIZE * x, this.TOP_OFFSET / 2 + this.CELL_SIZE * y, this.STONE_SIZE,
      this.STONE_SIZE);
  }

  _drawSymbol(ctx, x, y, stone) {
    if (stone == BLANK_STONE) {
      return;
    }
    const img = this.STONE_IMAGES[stone];
    if (!img) {
      console.error("invalid: " + stone);
    }
    ctx.drawImage(img, this.LEFT_OFFSET / 2 + this.CELL_SIZE * x + this.STONE_SIZE * 0.1, this.TOP_OFFSET / 2 + this.CELL_SIZE * y + this.STONE_SIZE * 0.1,
      this.STONE_SIZE * 0.8, this.STONE_SIZE * 0.8);
  }

  _drawStone(ctx, x, y, stone) {
    let img;
    ctx.beginPath();
    this._drawStoneAux(ctx, x, y, getStoneColor(stone));
    this._drawSymbol(ctx, x, y, getStoneSymbol(stone));
  }

  _repaintLastHand(ctx) {
    const hist = gameState.handHistory;
    for (let i = 0; i < hist.length; i++) {
      const hand = hist[hist.length - 1 - i];
      if (hand.type == "putOnBoard") {
        this._highlightHand(ctx, hand);
        break;
      }
    }

  }

  _highlightHand(ctx, hand) {
    ctx.beginPath();
    ctx.fillStyle = 'rgb(25,220,25)';
    ctx.globalAlpha = 0.85;
    ctx.arc(this.LEFT_OFFSET + this.CELL_SIZE * hand.x, this.TOP_OFFSET + this.CELL_SIZE
      * hand.y, this.CELL_SIZE / 8.0, 0, Math.PI * 2, true);
    ctx.fill();
    ctx.globalAlpha = 1.0;
  }

  _repaintBoardArea(ctx) {
    ctx.beginPath();
    ctx.fillStyle = 'rgb(199,159,109)';
    ctx.fillRect(0, 0, this.BOARD_WIDTH, this.BOARD_HEIGHT);
    for (let i = 0; i < this.CELL_NUM - 1; i++) {
      for (let j = 0; j < this.CELL_NUM - 1; j++) {
        ctx.strokeRect(this.LEFT_OFFSET + this.CELL_SIZE * j, this.TOP_OFFSET + this.CELL_SIZE * i, this.CELL_SIZE, this.CELL_SIZE);
      }
    }
  }


  _repaintAxisLabel(ctx) {
    const fsize = this.CELL_SIZE / 56;
    for (let i = 0; i < this.CELL_NUM; i++) {
      ctx.fillStyle = 'rgb(0,0,0)';
      ctx.font = "bold " + fsize + "rem sans-serif";
      ctx.textAlign = "left";
      ctx.textBaseline = "bottom"
      ctx.fillText(i + 1, this.LEFT_OFFSET + this.CELL_SIZE * i - this.CELL_SIZE / 6, this.CELL_SIZE * 1 - this.CELL_SIZE / 2, this.CELL_SIZE);
    }
    for (let i = 0; i < this.CELL_NUM; i++) {
      ctx.fillStyle = 'rgb(0,0,0)';
      ctx.font = "bold " + fsize + "rem sans-serif";
      ctx.textAlign = "right";
      ctx.textBaseline = "bottom"
      if (i <= 9) {
        ctx.fillText(numToKansuji(i), this.LEFT_OFFSET - this.CELL_SIZE / 2, this.TOP_OFFSET + this.CELL_SIZE * i + this.CELL_SIZE / 6, this.CELL_SIZE);
      } else {
        ctx.fillText("十", this.LEFT_OFFSET - this.CELL_SIZE / 2, this.TOP_OFFSET + this.CELL_SIZE * i, this.CELL_SIZE);
        ctx.fillText(numToKansuji(i - 10), this.LEFT_OFFSET - this.CELL_SIZE / 2, this.TOP_OFFSET + this.CELL_SIZE * (i + 0.35), this.CELL_SIZE);
      }
    }
  }


  _repaintStars(ctx) {
    const self = this;
    function paintStar(xys) {
      xys.forEach(function (xy) {
        ctx.beginPath();
        ctx.fillStyle = 'rgb(0,0,0)';
        ctx.arc(self.LEFT_OFFSET + self.CELL_SIZE * xy[0], self.TOP_OFFSET + self.CELL_SIZE * xy[1], (self.CELL_SIZE / 16.0) + 1, 0,
          Math.PI * 2, true);
        ctx.fill();
      });
    }
    switch (self.CELL_NUM) {
      case 9:
        paintStar([[2, 2], [2, 6], [4, 4], [6, 2], [6, 6]]);
        break;
      case 13:
        paintStar([[3, 3], [3, 6], [3, 9], [6, 3], [6, 6], [6, 9], [9, 3], [9, 6], [9, 9]]);
        break;
      case 19:
        paintStar([[3, 3], [9, 3], [15, 3], [3, 9], [9, 9], [15, 9], [3, 15], [9, 15], [15, 15]]);
        break;
      default:
    }
  }

  _repaintStones(ctx) {
    for (let i = 0; i < this.CELL_NUM; i++) {
      for (let j = 0; j < this.CELL_NUM; j++) {
        this._drawStone(ctx, i, j, gameState.cells[i][j]);
      }
    }
  }

  _repaintSymbols(ctx) {
    const self = this;
    Object.keys(gameState.symbols).forEach(function (key) {
      const x = key.split("-")[0];
      const y = key.split("-")[1];
      if (getStoneColor(gameState.cells[x][y]) == BLANK_STONE) {
        self._putOn(x, y, gameState.symbols[key]);
      }
      self._drawStone(ctx, x, y, gameState.symbols[key]);
    });
  }
  _repaintAgehama() {
    function showAgehama(num, selector1, selector2) {
      const capWidth = $("#black-stones-wrapper").width();
      const unit = capWidth / 10;
      $(selector1).css({
        "background-size": unit,
        "height": unit
      });
      $(selector2).css({
        "background-size": unit,
        "height": unit
      });
      if (num <= 5) {
        $(selector1).css('width', unit * num);
        $(selector2).css('width', 0);
      } else {
        $(selector1).css('width', unit * 5);
        $(selector2).css('width', unit * (num - 5 <= 5 ? num - 5 : 5));
      }
      $(selector1).css('max-width', capWidth);
      $(selector2).css('max-width', capWidth);
    }

    $("#black-agehama-num").text(gameState.agehama.black);
    $("#white-agehama-num").text(gameState.agehama.white);

    showAgehama(gameState.agehama.black, "#black-agehama-1",
      "#black-agehama-2");
    showAgehama(gameState.agehama.white, "#white-agehama-1",
      "#white-agehama-2");

  }


  bindEventOnCanvas(connection) {
    const self = this;

    let offsetTop;
    let offsetLeft;
    let pressed = false;
    let moving;
    let jqMoving;

    function setMoving(m) {
      moving = m;
      jqMoving = $(moving);
    }

    function getCellX(e) {
      function getLeft(e) {
        return getCurrentX(e) - offsetLeft
      }

      const x = Math.round((getLeft(e) - self.LEFT_OFFSET) / self.CELL_SIZE);
      if (x == -1) {
        return 0;
      } else if (x == self.CELL_NUM) {
        return self.CELL_NUM - 1
      } else if (x < 0 || self.CELL_NUM - 1 < x) { return -1; }
      return x;
    }

    function getCellY(e) {
      function getTop(e) {
        return getCurrentY(e) - offsetTop;
      }

      const y = Math.round((getTop(e) - self.TOP_OFFSET) / self.CELL_SIZE);
      if (y == -1) {
        return 0;
      } else if (y == self.CELL_NUM) {
        return self.CELL_NUM - 1
      } else if (y < 0 || self.CELL_NUM - 1 < y) { return -1; }
      return y;
    }

    function updateOffset() {
      const o = $('#game-board').offset();
      offsetTop = o.top;
      offsetLeft = o.left;
    }

    function setPressed(p) {
      pressed = p;
    }

    function revertLastHandIfOnBoard() {
      function revertLastHand() {
        self._putOn(pressed.lastHand.x, pressed.lastHand.y, pressed.lastHand.stone);
        if (getStoneSymbol(pressed.lastHand.stone) != BLANK_STONE
          && getStoneColor(pressed.lastHand.stone) == BLANK_STONE) {
          self._putSymbolOnBoard(pressed.lastHand.x, pressed.lastHand.y,
            getStoneSymbol(pressed.lastHand.stone));
        }
        sendGameStateWithOnBoardHand(connection, gameState, pressed.lastHand.x,
          pressed.lastHand.y, pressed.lastHand.stone);
      }
      if (pressed.type == ON_BOARD) {
        revertLastHand();
      }
    }

    function showAndUpdateStonePosition(e) {
      jqMoving.show();
      updateStonePosition(e);
    }

    function updateStonePosition(e) {
      jqMoving.offset({
        left: getCurrentX(e) - (jqMoving.width() / 2),
        top: getCurrentY(e) - (jqMoving.height() / 2)
      });
    }

    function setPressedFromPod() {
      setPressed({
        type: FROM_POD
      });
    }

    function setPressedAsAgehama(stone) {
      setPressed({
        type: AGEHAMA,
        stone: stone
      });
    }

    function setPressedAsOnBoard(x, y, stone) {
      setPressed({
        type: ON_BOARD,
        lastHand: {
          x: x,
          y: y,
          stone: stone
        }
      });
    }

    function sendGameStateWithRemovedFrom(connection, gameState, x, y, stone) {
      sendGameStateWithLastHand(connection, gameState, {
        type: REMOVE_FROM_BOARD,
        x: x,
        y: y,
        stone: stone
      });
    }

    function sendGameStateWithFromAgehama(connection, gameState, stone) {
      sendGameStateWithLastHand(connection, gameState, {
        type: FROM_AGEHAMA,
        stone: stone
      });
    }

    function sendGameStateWithAgehama(connection, gameState, stone) {
      sendGameStateWithLastHand(connection, gameState, {
        type: AGEHAMA,
        stone: stone
      });
    }

    setInterval(updateOffset, 500);

    (function () {
      $('#black-stones-cap').on(
        "mousedown touchstart",
        function (ev) {
          const e = ev.originalEvent;
          if (gameState.agehama.black == 0) { return; }
          setMoving('#moving-white-stone');
          gameState.agehama.black--;
          sendGameStateWithFromAgehama(connection, gameState,
            selectorToStone(moving));
          showAndUpdateStonePosition(e);
          setPressedAsAgehama(selectorToStone(moving));
          self.repaintBoard();
          e.preventDefault();
        });
    })();

    (function () {
      $('#white-stones-cap').on(
        "mousedown touchstart",
        function (ev) {
          const e = ev.originalEvent;
          if (gameState.agehama.white == 0) { return; }
          setMoving('#moving-black-stone');
          gameState.agehama.white--;
          sendGameStateWithFromAgehama(connection, gameState,
            selectorToStone(moving));
          showAndUpdateStonePosition(e);
          setPressedAsAgehama(selectorToStone(moving));
          self.repaintBoard();
          e.preventDefault();
        });
    })();

    (function () {
      $('#game-board').on("mousedown touchstart", function (ev) {
        const e = ev.originalEvent;
        const x = getCellX(e);
        const y = getCellY(e);
        if (x == -1 || y == -1) { return; }
        if (gameState.cells == null || isNaN(x) || isNaN(y)) { return; }
        const stone = gameState.cells[x][y];
        if (stone == BLANK_STONE) { return; }
        self._putOff(gameState, x, y);
        setMoving(stoneToSelector(stone));
        showAndUpdateStonePosition(e);
        setPressedAsOnBoard(x, y, stone);
        sendGameStateWithRemovedFrom(connection, gameState, x, y, stone)
        self.repaintBoard();
        e.preventDefault();
      });
    })();

    (function () {
      const A_STONES_SELECTOR = '#a-stones';
      const B_STONES_SELECTOR = '#b-stones';
      const C_STONES_SELECTOR = '#c-stones';
      const X_STONES_SELECTOR = '#x-stones';
      const CIRCLE_STONES_SELECTOR = '#circle-stones';
      const RECTANGLE_STONES_SELECTOR = '#rectangle-stones';
      const TRIANGLE_STONES_SELECTOR = '#triangle-stones';

      const CODE_SYMBOL_SELECTORS = [A_STONES_SELECTOR, B_STONES_SELECTOR,
        C_STONES_SELECTOR, X_STONES_SELECTOR, CIRCLE_STONES_SELECTOR,
        RECTANGLE_STONES_SELECTOR, TRIANGLE_STONES_SELECTOR];

      $(CODE_SYMBOL_SELECTORS.join(",")).on('mousedown touchstart', function (e) {
        setMoving("#moving-" + $(this).attr("id").replace("stones", "stone"));
        showAndUpdateStonePosition(e.originalEvent);
        setPressedFromPod();
        e.preventDefault();
      });
    })();

    (function () {
      $(['#black-stones', '#moving-black-stone'].join(",")).on('mousedown touchstart', function (e) {
        setMoving('#moving-black-stone');
        jqMoving.offset({
          left: jqMoving.offset().left,
          top: jqMoving.offset().top
        });
        showAndUpdateStonePosition(e.originalEvent);
        setPressedFromPod();
        e.preventDefault();
      });
    })();
    (function () {
      $(['#white-stones', '#moving-white-stone'].join(",")).on('mousedown touchstart', function (e) {
        setMoving('#moving-white-stone');
        jqMoving.offset({
          left: jqMoving.offset().left,
          top: jqMoving.offset().top
        });
        showAndUpdateStonePosition(e.originalEvent);
        setPressedFromPod();
        e.preventDefault();
      });
    })();

    (function () {
      $("body").on({
        "mousemove touchmove": function (e) {
          if (!pressed) { return; }
          updateStonePosition(e.originalEvent);
          // e.preventDefault();
        }
      })
    })();

    (function () {
      function procStoneOnCap(e) {
        function existsInCap(selector, x, y) {
          const capX = $(selector).offset().left;
          const capWidth = $(selector).width();
          const capY = $(selector).offset().top;
          const capHeight = $(selector).height();

          if (capX <= x && x <= capX + capWidth && capY <= y
            && y <= capY + capHeight) { return true; }
          return false;
        }
        if (existsInCap('#black-stones-cap', getCurrentX(e.originalEvent),
          getCurrentY(e.originalEvent))) {
          if (selectorToStone(moving) != WHITE_STONE) {
            if (pressed.type == AGEHAMA) {
              gameState.agehama.white++;
            }
            revertLastHandIfOnBoard();
            swalToast.fire({
              position: 'center-right',
              title: "白石を置くフタです",
            });
            return true;
          }
          gameState.agehama.black++;
          sendGameStateWithAgehama(connection, gameState, selectorToStone(moving));
        } else if (existsInCap('#white-stones-cap',
          getCurrentX(e.originalEvent), getCurrentY(e.originalEvent))) {
          if (selectorToStone(moving) != BLACK_STONE) {
            if (pressed.type == AGEHAMA) {
              gameState.agehama.black++;
            }
            revertLastHandIfOnBoard();
            swalToast.fire({
              position: 'center-left',
              title: "黒石を置くフタです",
            });
            return true;
          }
          gameState.agehama.white++;
          sendGameStateWithAgehama(connection, gameState, selectorToStone(moving));
        }
        return false;
      }

      function getTargetStone() {
        if (pressed.type == ON_BOARD) { return pressed.lastHand.stone; }
        return selectorToStone(moving);
      }

      function procStoneNotOnCap(e) {
        const x = getCellX(e.originalEvent);
        const y = getCellY(e.originalEvent);
        const stone = getTargetStone();

        if (!self._isValidCellIndex(x, y)) {
          // ここで誤った盤外ドロップを防ぎたいが，工夫しないと碁笥にも戻せなくなってしまう
          return;
        }

        if (getStoneColor(gameState.cells[x][y]) != BLANK_STONE
          && getStoneColor(stone) != BLANK_STONE) {
          if (pressed.type == ON_BOARD) {
            self._putOn(pressed.lastHand.x, pressed.lastHand.y, pressed.lastHand.stone);
            sendGameStateWithOnBoardHand(connection, gameState, x, y,
              selectorToStone(moving));
          } else if (pressed.type == AGEHAMA) {
            if (pressed.stone == BLACK_STONE) {
              gameState.agehama.white++;
            } else if (pressed.stone == WHITE_STONE) {
              gameState.agehama.black++;
            }
            sendGameStateWithAgehama(connection, gameState, pressed.stone);
          }
          return;
        }

        if (getStoneColor(gameState.cells[x][y]) == BLANK_STONE
          && getStoneSymbol(stone) != BLANK_STONE
          && getStoneColor(stone) == BLANK_STONE) {
          self._putSymbolOnBoard(x, y, getStoneSymbol(stone));
          self._putOn(x, y, stone);
        } else {
          const _stone = getStoneColor(gameState.cells[x][y]) + getStoneColor(stone)
            + getStoneSymbol(stone);
          self._putOn(x, y, _stone);
        }
        sendGameStateWithOnBoardHand(connection, gameState, x, y, stone);
      }

      const MOVING_SELECTORS =
        ["black", "white", "a", "b", "c", "x", "circle", "rectangle", "triangle"].map(e => "#moving-" + e + "-stone");

      $('body').on('mouseup touchend', function (e) {
        if (!pressed) { return; }
        $(MOVING_SELECTORS.join(",")).fadeOut(200);
        if (procStoneOnCap(e, pressed)) {
          self.repaintBoard();
          setPressed(false);
          return;
        }
        procStoneNotOnCap(e);
        self.repaintBoard();
        setPressed(false);
      });
    })();
  }
}

