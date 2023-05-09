package com.plumsoftware.rucalendar;

class Celebrations {
    private int month;
    private int day;

    private long timeInMillis;

    Celebrations(int month, int day) {
        this.month = month;
        this.day = day;
    }

    public Celebrations(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    String getDescription() throws IndexOutOfBoundsException{
        int[] i = new int[]{month, day};
        ArraysCelebrations arraysCelebrations = new ArraysCelebrations();
        return arraysCelebrations.check(i);
    }
}
