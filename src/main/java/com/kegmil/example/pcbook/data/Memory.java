package com.kegmil.example.pcbook.data;

public class Memory {

    private long value;

    private Unit unit;

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "Memory{" +
                "value=" + value +
                ", unit=" + unit +
                '}';
    }
}
