package com.emprovise.nlp.stanfordnlp;

public enum AnnotatorOptions {
    TOKENIZE("tokenize"),
    CLEANXML("cleanxml"),
    SSPLIT("ssplit"),
    POS("pos"),
    LEMMA("lemma"),
    NER("ner"),
    REGEXNER("regexner"),
    SENTIMENT("sentiment"),
    TRUECASE("truecase"),
    PARSE("parse"),
    DEPPARSE("depparse"),
    DCOREF("dcoref"),
    RELATION("relation");

    private String value;

    private AnnotatorOptions(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean contains(AnnotatorOptions[] options, AnnotatorOptions option) {
        return contains(options, option.value);
    }

    public static boolean contains(AnnotatorOptions[] options, String option) {

        if(option != null && options != null) {
            for (AnnotatorOptions opt : options) {
                if (opt.value().equals(option)) {
                    return true;
                }
            }
        }

        return false;
    }
}
