
package com.emprovise.nlp.wordnet;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WordNetUtils {

	private IDictionary iDictionary;

    public WordNetUtils(String wordnetPath) throws IOException {
        URL url = new URL("file", null, wordnetPath);
        iDictionary = new Dictionary(url);
        iDictionary.open();
    }

    public WordNetUtils(File wordnetDir) throws IOException {
        URL url = new URL("file", null, wordnetDir.getAbsolutePath());
        iDictionary = new Dictionary(url);
        iDictionary.open();
    }

    /**
     * A simple way to get the head word of a phrase.
     */
    public static String getHeadWord(String s) {

        String[] elements = s.split(" ");
        return elements[elements.length - 1];
    }

    /**
     * Initialize WordNet dictionary.
     */
    public static IDictionary getDictionary(String wordNetPath) throws IOException {

        URL url = new URL("file", null, wordNetPath);
        IDictionary iDictionary = new Dictionary(url);
        iDictionary.open();

        return iDictionary;
    }

    /**
     * Get a list of possible stems. Assume we are looking up a noun.
     */
    public List<String> getStems(String word, String posTag) {

        POS pos = POS.getPartOfSpeech(posTag.charAt(0));
        if(pos == null) {
            return new ArrayList<String>();
        }

        WordnetStemmer wordnetStemmer = new WordnetStemmer(iDictionary);
        List<String> stems = wordnetStemmer.findStems(word, pos);

        return stems;
    }

    /**
     * Retrieve a set of synonyms for a word. Use only the first sense if useFirstSense flag is true.
     */
    public HashSet<String> getSynonyms(String word, String posTag, boolean firstSenseOnly) {

        // need a set to avoid repeating words
        HashSet<String> synonyms = new HashSet<String>();

        POS pos = POS.getPartOfSpeech(posTag.charAt(0));
        if(pos == null) {
            return synonyms;
        }

        IIndexWord iIndexWord = iDictionary.getIndexWord(word, pos);
        if(iIndexWord == null) {
            return synonyms; // no senses found
        }

        // iterate over senses
        for(IWordID iWordId : iIndexWord.getWordIDs()) {
            IWord iWord = iDictionary.getWord(iWordId);

            ISynset iSynset = iWord.getSynset();
            for(IWord synsetMember : iSynset.getWords()) {
                synonyms.add(synsetMember.getLemma());
            }

            if(firstSenseOnly) {
                break;
            }
        }

        return synonyms;
    }

    /**
     * Retrieve a set of hypernyms for a word. Use only the first sense if useFirstSense flag is true.
     */
    public HashSet<String> getHypernyms(String word, String posTag, boolean firstSenseOnly) {

        HashSet<String> hypernyms = new HashSet<String>();

        POS pos = POS.getPartOfSpeech(posTag.charAt(0));
        if(pos == null) {
            return hypernyms;
        }

        IIndexWord iIndexWord = iDictionary.getIndexWord(word, pos);
        if(iIndexWord == null) {
            return hypernyms; // no senses found
        }

        // iterate over senses
        for(IWordID iWordId : iIndexWord.getWordIDs()) {
            IWord iWord1 = iDictionary.getWord(iWordId);
            ISynset iSynset = iWord1.getSynset();

            // multiple hypernym chains are possible for a synset
            for(ISynsetID iSynsetId : iSynset.getRelatedSynsets(Pointer.HYPERNYM)) {
                List<IWord> iWords = iDictionary.getSynset(iSynsetId).getWords();
                for(IWord iWord2: iWords) {
                    String lemma = iWord2.getLemma();
                    hypernyms.add(lemma.replace(' ', '_')); // also get rid of spaces
                }
            }

            if(firstSenseOnly) {
                break;
            }
        }

        return hypernyms;
    }

    public static HashSet<String> getHyperHypernyms(IDictionary dict, String word, String posTag, boolean firstSenseOnly) {

        HashSet<String> hypernyms = new HashSet<String>();

        POS pos = POS.getPartOfSpeech(posTag.charAt(0));
        if(pos == null) {
            return hypernyms;
        }

        IIndexWord iIndexWord = dict.getIndexWord(word, pos);
        if(iIndexWord == null) {
            return hypernyms; // no senses found
        }

        // iterate over senses
        for(IWordID iWordId : iIndexWord.getWordIDs()) {
            IWord iWord1 = dict.getWord(iWordId);
            ISynset iSynset = iWord1.getSynset();

            for(ISynsetID iSynsetId1 : iSynset.getRelatedSynsets(Pointer.HYPERNYM)) {
                for(ISynsetID iSynsetId2 : dict.getSynset(iSynsetId1).getRelatedSynsets(Pointer.HYPERNYM)) {
                    List<IWord> iWords = dict.getSynset(iSynsetId2).getWords();
                    for(IWord iWord2: iWords) {
                        String lemma = iWord2.getLemma();
                        hypernyms.add(lemma.replace(' ', '_')); // also get rid of spaces
                    }
                }
            }

            if(firstSenseOnly) {
                break;
            }
        }

        return hypernyms;
    }

    public synchronized POS getMostLikleyPOS(String word) {
        WordnetStemmer stemmer = new WordnetStemmer(iDictionary);

        int maxCount = -1;
        edu.mit.jwi.item.POS mostLikelyPOS = null;
        for(edu.mit.jwi.item.POS pos : edu.mit.jwi.item.POS.values()) {

            //From JavaDoc: The surface form may or may not contain whitespace or underscores, and may be in mixed case.
            word = word.replaceAll("\\s", "").replaceAll("_", "");
            List<String> stems = stemmer.findStems(word, pos);
            for(String stem : stems) {

                IIndexWord indexWord = iDictionary.getIndexWord(stem, pos);
                if(indexWord!=null) {
                    int count = 0;
                    for(IWordID wordId : indexWord.getWordIDs()) {
                        IWord aWord = iDictionary.getWord(wordId);
                        //ISynset synset = aWord.getSynset();
                        //log(LogLevel.DEBUG, synset.getGloss());
                        ISenseEntry senseEntry = iDictionary.getSenseEntry(aWord.getSenseKey());
                        //log(LogLevel.DEBUG, senseEntry.getSenseNumber());
                        count += senseEntry.getTagCount();
                    }

                    //int tagSenseCount = indexWord.getTagSenseCount();
                    //int wordIdCount = indexWord.getWordIDs().size();
                    if(count > maxCount) {
                        maxCount = count;
                        mostLikelyPOS = pos;
                    }
                }
            }
        }

        return translateWordNetPOSToPennPOS(mostLikelyPOS);
    }

    private POS translateWordNetPOSToPennPOS(edu.mit.jwi.item.POS pos) {
        if(pos==null)
            return null;
        switch(pos) {
            case NOUN:
                return POS.NOUN;
            case VERB:
                return POS.VERB;
            case ADJECTIVE:
                return POS.ADJECTIVE;
            case ADVERB:
                return POS.ADVERB;
            default:
                return null;
        }
    }
}
