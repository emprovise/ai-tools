package com.emprovise.nlp.opennlp.model;

public class Word {

    String word;
    PartsOfSentence partOfSentence;
    PartOfLanguage partOfLanguage;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public PartsOfSentence getPartOfSentence() {
        return partOfSentence;
    }

    public void setPartOfSentence(PartsOfSentence partOfSentence) {
        this.partOfSentence = partOfSentence;
    }

    public PartOfLanguage getPartOfLanguage() {
        return partOfLanguage;
    }

    public void setPartOfLanguage(String partOfLanguage) {
        this.partOfLanguage = PartOfLanguage.getValue(partOfLanguage);
    }

    @Override
    public String toString() {
        return "\nText = " + word + ";\t\tLabel = " + partOfSentence + ";\t\tPartOfLanguage = " + partOfLanguage;
    }
}
