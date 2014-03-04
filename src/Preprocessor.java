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
    private String _vulgarSlang = "dicts/vulgar slang.dic";
    private String _htmlDic = "dicts/html.dic";
    private String _positiveEmoticDic = "dicts/positive emoticons.dic";
    private String _negativeEmoticDic = "dicts/negative emoticons.dic";
    public String tweet;
    public String result;
    public Integer positiveEmoticons;
    public Integer negativeEmoticons;
    public Integer consecutiveCases;

    Preprocessor(String tweet) {
        this._tweet = this.tweet = tweet + " ";
        this.HtmlParser();
        this.CountPositivesEmoticons();
        this.CountNegativesEmoticons();
        this.CountCases();
        this.UriParser();
        this.Tweetifier();

        this.Process();
    }

    private void UriParser() {
        String emailPattern = "(mailto:)?([-_\\.\\w])+@[\\.\\w]+\\.[\\w]{2,4}";
        String urlPattern = "(https|http|ftp)://[^\\s]+";
        _tweet = _tweet.replaceAll(urlPattern, " ");
        //Todo: Buscar Regex para el correo
    }

    private void HtmlParser() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(_htmlDic));
            String[] line;
            while (reader.ready()) {
                line = reader.readLine().split("\t");
                _tweet = _tweet.replaceAll(line[0], line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Process() {
        Stack<Integer> s = new Stack<Integer>();
        List<Integer> remove = new ArrayList<Integer>();
        boolean[] trouble = new boolean[256*256];
        trouble['$'] = true;
        trouble['%'] = true;
        trouble['#'] = true;
        trouble['@'] = true;
        trouble['*'] = true;
        trouble['&'] = true;
        trouble['('] = true;
        trouble[')'] = true;
        trouble['"'] = true;

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
            if (!(remove.contains(i) || trouble[_tweet.charAt(i)])) {
                result += _tweet.charAt(i);
            }
        }
    }

    private void Tweetifier() {
        Character[] tags = new Character[2];
        tags[0] = '@';
        tags[1] = '#';
        for (Integer i = 0; i < 2; i++) {
            Pattern p = Pattern.compile("(^|[^\\w])" + tags[i] + "([^ ]+)");
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
            while (reader.ready()) {
                line = reader.readLine().split("\t");
                _tweet = _tweet.replaceAll("[^\\w](?i:" + line[0] + ")[^\\w]", " " + line[1] + " ");
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void CountCases() {
    }

    private void CountNegativesEmoticons() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(_negativeEmoticDic));
            String[] line;
            while (reader.ready()) {
                line = reader.readLine().split("\t");
                negativeEmoticons += _tweet.split(line[0]).length - 1;
                _tweet = _tweet.replaceAll(line[0], line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CountPositivesEmoticons() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(_positiveEmoticDic));
            String[] line;
            while (reader.ready()) {
                line = reader.readLine().split("\t");
                positiveEmoticons += _tweet.split(line[0]).length - 1;
                _tweet = _tweet.replaceAll(line[0], line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

