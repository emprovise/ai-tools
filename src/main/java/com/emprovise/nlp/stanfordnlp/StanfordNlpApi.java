package com.emprovise.nlp.stanfordnlp;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.emprovise.nlp.stanfordnlp.AnnotatorOptions.*;
import static edu.stanford.nlp.ling.CoreAnnotations.*;

public class StanfordNlpApi {

    private StanfordCoreNLP pipeline;

    private static final Map<String, String> CUSTOM_PROPERTIES = new HashMap<String, String>();
    private static Logger log = LoggerFactory.getLogger(StanfordNlpApi.class);

    static {
        CUSTOM_PROPERTIES.put("ner.useSUTime", "true");
        CUSTOM_PROPERTIES.put("sutime.markTimeRanges", "true");
        CUSTOM_PROPERTIES.put("sutime.includeRange", "true");
        CUSTOM_PROPERTIES.put("parse.untokenizable", "noneDelete");
        CUSTOM_PROPERTIES.put("dcoref.postprocessing", "true");
    }

    public StanfordNlpApi() {
        this(CUSTOM_PROPERTIES, TOKENIZE, SSPLIT, POS, LEMMA, PARSE, SENTIMENT, NER);
    }

    public StanfordNlpApi(Map<String, String> customProperties, AnnotatorOptions... options) {

        Properties props = new Properties();

        if (options != null && options.length > 0) {
            StringBuilder annotators = new StringBuilder();
            for (AnnotatorOptions option : options) {
                if (annotators.length() > 0) {
                    annotators.append(", ");
                }
                annotators.append(option.value());
            }
            props.setProperty("annotators", annotators.toString());
        }

        if (customProperties != null) {
            props.putAll(customProperties);
        }

        props.setProperty("nthreads", "16");
        pipeline = new StanfordCoreNLP(props);

        if (customProperties != null && customProperties.containsKey("ner.useSUTime")
                && customProperties.get("ner.useSUTime").equals("true")) {
            addSUTimeAnnotator();
        }
    }

    private void addSUTimeAnnotator() {
        Properties props = new Properties();
        props.setProperty("sutime.markTimeRanges", "true");
        props.setProperty("sutime.includeRange", "true");
        TimeAnnotator sutime = new TimeAnnotator("sutime", props);
        pipeline.addAnnotator(sutime);
    }

    private void checkRequirements(String... options) {
        Set<Annotator.Requirement> requirements = pipeline.requirementsSatisfied();

        List<String> reqnames = new ArrayList<String>();
        for (Annotator.Requirement requirement : requirements) {
            reqnames.add(requirement.toString());
        }

        for (String option : options) {
            if (!reqnames.contains(option)) {
                throw new UnsupportedOperationException("Operation not supported as Annotation not loaded: " + option);
            }
        }
    }

    public List<String> detectSentence(String text) throws IOException {

        List<String> sentenceList = new LinkedList<String>();
        List<List<HasWord>> sentences = tokenizeWordList(text);

        for (List<HasWord> sentence : sentences) {

            StringBuilder sentBuilder = new StringBuilder();

            for (HasWord token : sentence) {

                if (sentBuilder.length() > 1) {
                    sentBuilder.append(" ");
                }
                sentBuilder.append(token);
            }
            sentenceList.add(sentBuilder.toString());
        }

        return sentenceList;
    }

    public List<List<HasWord>> tokenizeWordList(String text) throws IOException {
        Reader reader = new StringReader(text);
        DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(reader);
        List<List<HasWord>> sentences = new ArrayList<List<HasWord>>();
        for (List<HasWord> sentence : docPreprocessor) {
            sentences.add(sentence);
        }

        return sentences;
    }

    public List<Word> tokenizeWords(String text) throws IOException {
        WordTokenFactory wtf = new WordTokenFactory();
        PTBTokenizer<Word> tokenizer = new PTBTokenizer<Word>(new StringReader(text), wtf, "tokenizeNLs=true");
        return tokenizer.tokenize();
    }

    public List<CoreLabel> tokenizeLabels(String text) throws IOException {
        PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), new CoreLabelTokenFactory(), "");
        return tokenizer.tokenize();
    }

    public Map<String, String> sentiments(String text) {

        checkRequirements(Annotator.BINARIZED_TREES_REQUIREMENT.name);
        Map<String, String> sentSentimentMap = new LinkedHashMap<String, String>();

        if (text != null && !text.isEmpty()) {
            Annotation annotation = pipeline.process(text);

            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
                SimpleMatrix sentimentCoefficients = RNNCoreAnnotations.getPredictions(tree);
                log.debug("SENTENCE: " + sentence);
                log.debug("veryNegative: " + sentimentCoefficients.get(0));
                log.debug("negative: " + sentimentCoefficients.get(1));
                log.debug("neutral: " + sentimentCoefficients.get(2));
                log.debug("positive: " + sentimentCoefficients.get(3));
                log.debug("veryPositive: " + sentimentCoefficients.get(4));

                String partText = sentence.toString();
                sentSentimentMap.put(partText, sentence.get(SentimentCoreAnnotations.ClassName.class));
            }
        }

        return sentSentimentMap;
    }

    public <T> List<T> tokenAnnotations(String text, Class<? extends TypesafeMap.Key<T>> annotationClass) {
        List<T> annotations = new ArrayList<T>();
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                annotations.add(token.get(annotationClass));
            }
        }

        return annotations;
    }

    public List<Tree> parseTree(String text) {
        List<Tree> trees = new ArrayList<Tree>();
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
            Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
            tree.pennPrint();
            trees.add(tree);
        }

        return trees;
    }

    public List<SemanticGraph> semanticGraphs(String text) {
        List<SemanticGraph> dependencies = new ArrayList<SemanticGraph>();
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
            SemanticGraph semanticGraph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

            for (SemanticGraphEdge edge : semanticGraph.edgeListSorted()) {
                int depId = edge.getTarget().index() - 1;
                log.debug(edge.getRelation().getLongName());
            }
            dependencies.add(semanticGraph);
        }

        return dependencies;
    }

    /**
     * This is the coreference link graph. Each chain stores a set of mentions that
     * link to each other, along with a method for getting the most representative mention.
     * Both sentence and token offsets start at 1!
     *
     * @param text
     * @return
     */
    public Map<Integer, CorefChain> coreferenceGraph(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        return annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    }

    public List<String> POSTags(String text) {
        return tokenAnnotations(text, PartOfSpeechAnnotation.class);
    }

    public List<String> nameEntityTags(String text) {
        checkRequirements(Annotator.STANFORD_NER);
        return tokenAnnotations(text, NamedEntityTagAnnotation.class);
    }

    private LexicalizedParser loadLexicalParser() {
        String grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        String[] options = {"-maxLength", "80", "-retainTmpSubcategories"};
        return LexicalizedParser.loadModel(grammar, options);
    }

    public Collection<TypedDependency> typedDependencies(String text) {
        LexicalizedParser parser = loadLexicalParser();
        PTBTokenizer<Word> tokenizer = PTBTokenizer.newPTBTokenizer(new StringReader(text));
        List<Word> words = tokenizer.tokenize();
        Tree tree = parser.apply(words);

        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
        return tdl;
    }

    public List<String> stemWords(Tree tree) {
        List<String> words = new ArrayList<String>();
        WordStemmer wordStemmer = new WordStemmer();
        wordStemmer.visitTree(tree);
        for (TaggedWord tw : tree.taggedYield()) {
            words.add(tw.word());
        }

        return words;
    }

    public enum NamedEntity {
        PERSON, LOCATION, ORGANIZATION, MISC
    }

    public Map<String, String> classify(String text) {
        checkRequirements(Annotator.STANFORD_NER);
        Map<String, String> nerTokens = new LinkedHashMap<String, String>();
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        StringBuilder sb = new StringBuilder();
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String currNeToken = token.get(NamedEntityTagAnnotation.class);
                if(currNeToken != null && !currNeToken.equals("O")) {
                    String word = token.get(TextAnnotation.class);
                    nerTokens.put(word, currNeToken);
                }
            }
        }
        return nerTokens;
    }

    public List<String> classifyNamedEntity(String text, NamedEntity entity) {
        String serializedClassifier = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
        AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
        List<List<CoreLabel>> labels = classifier.classify(text);
        List<String> words = new ArrayList<String>();

        for (List<CoreLabel> sentence : labels) {
            for (CoreLabel word : sentence) {
                if (entity.name().equals(word.get(AnswerAnnotation.class))) {
                    words.add(word.word());
                }
            }
        }

        return words;
    }

    public Map<String, SUTime.Temporal> classifyTemporalEntity(String text) {

        Annotation annotation = new Annotation(text);
        String currentDate= new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        annotation.set(CoreAnnotations.DocDateAnnotation.class, currentDate);
        pipeline.annotate(annotation);

        Map<String, SUTime.Temporal> timexMap = new LinkedHashMap<String, SUTime.Temporal>();
        List<CoreMap> timexAnnotations = annotation.get(TimeAnnotations.TimexAnnotations.class);

        for (CoreMap timex : timexAnnotations) {
            timexMap.put(timex.toString(), timex.get(TimeExpression.Annotation.class).getTemporal());
        }

        return timexMap;
    }

    public static void main(String[] args) throws IOException {

        String paragraph = "Partial invoice (€100,000, so roughly 40%) for the consignment C27655 we shipped on 15th August to London from the Make Believe Town depot. INV2345 is for the balance.. Customer contact (Sigourney) says they will pay this on the usual credit terms (30 days).";
        StanfordNlpApi api = new StanfordNlpApi();
        System.out.println(api.POSTags(paragraph));
        System.out.println(api.tokenizeWords(paragraph));
        System.out.println(api.typedDependencies(paragraph));
        System.out.println(api.sentiments(paragraph));
        System.out.println(api.classifyNamedEntity(paragraph, NamedEntity.LOCATION));
        System.out.println(api.classifyTemporalEntity(paragraph));
    }
}
