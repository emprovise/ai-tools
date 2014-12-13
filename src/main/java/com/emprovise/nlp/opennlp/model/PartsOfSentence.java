package com.emprovise.nlp.opennlp.model;

public enum PartsOfSentence {

    SUBJECT("S-S"), PREDICATE("S-VP"), OBJECT(""), ADDITION(""), DEFAULT("");

    private String type;

    PartsOfSentence(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static PartsOfSentence getValue(String value) {
        for (PartsOfSentence partsOfSentence : values()) {
            if (partsOfSentence.type.equals(value)) {
                return partsOfSentence;
            }
        }
        return null;
    }
}
