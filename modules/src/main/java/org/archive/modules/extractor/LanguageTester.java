/*
 * Copyright 2014 Internet Archive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.archive.modules.extractor;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import org.archive.modules.CrawlURI;

/**
 *
 * @author hwikgren
 */
public class LanguageTester {

    public LanguageTester() {
        //makeListOfWantedLanguages();
    }
    
    private static Logger logger =
        Logger.getLogger(ExtractorHTML.class.getName());
    
    String sentence = "";
    String language = "";
    int precedence = 3;
    Socket clientSocket = null;
    DataOutputStream outToServer = null;
    BufferedReader inFromServer = null;
    Random random = new Random();
    byte[] buf;
    int randomPortNumber;
    String finalLanguage;
    
    //Without Hungary
    //List<String> WANTED = Arrays.asList("fin", "ekk", "enf", "enh", "fit", "fkv", "izh", "kca", "koi", "kom", "kpv", "krl", "liv", "lud", "mdf", "mhr", "mns", "mrj", "mtm", "myv", "nio", "olo", "sel", "sia", "sjd", "sje", "sjk", "sjt", "sju", "sma", "sme", "smj", "smn", "sms", "udm", "vep", "vot", "vro", "xas", "yrk");
    
    // With Hungary
    //List<String> WANTED = Arrays.asList("fin", "ekk", "hun", "swe", "enf", "enh", "fit", "fkv", "izh", "kca", "koi", "kom", "kpv", "krl", "liv", "lud", "mdf", "mhr", "mns", "mrj", "mtm", "myv", "nio", "olo", "sel", "sia", "sjd", "sje", "sjk", "sjt", "sju", "sma", "sme", "smj", "smn", "sms", "udm", "vep", "vot", "vro", "xas", "yrk");
    

    // Without large languages
    List<String> WANTED = Arrays.asList("enf", "enh", "fit", "fkv", "izh", "kca", "koi", "kom", "kpv", "krl", "liv", "lud", "mdf", "mhr", "mns", "mrj", "mtm", "myv", "nio", "olo", "sel", "sia", "sjd", "sje", "sjk", "sjt", "sju", "sma", "sme", "smj", "smn", "sms", "udm", "vep", "vot", "vro", "xas", "yrk");
    
    //With Uralic and Finnish
    //List<String> WANTED = Arrays.asList("fin", "enf", "enh", "fit", "fkv", "izh", "kca", "koi", "kom", "kpv", "krl", "liv", "lud", "mdf", "mhr", "mns", "mrj", "mtm", "myv", "nio", "olo", "sel", "sia", "sjd", "sje", "sjk", "sjt", "sju", "sma", "sme", "smj", "smn", "sms", "udm", "vep", "vot", "vro", "xas", "yrk");
    
    //private final ArrayList<String> WANTED = new ArrayList<String>();
    
    private String pageContent;
    
    /*private void makeListOfWantedLanguages() {
        for (String language : Languages) {
            WANTED.add(language);
        }
    }*/
    
    public void testHTML(CrawlURI curi, String modifiable) throws FileNotFoundException, IOException {
        modifyHTML(curi, modifiable);
    }
    
    public void testPDF(CrawlURI curi, String modifiable) throws FileNotFoundException, IOException {
        pageContent = modifiable;
        modify(curi, modifiable);
    }
    
    private void modifyHTML(CrawlURI curi, String modifiable) throws FileNotFoundException, IOException {
         
        modifiable = modifiable.replaceFirst("^[^<]*", "");
        
        modifiable = replaceAll(modifiable, "((<(?is)(?:((script[^>]*+)>[\\d\\D]*?</script)|((style[^>]*+)>[\\d\\D]*?</style)|((head[^>]*+)>[\\d\\D]*?</head)|((nav[^>]*+)>[\\d\\D]*?</nav)|(/*(\\w{1,1024}\\s*[^>]*+))|([!\\?](?!\\[if])[\\d\\D]*?))>)|((&nbsp;{1,})|(\\t{1,})))", " ");
        
        modifiable = replaceAll(modifiable, "<[^p]+>[^<]*</[^p]+>", " ");
        
        modifiable = StringEscapeUtils.unescapeHtml(modifiable);
        
        pageContent = modifiable;
        
        pageContent = replaceAll(pageContent, "\\n\\s*\\n", "\n");
        pageContent = replaceAll(pageContent, " {5,}", " ");
        modify(curi, modifiable);
    }
    
    private void modify(CrawlURI curi, String modifiable) throws UnknownHostException, IOException {
        modifiable = replaceAll(modifiable, "([^\\p{L}\\p{Z}′'’´ʹ])", " ");
        
        modifiable = replaceAll(modifiable, " +", " ");
        
        Pattern p = Pattern.compile("\\p{L}{4,}\\p{L}?");
        Matcher matcher = p.matcher(modifiable);
        int count = 0;
        while(matcher.find()) {
            count ++;
            if (count == 10) {
                break;
            }
        }

        String uri = curi.getURI();
        if (modifiable.length()>300 && count == 10 && curi.is2XXSuccess() && uri.toLowerCase().indexOf("robots.txt")==-1) {
            test(curi, modifiable);
        }
    }
    
    private static String replaceAll(String modifiable, String pattern, String with) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(modifiable);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, with);
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public void test(CrawlURI curi, String modifiable) throws UnknownHostException, IOException {
        finalLanguage = "";
        
        String content = "";
        int len = modifiable.length();
        int start;
        if (len < 1000) {
            start = getPosition(modifiable, (len/2)-150);
            if (start < len-300) {
                //end = getPosition(modifiable, start+300);
                sentence = modifiable.substring(start, start+300);
            }
            else {
                sentence = modifiable;
            }
            //setLanguage(modifiable);
            //finalLanguage = language;
        }
        else {
            for (int i=1; i<4; i++) {
                start = getPosition(modifiable, ((len/4)*i)-50);
                //end = getPosition(modifiable, start+100);
                sentence = sentence+modifiable.substring(start, start+100);
                //language = getLanguage(sentence);
                /*if (testLanguage()) {
                    language = getLanguage(modifiable);
                    if (testLanguage()) {
                        precedence = 1;
                    }
                    finalLanguage = language;
                    break;
                }
                finalLanguage += language+" ";*/
            }
            //setLanguage(modifiable);
            //finalLanguage = language;
        }
        setLanguage(sentence);
        finalLanguage = language;
        
        if (precedence == 1) {
            content = pageContent+"\n";
        }
        //curi.setPrecedence(precedence);
        curi.setLanguage(finalLanguage);
        curi.setContentText(content);
    }
    
    
    
    private int getPosition(String modifiable, int start) {
        
        char c = modifiable.charAt(start);
        while (c != ' ' && start < modifiable.length()-1) {
            start ++;
            c = modifiable.charAt(start);
        }
        return start;
        
    }
    
    private String getLanguage(String sent) throws UnsupportedEncodingException, IOException {
        String lang = "";
        String toBeTested = sent+"\n";
        buf = toBeTested.getBytes("UTF-8");
        
        //randomPortNumber = random.nextInt((8081-8080)+1) +8080;
        
        try {
            clientSocket = new Socket("localhost", 8082);
            //clientSocket = new Socket("localhost", 8080);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            try {
                outToServer.write(buf, 0, buf.length);
            }
            catch (Exception e) {
                logger.log(Level.OFF, "Writing to server failed with: "+e.getMessage());
            }
            try {
                lang = inFromServer.readLine();
            }
            catch (Exception e) {
                logger.log(Level.OFF, "Reading from server failed with: "+e.getMessage());
            }
        }
        catch(UnknownHostException e) {
        logger.log(Level.OFF, "UnknownHostException: "+e.getMessage());

        }
        catch (IOException e) {
            logger.log(Level.OFF, "IOException: "+e.getMessage());
            outToServer.close();
            inFromServer.close();
            clientSocket.close();
            return getLanguage(sent);
        }
        finally {
            outToServer.close();
            inFromServer.close();
            clientSocket.close();
        }
        return lang;
    } 
    
    private boolean testLanguage() {
        if (language == null || language.isEmpty()) {
            language = "xxx10";
            return false;
        }
        if (WANTED.contains(language.substring(0, 3))) {
            return true;
        }
        return false;
    }
    
    private void setLanguage(String modifiable) throws UnsupportedEncodingException, IOException {
        language = getLanguage(sentence);
        if (testLanguage()) {
            //language = getLanguage(modifiable);
            //if (testLanguage()) {
                precedence = 1;
            //}
        }
    }
}
