import Mesure.Mesure;
import Mesure.SentiMesure;
import SentiGraph.Graph;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import edu.upc.freeling.*;
import weka.classifiers.functions.SMO;
import weka.core.FastVector;
import weka.core.Instances;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weka.classifiers.Evaluation;
import weka.filters.unsupervised.attribute.Remove;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Debug;

public class Main {

    public static void main(String[] args) {
        try {

//            CorpusReader negCp = new CorpusReader("input/neg.tsv");
//            CorpusReader posCp = new CorpusReader("input/pos.tsv");
//            CorpusReader neuCp = new CorpusReader("input/neu.tsv");

//            Resource negRes = CreateResource(negCp, "output/res.neg");
//            Resource posRes = CreateResource(posCp, "output/res.pos");
//            Resource neuRes = CreateResource(neuCp, "output/res.neu");

//            SentiMesure sm = new SentiMesure(posRes.getGraph(),negRes.getGraph(),neuRes.getGraph(),"output/mes.tsv");

            SentiMesure sm = new SentiMesure("output/mes.tsv");
            ArrayList<String> model = TrainModel(sm, "input/neg.tsv", "input/neu.tsv", "input/pos.tsv");
            SaveModel(model,"arffs/model.arff");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void SaveModel(ArrayList<String> model, String path) {
            String buffer = "@relation Evaluation\r\n" +
                    "@attribute pos numeric\r\n" +
                    "@attribute neg numeric\r\n" +
                    "@attribute obj numeric\r\n" +
                    "@attribute excl numeric\r\n" +
                    "@attribute inter numeric\r\n" +
                    "@attribute happy_emot numeric\r\n" +
                    "@attribute sad_emot numeric\r\n" +
                    "@attribute pos_count numeric\r\n" +
                    "@attribute neg_count numeric\r\n" +
                    "@attribute pos_measured_count numeric\r\n" +
                    "@attribute neg_measured_count numeric\r\n" +
                    "@attribute obj_measured_count numeric\r\n" +
                    "@attribute neu numeric\r\n" +
                    "@attribute neu_real_count numeric\r\n" +
                    "@attribute WORDS_count numeric\r\n" +
                    "@attribute WORDS_pos numeric\r\n" +
                    "@attribute WORDS_pos_count_res numeric\r\n" +
                    "@attribute WORDS_pos_count_dict numeric\r\n" +
                    "@attribute WORDS_neg numeric\r\n" +
                    "@attribute WORDS_neg_count_res numeric\r\n" +
                    "@attribute WORDS_neg_count_dict numeric\r\n" +
                    "@attribute eval {positive,negative,neutral}\r\n" +
                    "@data" +
                    "\r\n";
            for (String line : model)
                buffer += line + "\r\n";
            WriteText(path, buffer);
    }

    private static ArrayList<String> TrainModel(SentiMesure sm, String negPath, String neuPath, String posPath) throws IOException {
        ArrayList<String> model = new ArrayList<String>();
        CorpusReader negCp = new CorpusReader(negPath);
        CorpusReader posCp = new CorpusReader(posPath);
        CorpusReader neuCp = new CorpusReader(neuPath);
        CorpusReader[] cp = new CorpusReader[3];
        cp[0]= negCp;
        cp[1]= posCp;
        cp[2]= neuCp;
        String[] classify = new String[3];
        classify[0]="negative";
        classify[1]="positive";
        classify[2]="neutral";

        TreeSet<String> pwords = new TreeSet<String>();
        TreeSet<String> nwords = new TreeSet<String>();

        for (String s : ReadLines("dicts/pos words.dic"))
            pwords.add(s);
        for (String s : ReadLines("dicts/neg words.dic"))
            nwords.add(s);

        String line;
        int pp = 0;
        for (int ii = 0; ii < 3; ii++) {
            while ((line = cp[ii].GetLine()) != null) {
                if (line.equals("Not Available")) continue;

                Preprocessor pre = new Preprocessor(line);
                System.out.println(pp++ + ":>>> " + pre.result);
                ArrayList<String> lemmas = new ArrayList<String>();
                ArrayList<String> values = new ArrayList<String>();
                Graph graph = new Graph(0);

                ListSentence ls = Freeling.ParseLine(pre.result);
                ListSentenceIterator sIt = new ListSentenceIterator(ls);
                while (sIt.hasNext()) {
                    Sentence s = sIt.next();
                    TreeDepnode tree = s.getDepTree();
                    lemmas.add(tree.getInfo().getWord().getLemma());
                    values.add(tree.getInfo().getWord().getForm());
                    graph.AddNode(tree.getInfo().getWord().getLemma());

                    ArrayList<TreeDepnode> stack = new ArrayList<TreeDepnode>();
                    Integer index = 1;
                    stack.add(tree);
                    while (index > 0) {
                        index--;
                        tree = stack.get(index);

                        for (int i = 0; i < tree.numChildren(); i++) {
                            TreeDepnode child = tree.nthChildRef(i);
                            Word w = child.getInfo().getWord();
                            lemmas.add(w.getLemma());
                            values.add(w.getForm());
                            graph.AddNode(w.getLemma());
                            graph.AddEdge(w.getLemma(), tree.getInfo().getWord().getLemma(), 1);

                            if (stack.size() > index)
                                stack.add(index, child);
                            else
                                stack.add(child);
                            index++;
                        }
                    }
                }

                Double pos = 0.0,
                        neg = 0.0,
                        obj = 0.0,
                        excl = 0.0,
                        interr = 0.0,
                        happy_emot = 0.0,
                        sad_emot = 0.0,
                        pos_count = 0.0,
                        neg_count = 0.0,

                        pos_measured_count = 0.0,
                        neg_measured_count = 0.0,
                        obj_measured_count = 0.0,

                        neu = 0.0,
                        neu_real_count = 0.0,

                        WORDS_count = 0.0,
                        WORDS_pos = 0.0,
                        WORDS_pos_count_res = 0.0,
                        WORDS_pos_count_dict = 0.0,
                        WORDS_neg = 0.0,
                        WORDS_neg_count_res = 0.0,
                        WORDS_neg_count_dict = 0.0;

                ArrayList<String> WORDS = new ArrayList<String>();
                Pattern pattern = Pattern.compile("\\W([A-Z]+)\\W");
                Matcher matcher = pattern.matcher(pre.result);
                while (matcher.find())
                    WORDS.add(pre.result.substring(matcher.start(), matcher.end()));
                for (String WORD : WORDS) {
                    if (pwords.contains(WORD.toLowerCase()))
                        WORDS_pos_count_dict += 1.0;
                    if (nwords.contains(WORD.toLowerCase()))
                        WORDS_neg_count_dict += 1.0;
                    if (sm.Mesures.containsKey(WORD)) {
                        Mesure kk = sm.Mesures.get(WORD);
                        WORDS_pos_count_res += kk.getPositive() > 0 ? 1 : 0;
                        WORDS_neg_count_res += kk.getNegative() > 0 ? 1 : 0;
                        WORDS_pos += kk.getPositive();
                        WORDS_neg += kk.getNegative();
                    }
                }

                excl = line.split("!").length - 1.0;
                interr = line.split("\\?").length - 1.0;
                excl /= excl + interr;
                interr /= excl + interr;
                happy_emot += pre.positiveEmoticons;
                sad_emot += pre.negativeEmoticons;

                for (int i = 0; i < lemmas.size(); i++) {
                    if (sm.Mesures.containsKey(lemmas.get(i))) {
                        Mesure kk = sm.Mesures.get(lemmas.get(i));
                        pos += kk.getPositive();
                        neg += kk.getNegative();
                        obj += kk.getObjectiveCalc();
                        //cambios competición//
                        pos_measured_count += kk.getPositive() > 0 ? 1 : 0;
                        neg_measured_count += kk.getNegative() > 0 ? 1 : 0;
                        obj_measured_count += kk.getObjectiveCalc() > 0 ? 1 : 0;
                        //
                        neu += kk.getNeutral();
                        //
                        neu_real_count += kk.getNeutral() > 0 ? 1 : 0;
                    }
                    if (pwords.contains(values.get(i)) || pwords.contains(lemmas.get(i)))
                        pos_count += 1.0;
                    if (nwords.contains(values.get(i)) || nwords.contains(lemmas.get(i)))
                        neg_count += 1.0;
                }
                model.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s"
                        , pos.toString().replace(',', '.')
                        , neg.toString().replace(',', '.')
                        , obj.toString().replace(',', '.')
                        , excl.toString().replace(',', '.')
                        , interr.toString().replace(',', '.')
                        , happy_emot.toString().replace(',', '.')
                        , sad_emot.toString().replace(',', '.')
                        , pos_count.toString().replace(',', '.')
                        , neg_count.toString().replace(',', '.')
                        , pos_measured_count.toString().replace(',', '.')
                        , neg_measured_count.toString().replace(',', '.')
                        , obj_measured_count.toString().replace(',', '.')
                        , neu.toString().replace(',', '.')
                        , neu_real_count.toString().replace(',', '.')
                        , WORDS_count.toString().replace(',', '.')
                        , WORDS_pos.toString().replace(',', '.')
                        , WORDS_pos_count_res.toString().replace(',', '.')
                        , WORDS_pos_count_dict.toString().replace(',', '.')
                        , WORDS_neg.toString().replace(',', '.')
                        , WORDS_neg_count_res.toString().replace(',', '.')
                        , WORDS_neg_count_dict.toString().replace(',', '.')
                        , classify[ii]
                ));
            }
        }
        return model;
    }

    private static Resource CreateResource(CorpusReader cp, String path) {
        try {
            Resource res = new Resource(path);
            String line;
            int pp = 0;
            while ((line = cp.GetLine()) != null) {
                Preprocessor pre = new Preprocessor(line);
                System.out.println(pp++ + ":>>> " + pre.result);
                ListSentence ls = Freeling.ParseLine(pre.result);

                ListSentenceIterator sIt = new ListSentenceIterator(ls);
                while (sIt.hasNext()) {
                    Sentence s = sIt.next();
                    TreeDepnode tree = s.getDepTree();
                    res.Increment(tree.getInfo().getWord().getLemma());

                    ArrayList<TreeDepnode> stack = new ArrayList<TreeDepnode>();
                    Integer index = 1;
                    stack.add(tree);
                    while (index > 0) {
                        index--;
                        tree = stack.get(index);

                        for (int i = 0; i < tree.numChildren(); i++) {
                            TreeDepnode child = tree.nthChildRef(i);
                            Word w = child.getInfo().getWord();
                            res.Increment(w.getLemma());
                            res.AddLink(w.getLemma(), tree.getInfo().getWord().getLemma());

                            if (stack.size() > index)
                                stack.add(index, child);
                            else
                                stack.add(child);
                            index++;
                        }
                    }
                }
            }
            res.Save();
            return res;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }


    private static ArrayList<String> WekaParser(String path) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            Instances train = new Instances(reader);
            reader.close();

            train.setClassIndex(train.numAttributes() - 1);

            Remove rm = new Remove();
            rm.setAttributeIndices("1");  // remove 1st attribute

            FilteredClassifier fc = new FilteredClassifier();
            fc.setFilter(rm);

            // build 1. SMO
            SMO smo = new SMO();
            smo.buildClassifier(train);

            fc.setClassifier(smo);
            fc.buildClassifier(train);

            for (int i = 0; i < train.numInstances(); i++) {
                double pred = fc.classifyInstance(train.instance(i));
                result.add("ID: " + train.instance(i).value(0) +
                        ", actual: " + train.classAttribute().value((int) train.instance(i).classValue()) +
                        ", predicted: " + train.classAttribute().value((int) pred) +
                        "\r\n" + train.classAttribute().value((int) pred));
            }

            Evaluation obj = new Evaluation(train);

            obj.crossValidateModel(smo, train, 10, new Debug.Random(1));
            System.out.println(obj.weightedPrecision());
            System.out.println(obj.weightedRecall());
            System.out.println(obj.weightedFMeasure());
            System.out.println(obj.correct());
            System.out.println(obj.incorrect());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static ArrayList<String> ReadLines(String name) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(name));
            ArrayList<String> lines = new ArrayList<String>();
            while (reader.ready())
                lines.add(reader.readLine());
            return lines;
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    private static void WriteText(String name, String buffer) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(name));
            writer.write(buffer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
