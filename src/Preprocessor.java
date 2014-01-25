import com.sun.javafx.collections.transformation.SortedList;
import sun.misc.Regexp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Andy
 * Date: 25/01/14
 * Time: 18:33
 * To change this template use File | Settings | File Templates.
 */
public class Preprocessor {

    private String _tweet;
    private String _vulgarSlang="vulgar slang.dic";
    public String tweet;
    public String result;
    public Integer positiveEmoticons;
    public Integer negativeEmoticons;
    public Integer consecutiveCases;

    Preprocessor(String tweet) {
        this._tweet = this.tweet = tweet+" ";
        this.CountPositivesEmoticons();
        this.CountNegativesEmoticons();
        this.CountCases();
        this.Tweetifier();

        this.Process();
    }

    private void Process() {
        Stack<Integer> s = new Stack<Integer>();
        List<Integer> remove = new ArrayList<Integer>();
        result = "";
        _tweet = _tweet.replace('.', ';');
        if (_tweet.charAt(_tweet.length() - 1) != '.') {
            _tweet += ".";
        }

        Character[][] check = new Character[3][2];
        check[0][0] = '(';
        check[0][1] = ')';
        check[1][0] = '{';
        check[1][1] = '}';
        check[2][0] = '[';
        check[2][1] = ']';
        for (Integer index = 0; index < 3; index++) {
            for (Integer i = 0; i < _tweet.length(); i++) {
                if (_tweet.charAt(i) == check[index][0]) {
                    s.push(i);
                } else if (_tweet.charAt(i) == check[index][1]) {
                    if (s.empty()) {
                        remove.add(i);
                    } else {
                        s.pop();
                    }
                }
            }
            while (!s.empty()) {
                remove.add(s.pop());
            }
        }
//        SortedList<Integer> sRemove = new SortedList<Integer>(remove);
        for (Integer i = 0; i < _tweet.length(); i++) {
            if (!remove.contains(i)) {
                result += _tweet.charAt(i);
            }
        }
    }

    private void Tweetifier() {
        Character[] tags = new Character[2];
        tags[0] = '@';
        tags[1] = '#';
        for (Integer i = 0; i < 2; i++) {
            Pattern p = Pattern.compile("(^|[^\\w])"+tags[i] + "([^ ]+)");
            Matcher m = p.matcher(_tweet);
            while (m.find()) { // Find each match in turn; String can't do this.
                String name = m.group(2); // Access a submatch group; String can't do this.
                String aux = "";
                aux += name.charAt(0);
                aux = aux.toUpperCase() + name.substring(1);
                _tweet = _tweet.replace(tags[i] + name, aux);
            }
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(_vulgarSlang));
            String[] line;
            while (reader.ready()){
                line = reader.readLine().split("\t");
                _tweet = _tweet.replaceAll("[^\\w](?i:"+line[0]+")[^\\w]"," "+line[1]+" ");
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void CountCases() {
    }

    private void CountNegativesEmoticons() {
    }

    private void CountPositivesEmoticons() {
    }

}

