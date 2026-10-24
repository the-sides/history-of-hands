package com.jacobsides.hands;

final class QuickResult {
  static String opponent(String mine, String result) {
    String[] throwsList = {"ROCK", "PAPER", "SCISSORS"};
    int index = java.util.Arrays.asList(throwsList).indexOf(mine);
    if (index < 0) throw new IllegalArgumentException("Invalid throw");
    int offset;
    switch (result) {
      case "win":
        offset = 2;
        break;
      case "loss":
        offset = 1;
        break;
      case "tie":
        offset = 0;
        break;
      default:
        throw new IllegalArgumentException("Invalid result");
    }
    return throwsList[(index + offset) % 3];
  }
}
