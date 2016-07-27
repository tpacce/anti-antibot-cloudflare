package com.github.alessiop86.antiantibotcloudflare;

import com.github.alessiop86.antiantibotcloudflare.exceptions.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private static final String BASE_REGEX =	"setTimeout\\(function\\(\\)\\{\\s+(var s,t,o,p,b,r,e,a,k,i,n,g,f.+?\\r?\\n[\\s\\S]+?a\\.value =.+?)\\r?\\n";
    private static final Pattern patternRegex = Pattern.compile(BASE_REGEX);

    private static final String CHALLENGE_FORM_SELECTOR = "#challenge-form";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String INPUT_FIELD_1 = "pass";
    private static final String INPUT_FIELD_2 = "jschl_vc";

    private final String field1;
    private final String field2;
    private final String jsChallenge;

    public String getField2() {
        return field2;
    }

    public String getField1() {
        return field1;
    }


    public Parser(String document) throws ParseException {
        try {
            Element form = findForm(document);
            Elements elementsMatchingField1 = form.getElementsByAttributeValue(NAME_ATTRIBUTE, INPUT_FIELD_1);
            Elements elementsMatchingField2 = form.getElementsByAttributeValue(NAME_ATTRIBUTE, INPUT_FIELD_2);
            if (elementsMatchingField1.size() != 1 || elementsMatchingField2.size() != 1) {
                throw new ParseException("The challenge form format has changed. New format:" + form.html());
            }

            field1 = elementsMatchingField1.attr(VALUE_ATTRIBUTE);
            field2 = elementsMatchingField2.attr(VALUE_ATTRIBUTE);

            jsChallenge = extractJsChallenge(document);
        }
        catch(ParseException e) {
            throw e;
        }
        catch(Exception e) {
            throw new ParseException(e);
        }
    }

    private String extractJsChallenge(String document) {
        Matcher matcher = patternRegex.matcher(document);
        matcher.find();
        String step1 =  matcher.group(1);
        String step2 = step1.replaceFirst("a\\.value = (parseInt\\(.+?\\)).+","$1");
        String step3 = step2.replaceAll("\\s{3,}[a-z](?: = |\\.).+","");
        return step3.replaceFirst("[\\n\\\\']","");
    }

    private Element findForm(String document) throws ParseException {
        Elements forms = Jsoup.parse(document).select(CHALLENGE_FORM_SELECTOR);
        if (forms.size() == 0)
            throw new ParseException("The challenge format has changed. New format:" + document);
        return forms.get(0);
    }

    public String getJsChallenge() {
        return jsChallenge;
    }
}
