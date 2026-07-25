package com.itpassport.app.stats;

public enum GenreJudgement {
    EXCELLENT("得意", "excellent"),
    NORMAL("普通", "normal"),
    WEAK("苦手", "weak"),
    VERY_WEAK("かなり苦手", "very-weak"),
    INSUFFICIENT_DATA("判定対象外", "insufficient");

    private final String label;
    private final String cssClass;

    GenreJudgement(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String label() {
        return label;
    }

    public String cssClass() {
        return cssClass;
    }
}
