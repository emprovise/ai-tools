package com.emprovise.nlp.wordnet;

import com.emprovise.nlp.util.FileUtil;
import com.emprovise.nlp.util.NetUtil;
import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class WordNetApi {

	public static final String DEFAULT_WORDNET_DB_FILES_URL = "http://wordnetcode.princeton.edu/wn3.1.dict.tar.gz";
	public static final String DEFAULT_WORDNET_BASE_DIR = "library/wordnet";

	private WordNetDatabase database;
	private WordNetUtils wordNetUtils;

	public WordNetApi() {
		this(new NetUtil());
	}

	public WordNetApi(String proxyAddress, int proxyPort, String proxyUser, String proxyPassword) {
		this(new NetUtil(proxyAddress, proxyPort, proxyUser, proxyPassword));
	}

	private WordNetApi(NetUtil netUtil) {
		try {
			File wordnetLibrary = loadWordnetLibrary(netUtil, DEFAULT_WORDNET_BASE_DIR);
			System.setProperty("wordnet.database.dir", wordnetLibrary.getAbsolutePath());
			database = WordNetDatabase.getFileInstance();
			wordNetUtils = new WordNetUtils(wordnetLibrary);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static File loadWordnetLibrary(NetUtil netUtil, String wordnetBaseDir) throws IOException, URISyntaxException {

		URL wordnetUrl = new URL(DEFAULT_WORDNET_DB_FILES_URL);

		File wordnetDir = new File(wordnetBaseDir);
		File dictionaryFile = new File(wordnetBaseDir, "dict");

		if(!dictionaryFile.exists() || dictionaryFile.list().length == 0) {

			File wordnetFile = netUtil.downloadFile(wordnetUrl, "wn.dict.tar.gz", wordnetBaseDir);
			FileUtil.extractFile(wordnetFile, wordnetDir);

			File framesVrbFile = new File(WordNetApi.class.getClassLoader().getResource("wordnet/frames.vrb").toURI());
			Files.copy(framesVrbFile.toPath(), new File(wordnetBaseDir + "/dict/frames.vrb").toPath());
			wordnetFile.delete();
		}

		return dictionaryFile;
	}

	public List<String> getSynonyms(String word, SynsetType synsetType) {
		Synset[] synsets = database.getSynsets(word, synsetType);
		return getSynonyms(synsets);
	}

	public List<String> getSynonyms(String word) {
		Synset[] synsets = database.getSynsets(word);
		return getSynonyms(synsets);
	}

	private List<String> getSynonyms(Synset[] synsets) {

		List<String> synonyms=new ArrayList<String>();

		for (int i = 0; i < synsets.length; i++) {
			String[] wordForms = synsets[i].getWordForms();
			for (int j = 0; j < wordForms.length; j++) {
				if(!synonyms.contains(wordForms[j])) {
					synonyms.add(wordForms[j]);
				}
			}
		}
		return synonyms;
	}

	public List<String> getHyponyms(String word, SynsetType synsetType) {
		Synset[] synsets = database.getSynsets(word, synsetType);
		return getHyponyms(synsets);
	}

	private List<String> getHyponyms(Synset[] synsets) {

		List<String> hyponymList=new ArrayList<String>();

		for (int i = 0; i < synsets.length; i++) {
			NounSynset[] hyponyms = ((NounSynset)synsets[i]).getHyponyms();
			for (NounSynset hyponym : hyponyms) {
				for(String wordForm : hyponym.getWordForms()) {
					hyponymList.add(wordForm);
				}
			}
		}
		return hyponymList;
	}

	public WordNetUtils getWordNetUtils() {
		return wordNetUtils;
	}

	public static void main(String[] args) throws Exception {

		WordNetApi api = new WordNetApi();
		String wordForm = "cry";
		System.out.println("Id = " + api.getHyponyms(wordForm, SynsetType.NOUN));
		WordNetUtils utils = api.getWordNetUtils();
		System.out.println(utils.getMostLikleyPOS(wordForm));
		System.out.println(utils.getStems("washed", "V"));
		System.out.println(utils.getSynonyms("animal", "N", true));
		System.out.println(utils.getHypernyms("day", "N", true));
	}
}
