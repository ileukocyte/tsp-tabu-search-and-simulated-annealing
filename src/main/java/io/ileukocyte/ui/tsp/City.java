package io.ileukocyte.ui.tsp;

public record City(int x, int y) {
    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }

    public double euclidDistance(City another) {
        return Math.sqrt(Math.pow(x - another.x(), 2) + Math.pow(y - another.y(), 2));
    }
}