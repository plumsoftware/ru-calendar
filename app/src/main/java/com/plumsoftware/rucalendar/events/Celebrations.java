package com.plumsoftware.rucalendar.events;

public class Celebrations {
    private int month;
    private int day;

    private long timeInMillis;

    public Celebrations(int month, int day) {
        this.month = month;
        this.day = day;
    }

    public Celebrations(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public String getDescription() throws IndexOutOfBoundsException{
        int[] i = new int[]{month, day};
        ArraysCelebrations arraysCelebrations = new ArraysCelebrations();
        return arraysCelebrations.check(i);
    }
}
