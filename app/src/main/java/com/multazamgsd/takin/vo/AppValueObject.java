package com.multazamgsd.takin.vo;

public enum AppValueObject {
    // Main section
    HOME("home"),
    SEMINAR("seminar"),
    COMMITTEE("committee"),
    CONTEST("contest"),

    // All event page
    LIKED("liked"),
    BOOKED("booked"),
    SEARCH("search");

    private final String value;

    AppValueObject(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
