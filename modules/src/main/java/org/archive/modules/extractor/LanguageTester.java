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
 * @author Heidi Jauhiainen
 */
public class LanguageTester {
    
    // SET language identifier SERVER ADDRESS
    String server = "localhost";
    // SET PORT NUMBER where language identifier is waiting
    int port = 8082;
    // list of Uralic language codes for precedence
    // SET YOUR OWN LIST of languages presedence wanted
    List<String> WANTED = Arrays.asList("enf", "enh", "fit", "fkv", "izh", "kca", "koi", "kom", "kpv", "krl", "liv", "lud", "mdf", "mhr", "mns", "mrj", "mtm", "myv", "nio", "olo", "sel", "sia", "sjd", "sje", "sjk", "sjt", "sju", "sma", "sme", "smj", "smn", "sms", "udm", "vep", "vot", "vro", "xas", "yrk");
    private static Logger logger = Logger.getLogger(ExtractorHTML.class.getName());
    String sentence = "";
    String language = "";
    int precedence = 3;
    Socket clientSocket = null;
    DataOutputStream outToServer = null;
    BufferedReader inFromServer = null;
    byte[] buf;
    private String pageContent;

    public LanguageTester() {
    }
    
    public void testHTML(CrawlURI curi, String modifiable) throws FileNotFoundException, IOException {
        modifyHTML(curi, modifiable);
    }
    
    public void testPDF(CrawlURI curi, String modifiable) throws FileNotFoundException, IOException {
        pageContent = modifiable;
        // when pdf, no code so send directly to modify()
        modify(curi, modifiable);
    }
    
    /**
     * remove code from text
     * @param curi
     * @param modifiable
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void modifyHTML(CrawlURI curi, String modifiable) throws FileNotFoundException, IOException {
        // remove space from the very beginning of document
        modifiable = modifiable.replaceFirst("^[^<]*", "");
        //remove script, style, head and nav tags and the text in between
        // also replace any tag (without text) etc.
        modifiable = replaceAll(modifiable, "((<(?is)(?:((script[^>]*+)>[\\d\\D]*?</script)|((style[^>]*+)>[\\d\\D]*?</style)|((head[^>]*+)>[\\d\\D]*?</head)|((nav[^>]*+)>[\\d\\D]*?</nav)|(/*(\\w{1,1024}\\s*[^>]*+))|([!\\?](?!\\[if])[\\d\\D]*?))>)|((&nbsp;{1,})|(\\t{1,})))", " ");
        // remove all other tags except <p> and the text in between
        modifiable = replaceAll(modifiable, "<[^p]+>[^<]*</[^p]+>", " ");
        
        modifiable = StringEscapeUtils.unescapeHtml(modifiable);
        
        // save stripped text for later before removing spaces for language identification
        pageContent = modifiable;
        // remove extra spaces
        pageContent = replaceAll(pageContent, "\\n\\s*\\n", "\n");
        pageContent = replaceAll(pageContent, " {5,}", " ");
        modify(curi, modifiable);
    }
    
    /**
     * remove line carriages and extra spaces
     * @param curi
     * @param modifiable
     * @throws UnknownHostException
     * @throws IOException 
     */
    private void modify(CrawlURI curi, String modifiable) throws UnknownHostException, IOException {
        // remove everything that is not a letter, whitespace or apostroph
        modifiable = replaceAll(modifiable, "([^\\p{L}\\p{Z}′'’´ʹ])", " ");
        // remove extra spaces
        modifiable = replaceAll(modifiable, " +", " ");
        
        // make sure text has at least 10 4-letter words
        Pattern p = Pattern.compile("\\p{L}{4,}\\p{L}?");
        Matcher matcher = p.matcher(modifiable);
        int count = 0;
        while(matcher.find()) {
            count ++;
            if (count == 10) {
                break;
            }
        }

        // send for testing if long enough and there's enough actual words
        if (modifiable.length()>300 && count == 10) {
            test(curi, modifiable);
        }
    }
    
    /**
     * 
     * @param modifiable (string where replacement is done)
     * @param pattern (which part is replaced)
     * @param with (with what is replaced)
     * @return modified string
     */
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
    
    /**
     * get part of the string, send it to be tested and set curi language and text
     * @param curi
     * @param modifiable
     * @throws UnknownHostException
     * @throws IOException 
     */
    public void test(CrawlURI curi, String modifiable) throws UnknownHostException, IOException {
        String content = "";
        int len = modifiable.length();
        int start;
        // from short text get string length = 300 from the middle
        if (len < 1000) {
            start = getIndex(modifiable, (len/2)-150);
            if (start < len-300) {
                sentence = modifiable.substring(start, start+300);
            }
            else {
                sentence = modifiable;
            }
        }
        // else get 3 x 100 evenly from different parts
        else {
            for (int i=1; i<4; i++) {
                start = getIndex(modifiable, ((len/4)*i)-50);
                sentence = sentence+modifiable.substring(start, start+100);
            }
        }
        // get language (and possibly change precedence) for this text
        getLanguage(sentence);
        // if wanted language (i.e. precedence is 1), set curi page text as curi content
        // otherwise curi content = ""
        if (precedence == 1) {
            content = pageContent+"\n";
        }
        curi.setContentText(content);
        curi.setLanguage(language);
        
    }
    
    /**
     * get index of next first letter of a word after given position of modifiable
     * @param modifiable
     * @param start
     * @return 
     */
    private int getIndex(String modifiable, int start) {
        char c = modifiable.charAt(start);
        while (c != ' ' && start < modifiable.length()-1) {
            start ++;
            c = modifiable.charAt(start);
        }
        return start;
        
    }
    
    /**
     * test language and if one of the wanted languages, test again with the whole text
     * @param modifiable
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    private void getLanguage(String modifiable) throws UnsupportedEncodingException, IOException {
        language = testLanguage(sentence);
        if (isWanted(language)) {
            language = testLanguage(modifiable);
            // if after second test still one of the wanted languages, set precedence to 1
            if (isWanted(language)) {
                precedence = 1;
            }
        }
    }
    
    /**
     * send string to language tester
     * @param sent
     * @return
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    private String testLanguage(String sent) throws UnsupportedEncodingException, IOException {
        String lang = "";
        String toBeTested = sent+"\n";
        buf = toBeTested.getBytes("UTF-8");
        try {
            clientSocket = new Socket(server, port);
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
        // catching ioException try again
        catch (IOException e) {
            logger.log(Level.OFF, "IOException: "+e.getMessage());
            outToServer.close();
            inFromServer.close();
            clientSocket.close();
            return testLanguage(sent);
        }
        finally {
            outToServer.close();
            inFromServer.close();
            clientSocket.close();
        }
        return lang;
    } 
    
    private boolean isWanted(String lang) {
        if (lang == null || lang.isEmpty()) {
            language = "xxx10";
            return false;
        }
        if (WANTED.contains(lang.substring(0, 3))) {
            return true;
        }
        return false;
    }
    
    
}
