import edu.upc.freeling.*;
import weka.classifiers.functions.SMO;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import weka.classifiers.Evaluation;
import weka.filters.unsupervised.attribute.Remove;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Debug;

public class Main {

    public static void main(String[] args) {
        try {
            CorpusReader testCp = new CorpusReader("input/test.txt");
            CorpusReader negCp = new CorpusReader("input/neg.tsv");
            CorpusReader posCp = new CorpusReader("input/pos.tsv");
            CorpusReader neuCp = new CorpusReader("input/neu.tsv");
            Resource negTest = CreateResource(negCp, "output/test.neg");
            Resource negRes = CreateResource(negCp, "output/res.neg");
            Resource posRes = CreateResource(posCp, "output/res.pos");
            Resource neuRes = CreateResource(neuCp,"output/res.neu");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static Resource CreateResource(CorpusReader cp, String path) {
        try {
            Resource res = new Resource(path);
            String line;  int pp =0;
            while ((line = cp.GetLine()) != null) {
                Preprocessor pre = new Preprocessor(line);
                System.out.println(pp++ + ":>>> " +pre.result);
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
}
