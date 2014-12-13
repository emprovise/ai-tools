package com.emprovise.nlp.opennlp.model;

public enum PartOfLanguage {

    NOUN("NP"),
    VERB("VBP"),
    PRONOUN("PRP"),
    PREPOSITION("IN"),
    CONJUNCTION("CC"),
    ADVERB("RB"),
    DEFAULT("");

    private String type;

    PartOfLanguage(String type) {
        this.type = type;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static PartOfLanguage getValue(String value) {
        for (PartOfLanguage partOfLanguage : values()) {
            if (partOfLanguage.type.contains(value)) {
                return partOfLanguage;
            }
        }
        return null;
    }
}
