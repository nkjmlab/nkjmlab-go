package org.nkjmlab.go.javalin.model.json;

/**
 * stone<br>
 * 1桁目 0:ブランク， 1:黒， 2:白, 3:A, 4:B, 5:C<br>
 * 2桁目 0：ブランク， 1:□， 2：△, 3:x
 *
 */
public record HandJson(String type, int number, int x, int y, int stone) {


}
