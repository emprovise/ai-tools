package com.emprovise.nlp.lingpipe;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.classify.Classification;
import com.aliasi.classify.ConditionalClassification;
import com.aliasi.classify.LMClassifier;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tag.ScoredTagging;
import com.aliasi.tag.TagLattice;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;
import com.emprovise.nlp.util.NetUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * <p>This class provides a common interface to the
 * <a href="http://www.alias-i.com/lingpipe/">LingPipe</a> toolkit.</p>
 * 
 * <p>It supports the following natural language processing tools:
 * <ul>
 * <li>Tokenization</li>
 * <li>Sentence detection</li>
 * <li>Parts Of Speech Tagging</li>
 * </ul>
 * </p>
 * 
 * @author Nico Schlaefer
 * @version 2006-11-25
 */
public class LingPipeApi {

	public static final int MAX_N_BEST = 5;
	/** Tokenization model. */
	private TokenizerFactory tokenizerFactory;
	/** Sentence detection model. */
	private SentenceModel sentenceModel;

	private HiddenMarkovModel hiddenMarkovModel;

	protected LMClassifier classifier;
	protected Chunker chunker;

	private MapDictionary<String> dictionary = new MapDictionary<String>();

	private static Logger log = LoggerFactory.getLogger(LingPipeApi.class);

	public static final String DEFAULT_LINGPIPE_BASE_DIR = "library/lingpipe";

	private static final String POS_HMM_URL = "http://alias-i.com/lingpipe/demos/models/pos-en-general-brown.HiddenMarkovModel";
	private static final String CHUNKER_URL = "http://alias-i.com/lingpipe/demos/models/ne-en-news-muc6.AbstractCharLmRescoringChunker";
	private static final String CLASSIFIER_PATH = null;

	/**
	 * Creates models for the tokenizer and the sentence detector, if not
	 * already done.
	 */
	public LingPipeApi() {

		tokenizerFactory = new IndoEuropeanTokenizerFactory();
		sentenceModel = new MedlineSentenceModel();

		try {
			NetUtil netUtil = new NetUtil();
			File posHmmFile = netUtil.downloadFile(new URL(POS_HMM_URL), getFileNameFromUrl(POS_HMM_URL), DEFAULT_LINGPIPE_BASE_DIR);
			log.info(posHmmFile.getAbsolutePath() + " Loaded...");
			File chunkerFile = netUtil.downloadFile(new URL(CHUNKER_URL), getFileNameFromUrl(CHUNKER_URL), DEFAULT_LINGPIPE_BASE_DIR);
			log.info(chunkerFile.getAbsolutePath() + " Loaded...");

			hiddenMarkovModel = (HiddenMarkovModel) AbstractExternalizable.readObject(posHmmFile);
			chunker = (Chunker)AbstractExternalizable.readObject(chunkerFile);

			if(CLASSIFIER_PATH != null) {
				classifier = (LMClassifier) AbstractExternalizable.readObject(new File(CLASSIFIER_PATH));
			}
		}
		catch (IOException | ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	private String getFileNameFromUrl(String url) {
		return url.substring(url.lastIndexOf("/")+1, url.length());
	}

	/**
	 * Tokenizes a text.
	 * 
	 * @param text text to tokenize
	 * @return array of tokens or <code>null</code>, if the tokenizer is not
	 *         initialized
	 */
	public String[] tokenize(String text) {
		if (tokenizerFactory == null) return null;
		
		ArrayList<String> tokenList = new ArrayList<String>();
		ArrayList<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer =
			tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
		tokenizer.tokenize(tokenList, whiteList);
		
		return tokenList.toArray(new String[tokenList.size()]);
	}
	
	/**
	 * Tokenizes a text and concatenates the tokens with spaces.
	 * 
	 * @param text text to tokenize
	 * @return string of space-delimited tokens or <code>null</code>, if the
	 *         tokenizer is not initialized
	 */
	public String tokenizeWithSpaces(String text) {
		String[] tokens = tokenize(text);
		return (tokens != null) ? StringUtils.join(tokens, ' ') : null;
	}

	public String[] tokenizeArray(String text) {
		return tokenize(text);
	}
	/**
	 * Splits a text into sentences.
	 * 
	 * @param text sequence of sentences
	 * @return array of sentences in the text or <code>null</code>, if the
	 *         sentence detector is not initialized
	 */
	public String[] sentDetect(String text) {

		if (sentenceModel == null) {
			return null;
		}
		
	    // tokenize text
		ArrayList<String> tokenList = new ArrayList<String>();
		ArrayList<String> whiteList = new ArrayList<String>();
		Tokenizer tokenizer = tokenizerFactory.tokenizer(text.toCharArray(), 0, text.length());
		tokenizer.tokenize(tokenList, whiteList);
		
		String[] tokens = tokenList.toArray(new String[tokenList.size()]);
		String[] whites = whiteList.toArray(new String[whiteList.size()]);
		
		// detect sentences
		int[] sentenceBoundaries = sentenceModel.boundaryIndices(tokens, whites);
		
		int sentStartTok = 0;
		int sentEndTok = 0;
		String[] sentences = new String[sentenceBoundaries.length];

		for (int i = 0; i < sentenceBoundaries.length; i++) {
			sentEndTok = sentenceBoundaries[i];
			
			StringBuilder sb = new StringBuilder();
			for (int j = sentStartTok; j <= sentEndTok; j++) {
				sb.append(tokens[j]);
				if (whites[j + 1].length() > 0 && j < sentEndTok)
					sb.append(" ");
			}

			sentences[i] = sb.toString();
			sentStartTok = sentEndTok+1;
		}
		
		return sentences;
	}

	/**
	 * Obtains first five best outputs.
	 * @param tokens
	 * @return
	 */
	public Map<Double, List<String>> nBest(List<String> tokens) {
		Map<Double, List<String>> tagMap = new HashMap<Double, List<String>>();
		HmmDecoder decoder = new HmmDecoder(hiddenMarkovModel);
		Iterator<ScoredTagging<String>> nBestIt = decoder.tagNBest(tokens, MAX_N_BEST);

		for(int n = 0; n < MAX_N_BEST && nBestIt.hasNext(); ++n) {
			ScoredTagging<String> tagScores = nBestIt.next();
			double score = tagScores.score();
			List<String> tags = tagScores.tags();
			tagMap.put(new Double(score), tags);
		}

		return tagMap;
	}

	/**
	 * Obtains only the first best result.
	 * @param tokens
	 * @param decoder
	 * @return an array of pos tags.
	 */
	private List<String> firstBest(List<String> tokens, HmmDecoder decoder) {
		Tagging<String> tagging = decoder.tag(tokens);
		return tagging.tags();
	}

	/**
	 * For every word, it obtains five pos tags and their confidence
	 * @param tokens
	 * @param decoder
	 * @return
	 */
	private List<Map<String, Double>> confidence(List<String> tokens,
												 HmmDecoder decoder) {
		List<Map<String, Double>> toReturn = new ArrayList<Map<String, Double>>();
		TagLattice<String> lattice = decoder.tagMarginal(tokens);
		for(int tokenIndex = 0; tokenIndex < tokens.size(); ++tokenIndex) {
			ConditionalClassification tagScores = lattice.tokenClassification(tokenIndex);

			Map<String, Double> map = new HashMap<String, Double>();
			for(int i = 0; i < tagScores.size(); ++i) {
				double conditionalProb = tagScores.conditionalProbability(i);
				String tag = tagScores.category(i);
				map.put(tag, new Double(conditionalProb));
			}
			toReturn.add(map);
		}
		return toReturn;
	}

	/**
	 * Classify the text of the whole document and return the best category.
	 * @param text
	 * @return
	 */
	public String classify(String text) {
		Classification classification = classifier.classify(text);
		return classification.bestCategory();
	}

	public List<String> chunk(String text) {
		List<String> chunkList = new ArrayList<String>();
		Chunking chunking = chunker.chunk(text);
		for(Chunk chunk : chunking.chunkSet()) {
			String phrase = text.substring(chunk.start(), chunk.end());
			chunkList.add(phrase);
			System.out.println("    phrase=|" + phrase + "|"
								+ " type=" + chunk.type()
								+ " score=" + chunk.score());
		}

		return chunkList;
	}

	private void addDictionaryEntry(String phrase, String category, double score) {
		dictionary.addEntry(new DictionaryEntry<String>(phrase, category, score));
	}

	private ExactDictionaryChunker getDictionaryChunker(TokenizerFactory factory) {
		ExactDictionaryChunker dictionaryChunker = new ExactDictionaryChunker(dictionary, factory, true, true);
		return dictionaryChunker;
	}

	public static void main(String[] args) {
		LingPipeApi lingPipe = new LingPipeApi();
		String sentence = "It has implementations of probabilistic natural language parsers, both highly optimized PCFG and lexicalized dependency parsers, and a lexicalized PCFG parser.";
		String[] list = lingPipe.tokenizeArray(sentence);
		System.out.println("nBEST : " + lingPipe.nBest(Arrays.asList(list)));
		System.out.println("CHUNKS : " + lingPipe.chunk(sentence));
	}
}
