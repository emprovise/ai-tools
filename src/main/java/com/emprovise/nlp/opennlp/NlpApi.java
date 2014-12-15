package com.emprovise.nlp.opennlp;

import com.emprovise.nlp.opennlp.model.PartOfLanguage;
import com.emprovise.nlp.opennlp.model.Sentence;
import com.emprovise.nlp.opennlp.parsers.NlpParser;
import com.emprovise.nlp.util.NetUtil;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NlpApi {

	private static Logger log = LoggerFactory.getLogger(NlpApi.class);

	public static final String DEFAULT_OPENNLP_BASE_DIR = "library/opennlp";

	private NlpModelFactory modelFactory;

	private NlpParser nlpParser;

	public NlpApi() {
		modelFactory = new NlpModelFactory(new NetUtil(), DEFAULT_OPENNLP_BASE_DIR);
	}

	public NlpApi(String opennlpBaseDir) {
		modelFactory = new NlpModelFactory(new NetUtil(), opennlpBaseDir);
	}

	public NlpApi(String proxyAddress, int proxyPort, String proxyUser, String proxyPassword) {
		modelFactory = new NlpModelFactory(new NetUtil(proxyAddress, proxyPort, proxyUser, proxyPassword), DEFAULT_OPENNLP_BASE_DIR);
	}

	public NlpApi(String proxyAddress, int proxyPort, String proxyUser, String proxyPassword, String opennlpBaseDir) {
		modelFactory = new NlpModelFactory(new NetUtil(proxyAddress, proxyPort, proxyUser, proxyPassword), opennlpBaseDir);
	}

	private void initNlpParser() {
		if (nlpParser == null) {
			nlpParser = new NlpParser(modelFactory.getSentenceModel(), modelFactory.getParserModel());
		}
	}

	public String[] tokenize(String text) throws IOException {

		TokenizerModel model = modelFactory.getTokenizerModel();
		Tokenizer tokenizer = new TokenizerME(model);
		return tokenizer.tokenize(text);
	}

	// always start with a model, a model is learned from training data
	public String[] detectSentence(String text) throws IOException {

		SentenceModel model = modelFactory.getSentenceModel();
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
		return sentenceDetector.sentDetect(text);
	}

	public Parse parse(String sentence) throws IOException {
		return parse(sentence, 1)[0];
	}

	// http://sourceforge.net/apps/mediawiki/opennlp/index.php?title=Parser#Training_Tool
	public Parse[] parse(String sentence, int numParses) throws IOException {
		ParserModel model = modelFactory.getParserModel();
		Parser parser = ParserFactory.create(model);
		Parse topParses[] = ParserTool.parseLine(sentence, parser, numParses);
		return topParses;
	}

	public List<String> getPhrases(Parse parse, PartOfLanguage partOfLanguage) {
		return getPhrases(parse, partOfLanguage, new ArrayList<String>());
	}

	private List<String> getPhrases(Parse parse, PartOfLanguage partOfLanguage, List<String> phrases) {

		if (parse.getType().equals(partOfLanguage.getType())) {
			phrases.add(parse.getCoveredText());
		}
		for (Parse child : parse.getChildren()) {
			getPhrases(child, partOfLanguage, phrases);
		}

		return phrases;
	}

	public Span[] findName(String[] words) throws IOException {

		TokenNameFinderModel model = modelFactory.getTokenNameFinderModelByPerson();
		NameFinderME nameFinder = new NameFinderME(model);
		Span nameSpans[] = nameFinder.find(words);
		return nameSpans;
	}

	public String[] getPOSTags(String text) throws IOException {

		String[] words = tokenize(text);
		return getPOSTags(words);
	}

	public String[] getPOSTags(String[] words) throws IOException {

		POSTaggerME tagger = new POSTaggerME(modelFactory.getPOSMaxentModel());
		return tagger.tag(words);
	}

	public POSSample POSTag(String text) throws IOException {

		POSModel model = modelFactory.getPOSMaxentModel();
		PerformanceMonitor performanceMonitor = new PerformanceMonitor(System.err, "sent");
		POSTaggerME tagger = new POSTaggerME(model);

		ObjectStream<String> lineStream = new PlainTextByLineStream(new StringReader(text));

		performanceMonitor.start();
		String line;
		POSSample sample = null;

		while ((line = lineStream.read()) != null) {

			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE.tokenize(line);
			String[] tags = tagger.tag(whitespaceTokenizerLine);

			sample = new POSSample(whitespaceTokenizerLine, tags);
			System.out.println(sample.toString());

			performanceMonitor.incrementCounter();
		}

		performanceMonitor.stopAndPrintFinalResult();
		return sample;
	}

	public Span[] chunkAsSpans(String text) throws IOException {
		POSSample sample = POSTag(text);
		return chunkAsSpans(sample.getSentence(), sample.getTags());
	}

	public Span[] chunkAsSpans(String words[], String[] tags) {
		ChunkerME chunkerME = new ChunkerME(modelFactory.getChunkerModel());
		return chunkerME.chunkAsSpans(words, tags);
	}

	public String[] chunk(String text) throws IOException {
		POSSample sample = POSTag(text);
		return chunk(sample.getSentence(), sample.getTags());
	}

	public String[] chunk(String words[], String[] tags) {
		ChunkerME chunkerME = new ChunkerME(modelFactory.getChunkerModel());
		return chunkerME.chunk(words, tags);
	}

	public Sentence parseSentence(String input) {
		initNlpParser();
		return nlpParser.parseSentence(input);
	}

	public List<Sentence> parseText(String input) {
		initNlpParser();
		return nlpParser.parseText(input);
	}

	/**
	 * Classifies text via a maxent model. Try to keep chunks of text small, or
	 * typically there will be all low scores with little difference.
	 *
	 * @param text the string to be classified
	 * @return
	 */
	public Map<String, Double> probabilityDist(String text) throws IOException {

		Map<String, Double> probDist = new HashMap<String, Double>();
		DoccatModel model = modelFactory.getModel("en-doccat.train", DoccatModel.class);
		if(model == null) {
			model = NlpTrainingApi.trainDocumentCategorizer("nlptraining/en-doccat.train");
			modelFactory.addModel("en-doccat.train", model);
		}
		DocumentCategorizerME documentCategorizer = new DocumentCategorizerME(model);
		double[] categorize = documentCategorizer.categorize(text);

		for (int i = 0; i < documentCategorizer.getNumberOfCategories(); i++) {
			String category = documentCategorizer.getCategory(i);
			probDist.put(category, categorize[documentCategorizer.getIndex(category)]);
		}
		return probDist;
	}

	public static void main(String[] args) throws IOException {

		NlpApi api = new NlpApi();
		String text = "Who is the author of The Call of the Wild ?";
		System.out.println(api.parse(text));
		String[] sentence = new String[] { "Mike", "Smith", "is", "a", "good", "person" };
		System.out.println(api.findName(sentence));
		System.out.println(api.probabilityDist("Where are you now ?"));
	}
}
