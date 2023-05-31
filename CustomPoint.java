package com.tongs.funpatternwifi;


import android.graphics.Point;

import java.io.Serializable;

public class CustomPoint implements Serializable {
    public int x;
    public int y;

    public CustomPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public CustomPoint( CustomPoint src) {
        this.x = src.x;
        this.y = src.y;
    }
    public CustomPoint( Point src) {
        this.x = src.x;
        this.y = src.y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int setX(int x0) {
        return x=x0;
    }

    public int setY(int y0) {
        return y = y0;
    }
    public Point toPoint() {
        return new Point(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomPoint)) return false;
        CustomPoint other = (CustomPoint) o;
        return this.x == other.x && this.y == other.y;
    }
}
