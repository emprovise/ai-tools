package com.emprovise.nlp.opennlp;

import opennlp.tools.chunker.*;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.lang.en.HeadRules;
import opennlp.tools.postag.*;
import opennlp.tools.sentdetect.*;
import opennlp.tools.tokenize.*;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.model.BaseModel;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;

public class NlpTrainingApi {

    private NlpTrainingApi() {
        throw new UnsupportedOperationException();
    }

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String DEFAULT_LANG = "en";

    public static SentenceModel trainSentenceDetector(String trainingFile) throws IOException {

        trainingFile = getResourceFile(trainingFile);
        ObjectStream<String> lineStream =
                new PlainTextByLineStream(new FileInputStream(trainingFile), UTF_8);
        ObjectStream<SentenceSample> sampleStream = new SentenceSampleStream(lineStream);
        SentenceModel model;

        try {
            SentenceDetectorFactory factory = new SentenceDetectorFactory(DEFAULT_LANG, true, null, null);
            model = SentenceDetectorME.train(DEFAULT_LANG, sampleStream, factory, TrainingParameters.defaultParams());
            return model;
        }
        finally {
            sampleStream.close();
        }
    }

    public static TokenizerModel trainTokenizer(String trainingFile) throws IOException {

        trainingFile = getResourceFile(trainingFile);
        ObjectStream<String> lineStream = new PlainTextByLineStream(new FileInputStream(trainingFile), UTF_8);
        ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

        TokenizerModel model;

        try {
            TokenizerFactory factory = new TokenizerFactory(DEFAULT_LANG, null, true, null);
            model = TokenizerME.train(sampleStream, factory, TrainingParameters.defaultParams());
            return model;
        }
        finally {
            sampleStream.close();
        }
    }

    public static TokenNameFinderModel trainNameFinder(String trainingFile) throws IOException {

        trainingFile = getResourceFile(trainingFile);
        ObjectStream<String> lineStream =
                new PlainTextByLineStream(new FileInputStream(trainingFile), UTF_8);
        ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

        TokenNameFinderModel model;

        try {
            model = NameFinderME.train(DEFAULT_LANG, "person", sampleStream, TrainingParameters.defaultParams(),
                    (AdaptiveFeatureGenerator) null, Collections.<String, Object>emptyMap());
            return model;
        }
        finally {
            sampleStream.close();
        }
    }

    /**
     * Note: The training file for Document Categorizer must contain a minimum of 100 entries to generate a DocumentCategorizerME model.
     *
     * @param trainingFile
     * @return
     * @throws IOException
     */
    public static DoccatModel trainDocumentCategorizer(String trainingFile) throws IOException {

        DoccatModel model = null;
        InputStream dataIn = null;

        try {
            trainingFile = getResourceFile(trainingFile);
            dataIn = new FileInputStream(trainingFile);
            ObjectStream<String> lineStream =
                    new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
            model = DocumentCategorizerME.train(DEFAULT_LANG, sampleStream);
            return model;
        } finally {
            if (dataIn != null) {
               dataIn.close();
            }
        }
    }

    public static POSModel trainPOSTagger(String trainingFile, String tagDictionaryFile) throws IOException {

        POSModel model = null;
        InputStream dataIn = null;

        try {
            trainingFile = getResourceFile(trainingFile);
            dataIn = new FileInputStream(trainingFile);
            ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream<POSSample> sampleStream = new WordTagSampleStream(lineStream);
            POSTaggerFactory factory = null;

            if(tagDictionaryFile != null) {
                FileInputStream fis = new FileInputStream(tagDictionaryFile);
                POSDictionary tagDictionary = POSDictionary.create(fis);
                factory = new POSTaggerFactory(null, tagDictionary);
            }
            else {
                factory = new POSTaggerFactory();
            }

            model = POSTaggerME.train(DEFAULT_LANG, sampleStream, TrainingParameters.defaultParams(), factory);
            return model;
        }
        finally {
            if (dataIn != null) {
               dataIn.close();
            }
        }
    }

    public static ChunkerModel trainChunker(String trainingFile) throws IOException {

        trainingFile = getResourceFile(trainingFile);
        ObjectStream<String> lineStream =
                new PlainTextByLineStream(new FileInputStream(trainingFile), UTF_8);
        ObjectStream<ChunkSample> sampleStream = new ChunkSampleStream(lineStream);
        ChunkerModel model;

        try {
            ChunkerFactory factory = ChunkerFactory.create(null);
            model = ChunkerME.train(DEFAULT_LANG, sampleStream, TrainingParameters.defaultParams(), factory);
            return model;
        } finally {
            sampleStream.close();
        }
    }

    private static String getResourceFile(String trainingFile) {
        URL resource = null;
        if((resource = NlpTrainingApi.class.getClassLoader().getResource(trainingFile)) != null) {
            trainingFile = resource.getFile();
        }
        return trainingFile;
    }

    public static void serializeModel(BaseModel model, String filename) throws IOException {
        OutputStream modelOut = null;

        try {
            modelOut = new BufferedOutputStream(new FileOutputStream(filename));
            model.serialize(modelOut);
        }
        finally {
            if (modelOut != null) {
                modelOut.close();
            }
        }
    }

    public static HeadRules loadHeadRules(String headRulesFile) throws IOException {
        headRulesFile = getResourceFile(headRulesFile);
        InputStream in = new FileInputStream(headRulesFile);
        HeadRules headRules = new HeadRules(new BufferedReader(new InputStreamReader(in, UTF_8)));
        in.close();
        return headRules;
    }

    public static POSDictionary loadDictionary(String podDictionaryFile) throws IOException {
        podDictionaryFile = getResourceFile(podDictionaryFile);
        InputStream in = new FileInputStream(podDictionaryFile);
        return POSDictionary.create(in);
    }
}
