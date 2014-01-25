import edu.upc.freeling.*;
import weka.classifiers.functions.SMO;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import weka.classifiers.Evaluation;
import weka.filters.unsupervised.attribute.Remove;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Debug;

public class Main {

    public static void main(String[] args) {
        String line =   "it'll be fun, lol";
        Preprocessor pre = new Preprocessor(line);
        System.out.println(pre.result);

        ListSentence ls = Freeling.ParseLine(pre.result);
        ListSentenceIterator sIt = new ListSentenceIterator(ls);
        while (sIt.hasNext()) {
            Sentence s = sIt.next();
            TreeDepnode tree = s.getDepTree();
            Freeling.printDepTree(0, tree);
        }

        ArrayList<String> result = WekaParser("E:/test.arff");
        //for (String aResult : result) System.out.println(aResult);
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
