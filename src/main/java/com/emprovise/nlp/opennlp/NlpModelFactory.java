package com.emprovise.nlp.opennlp;

import com.emprovise.nlp.util.NetUtil;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.chunker.ChunkerModelLoader;
import opennlp.tools.cmdline.namefind.TokenNameFinderModelLoader;
import opennlp.tools.cmdline.parser.ParserModelLoader;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.cmdline.tokenizer.TokenizerModelLoader;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.model.BaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.emprovise.nlp.opennlp.NlpBinary.*;

public class NlpModelFactory {

    private static Logger log = LoggerFactory.getLogger(NlpApi.class);

    private Map<String, BaseModel> modelMap = new HashMap<String, BaseModel>();

    private Map<NlpBinary, File> opennlpMap = new HashMap<NlpBinary, File>();

    public NlpModelFactory(NetUtil netUtil, String opennlpBaseDir) {
        try {
            for (NlpBinary opennlpBinary : NlpBinary.values()) {
                URL opennlpUrl = new URL(opennlpBinary.getUrl());
                File opennlpFile = netUtil.downloadFile(opennlpUrl, opennlpBinary.value(), opennlpBaseDir);
                opennlpMap.put(opennlpBinary, opennlpFile);
                log.info(opennlpFile.getAbsolutePath() + " Loaded...");
            }
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    public TokenizerModel getTokenizerModel(File file) {
        return new TokenizerModelLoader().load(file);
    }

    public SentenceModel getSentenceModel(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        SentenceModel model = new SentenceModel(is);
        is.close();
        return model;
    }

    public TokenNameFinderModel getTokenNameFinderModel(File file) {
        return new TokenNameFinderModelLoader().load(file);
    }

    public POSModel getPOSModel(File file) {
        return new POSModelLoader().load(file);
    }

    public ParserModel getParserModel(File file) {
        return new ParserModelLoader().load(file);
    }

    public ChunkerModel getChunkerModel(File file) {
        return new ChunkerModelLoader().load(file);
    }

    public BaseModel getModel(NlpBinary nlpBinary) {
        try {
            if (modelMap.containsKey(nlpBinary.value())) {
                return modelMap.get(nlpBinary.value());
            } else {
                Method method = getClass().getMethod("get" + nlpBinary.type().getSimpleName(), File.class);
                BaseModel model = (BaseModel) method.invoke(this, opennlpMap.get(nlpBinary));
                modelMap.put(nlpBinary.value(), model);
                return model;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public TokenizerModel getTokenizerModel() {
        return (TokenizerModel) getModel(EN_TOKEN);
    }

    public SentenceModel getSentenceModel() {
        return (SentenceModel) getModel(EN_SENT);
    }

    public TokenNameFinderModel getTokenNameFinderModelByPerson() {
        return (TokenNameFinderModel) getModel(EN_NER_PERSON);
    }

    public TokenNameFinderModel getTokenNameFinderModelByOrganization() {
        return (TokenNameFinderModel) getModel(EN_NER_ORGANIZATION);
    }

    public TokenNameFinderModel getTokenNameFinderModelByLocation() {
        return (TokenNameFinderModel) getModel(EN_NER_LOCATION);
    }

    public TokenNameFinderModel getTokenNameFinderModelByPercentage() {
        return (TokenNameFinderModel) getModel(EN_NER_PERCENTAGE);
    }

    public TokenNameFinderModel getTokenNameFinderModelByTime() {
        return (TokenNameFinderModel) getModel(EN_NER_TIME);
    }

    public TokenNameFinderModel getTokenNameFinderModelByMoney() {
        return (TokenNameFinderModel) getModel(EN_NER_MONEY);
    }

    public TokenNameFinderModel getTokenNameFinderModelByDate() {
        return (TokenNameFinderModel) getModel(EN_NER_DATE);
    }

    public TokenNameFinderModel getTokenNameFinderModel(NlpBinary nlpBinary) {
        return (TokenNameFinderModel) getModel(nlpBinary);
    }

    public POSModel getPOSMaxentModel() {
        return (POSModel) getModel(EN_POS_MAXENT);
    }

    public POSModel getPOSPerceptronModel() {
        return (POSModel) getModel(EN_POS_PERCEPTRON);
    }

    public ParserModel getParserModel() {
        return (ParserModel) getModel(EN_PARSER_CHUNKING);
    }

    public ChunkerModel getChunkerModel() {
        return (ChunkerModel) getModel(EN_CHUNKER);
    }

    public void addModel(String modelName, BaseModel model) {
        if(NlpBinary.containsValue(modelName)) {
            throw new IllegalArgumentException("Invalid value for model name.");
        }

        modelMap.put(modelName, model);
    }

    public <T> T getModel(String modelName, Class T) {
        return (T) modelMap.get(modelName);
    }
}
