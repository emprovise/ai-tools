package com.emprovise.nlp.opennlp.model;

import java.util.ArrayList;
import java.util.List;

public class Rule {

    private List<String> part;
    private String label;
    private PartsOfSentence result;

    @SuppressWarnings("serial")
	private static List<Rule> rules = new ArrayList<Rule>() {{
        Rule subject = new Rule();
        subject.label = "S-S";
        subject.part = new ArrayList<String>() {{
            add("NP");
            add("NNS");
            add("NNP");
        }};
        subject.result = PartsOfSentence.SUBJECT;
        add(subject);

        subject = new Rule();
        subject.label = "C-S";
        subject.part = new ArrayList<String>() {{
            add("NP");
        }};
        subject.result = PartsOfSentence.SUBJECT;
        add(subject);

        subject = new Rule();
        subject.label = "S-VP";
        subject.part = new ArrayList<String>() {{
            add("NP");
        }};
        subject.result = PartsOfSentence.SUBJECT;
        add(subject);


        subject = new Rule();
        subject.label = "S-VP";
        subject.part = new ArrayList<String>() {{
            add("VBD");
            add("VBZ");
            add("VBP");
        }};
        subject.result = PartsOfSentence.PREDICATE;
        add(subject);
        subject = new Rule();
        subject.label = "C-VP";
        subject.part = new ArrayList<String>() {{
            add("VP");
        }};
        subject.result = PartsOfSentence.PREDICATE;
        add(subject);

        subject = new Rule();
        subject.label = "C-VP";
        subject.part = new ArrayList<String>() {{
            add("NP");
            add("ADVP");
            add("RB");
        }};
        subject.result = PartsOfSentence.OBJECT;
        add(subject);


        subject = new Rule();
        subject.label = "C-PP";
        subject.part = new ArrayList<String>() {{
            add("NP");
        }};
        subject.result = PartsOfSentence.OBJECT;
        add(subject);
    }};

    public static List<Rule> getRules() {
        return rules;
    }

    public List<String> getPart() {
        return part;
    }

    public String getLabel() {
        return label;
    }

    public PartsOfSentence getResult() {
        return result;
    }
}
