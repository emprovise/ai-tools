package com.emprovise.nlp.opennlp.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sentence {

	private java.lang.String text;
    private LinkedList<Word> words = new LinkedList<Word>();
    private List<Triplet> triplets = new ArrayList<Triplet>();
    private static Logger log = LoggerFactory.getLogger(Sentence.class);

    public void addWord(String word, PartsOfSentence part, String type) {
        Word w = new Word();
        w.setWord(word);
        w.setPartOfSentence(part);
        w.setPartOfLanguage(type);
        words.add(w);
    }

    public void postProcess() {
        mergeWords();
        createTriplets();
        replaceSubjects();
        for (Triplet triplet : triplets) {
            log.info(triplet.toString());
        }
    }

    public void replaceSubjects() {
        Iterator<Triplet> iterator = triplets.iterator();
        Triplet prevTriplet = null;
        Triplet currTriplet = null;
        
        if (iterator.hasNext()) {
            prevTriplet = (Triplet) iterator.next();
        }

        if (prevTriplet != null) {
            while (iterator.hasNext()) {
                currTriplet = (Triplet) iterator.next();
                if (currTriplet.getSubjectType().equals("PRP")) {
                    currTriplet.setSubject(prevTriplet.getSubject());
                }
            }
        }
    }

    public void createTriplets() {
        int currIndex = 0;
        int index = 0;
        int nextIndex = -1;
        while (true) {
            Triplet triplet = new Triplet();
            index = findNextPosition(index, PartsOfSentence.SUBJECT);
            triplet.setSubject(words.get(index).getWord());
            triplet.setSubjectType(words.get(index).getPartOfLanguage().getType());
            index = index + 1;
            currIndex = index;
            index = findNextPosition(currIndex, PartsOfSentence.SUBJECT);
            nextIndex = findNextPosition(currIndex, PartsOfSentence.PREDICATE);
            if (index == -1 || nextIndex < index) {
                triplet.setPredicate(words.get(nextIndex).getWord());
            }
            index = nextIndex + 1;
            currIndex = index;
            index = findNextPosition(currIndex, PartsOfSentence.PREDICATE);
            nextIndex = findNextPosition(currIndex, PartsOfSentence.OBJECT);
            if (index == -1 || nextIndex < index) {
                triplet.setObject(words.get(nextIndex).getWord());
            }
            index = nextIndex + 1;
            triplets.add(triplet);
            if (index == words.size()) { 
            	break;
            }
        }
    }

    private int findNextPosition(int currentPosition, PartsOfSentence part) {
        for (int i = currentPosition; i < words.size(); i++) {
            if (words.get(i).getPartOfSentence().equals(part)) {
                return i;
            }
        }
        return -1;
    }

    private void mergeWords() {
        Iterator<Word> iterator = words.iterator();
        Word prevWord;
        if (iterator.hasNext()) {
            prevWord = iterator.next();
            while (iterator.hasNext()) {

                Word w = iterator.next();
                if (prevWord.getWord().equals(w.getWord()) && w.getPartOfSentence() == null) {
                    iterator.remove();
                    continue;
                }
                if (prevWord.getPartOfSentence() != null
                        && prevWord.getPartOfSentence().equals(w.getPartOfSentence())
                        ) {
                    prevWord.setWord(prevWord.getWord() + " " + w.getWord());
                    prevWord.setPartOfLanguage(w.getPartOfLanguage().getType());
                    iterator.remove();
                    continue;
                }
                prevWord = w;
            }
        }
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(LinkedList<Word> words) {
        this.words = words;
    }

    public java.lang.String getText() {
        return text;
    }

    public void setText(java.lang.String text) {
        this.text = text;
    }

    @Override
    public java.lang.String toString() {
        return text + " " + words + "\n";
    }
}
