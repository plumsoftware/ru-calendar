package com.plumsoftware.rucalendar;

import java.io.Serializable;

public class CelebrationItem implements Serializable {
    private String name, desc, color;
    private long timeInMillis;

    public CelebrationItem(String name, String desc, long timeInMillis) {
        this.name = name;
        this.desc = desc;
        this.timeInMillis = timeInMillis;
    }

    public CelebrationItem(String name, String desc, String color, long timeInMillis) {
        this.name = name;
        this.desc = desc;
        this.color = color;
        this.timeInMillis = timeInMillis;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
