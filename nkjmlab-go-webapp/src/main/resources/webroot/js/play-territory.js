$(function () {
  $('#btn-territory').on('click', function() {
    TerritoryMarker.showResult();
  });
});

class TerritoryMarker {
  static _neighbor(x, y, N) {
    return [
      [x - 1, y],
      [x + 1, y],
      [x, y - 1],
      [x, y + 1]
    ].filter(([nx, ny]) => nx >= 0 && nx < N && ny >= 0 && ny < N);
  }

  static _exploreRegion(cells, visited, x, y, N) {
    const region = [];
    const border = new Set();
    const stack = [[x, y]];

    while (stack.length) {
      const [cx, cy] = stack.pop();
      if (visited[cx][cy]) continue;
      visited[cx][cy] = true;
      region.push([cx, cy]);

      for (const [nx, ny] of this._neighbor(cx, cy, N)) {
        const val = cells[nx][ny];
        if (val === 0 && !visited[nx][ny]) {
          stack.push([nx, ny]);
        } else if (val === 1 || val === 2) {
          border.add(val);
        }
      }
    }

    return { region, border };
  }

  static _mark(cells) {
    const N = cells.length;
    
    for (let x = 0; x < N; x++) {
      for (let y = 0; y < N; y++) {
        if (cells[x][y] !== 0 && cells[x][y] !== 1 && cells[x][y] !== 2) {
          cells[x][y] = 0;
        }
      }
    }
    
    const visited = Array.from({ length: N }, () => Array(N).fill(false));

    for (let x = 0; x < N; x++) {
      for (let y = 0; y < N; y++) {
        if (cells[x][y] !== 0 || visited[x][y]) continue;

        const { region, border } = this._exploreRegion(cells, visited, x, y, N);

        if (border.size !== 1){
          continue;
        }

        const color = Array.from(border)[0] === 1 ? 10 : 70;
        for (const [rx, ry] of region) {
          cells[rx][ry] = color;
        }
      }
    }
  }
  static _countTerritory(cells) {
    let black = 0;
    let white = 0;
    const N = cells.length;

    for (let x = 0; x < N; x++) {
      for (let y = 0; y < N; y++) {
        if (cells[x][y] === 10) black++;
        else if (cells[x][y] === 70) white++;
      }
    }

    return { black, white };
  }

  static showResult() {
    this._mark(gameState.cells);
    gameBoard.repaintBoard();
    const {black, white} = this._countTerritory(gameState.cells);
    const blackAgehama = gameState.agehama.black;
    const whiteAgehama = gameState.agehama.white;
    const blackTotal = black - whiteAgehama;
    const whiteTotal = white - blackAgehama;
    const title = 
    `黒 ${blackTotal}目 vs 白 ${whiteTotal}目`;
    const message = 
     `黒 <img class="symbol-in-msg" src="/img/play/rectangle-stone.png"></img> (地: ${black}目，アゲハマ: ${blackAgehama}個)<br>
     白 <img class="symbol-in-msg" src="/img/play/circle-stone.png"></img> (地: ${white}目，アゲハマ: ${whiteAgehama}個)`;
    swalAlert(title, message, "info");
  }
}

