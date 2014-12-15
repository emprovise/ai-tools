package com.emprovise.nlp.opennlp;

import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerModel;

public enum NlpBinary {

    EN_TOKEN("en-token.bin", TokenizerModel.class),
    EN_SENT("en-sent.bin", SentenceModel.class),
    EN_POS_MAXENT("en-pos-maxent.bin", POSModel.class),
    EN_POS_PERCEPTRON("en-pos-perceptron.bin", POSModel.class),
    EN_NER_DATE("en-ner-date.bin", TokenNameFinderModel.class),
    EN_NER_LOCATION("en-ner-location.bin", TokenNameFinderModel.class),
    EN_NER_MONEY("en-ner-money.bin", TokenNameFinderModel.class),
    EN_NER_ORGANIZATION("en-ner-organization.bin", TokenNameFinderModel.class),
    EN_NER_PERCENTAGE("en-ner-percentage.bin", TokenNameFinderModel.class),
    EN_NER_PERSON("en-ner-person.bin", TokenNameFinderModel.class),
    EN_NER_TIME("en-ner-time.bin", TokenNameFinderModel.class),
    EN_CHUNKER("en-chunker.bin", ChunkerModel.class),
    EN_PARSER_CHUNKING("en-parser-chunking.bin", ParserModel.class);

    private String name;

    private Class type;

    private NlpBinary(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    public String value() {
        return name;
    }

    public Class type() {
        return type;
    }

    @Override
    public String toString(){
        return name;
    }

    public String getUrl(){
        return OPENNLP_BASE_URI + name;
    }

    public static boolean containsValue(String string) {

        for (NlpBinary binary : NlpBinary.values()) {
            if (binary.value().equals(string)) {
                return true;
            }
        }

        return false;
    }

    public static final String OPENNLP_BASE_URI = "http://opennlp.sourceforge.net/models-1.5/";
}
