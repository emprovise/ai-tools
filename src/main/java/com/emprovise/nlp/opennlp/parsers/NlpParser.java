package com.emprovise.nlp.opennlp.parsers;

import com.emprovise.nlp.opennlp.model.PartsOfSentence;
import com.emprovise.nlp.opennlp.model.Rule;
import com.emprovise.nlp.opennlp.model.Sentence;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NlpParser {

	private static Logger log = LoggerFactory.getLogger(NlpParser.class);

    private SentenceDetector sentenceDetector;
    private Parser parser;

    public NlpParser(File sentFile, File parserChunkingFile) throws IOException {
        SentenceModel sentenceModel = new SentenceModel(new FileInputStream(sentFile));
        this.sentenceDetector = new SentenceDetectorME(sentenceModel);

        ParserModel parserModel = new ParserModel(new FileInputStream(parserChunkingFile));
        this.parser = ParserFactory.create(parserModel);
    }

    public NlpParser(SentenceModel sentenceModel, ParserModel parserModel) {
        this.sentenceDetector = new SentenceDetectorME(sentenceModel);
        this.parser = ParserFactory.create(parserModel);
    }

    public Sentence parseSentence(String inputLine) {
        Sentence result = new Sentence();
        result.setText(inputLine);
        inputLine = inputLine.replace(".", " ");
        Parse[] parses = ParserTool.parseLine(inputLine, parser, 1);
        log.info(inputLine);
        collect(result, parses, "");
        log.info("");
        result.postProcess();
        return result;
    }

    private void collect(Sentence result, Parse[] p, String tab) {
        for (Parse parse : p) {
            PartsOfSentence part = useRules(parse);
            //System.out.println(parse.getType() + "_" + parse.getLabel() + "-" + parse.getHead() + " = " + part);

            if (part != null) {
                log.info(parse.getType() + "_" + parse.getLabel() + "-" + parse.getHead() + " = " + part);
                result.addWord(parse.getHead().toString(), part, parse.getType());
            }
            if (parse.getChildCount() != 0) {
                collect(result, parse.getChildren(), tab + "");
            }
        }
    }

    private PartsOfSentence useRules(Parse parse) {
        for (Rule rule : Rule.getRules()) {
            if (rule.getPart().contains(parse.getType()) &&
                    rule.getLabel().equals(parse.getLabel())) {
                return rule.getResult();
            }
        }
        return null;
    }

    public List<Sentence> parseText(String inputText) {
        String[] sentences = sentenceDetector.sentDetect(inputText);
        List<Sentence> result = new ArrayList<Sentence>();
        for (String sentence : sentences) {
            result.add(parseSentence(sentence));
        }

        return result;
    }
}
