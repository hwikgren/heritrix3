/*
 * Copyright 2017 Internet Archive.
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
package org.archive.modules.deciderules;

import java.util.regex.Pattern;
import org.archive.modules.CrawlURI;

/**
 * DecideRule whose decision is applied if the URI's content-type 
 * is present and does not match the supplied regular expression. 
 * 
 * @author Olaf Freyer
 * ContentTypeNotMatchesRegexDecideRule
 * modified by Heidi Jauhiainen
 * only accept documents where content type is text/html/pdf/rtf
 * use with shouldFetchBodyRule: REJECT
 */
public class ContentTypeNotMatchesTextDecideRule extends
        ContentTypeMatchesRegexDecideRule {
    private static final long serialVersionUID = 4729800377757426137L;

    public ContentTypeNotMatchesTextDecideRule() {
    }
    
    /**
     * Evaluate whether given object's string version does not match 
     * configured regex (by reversing the superclass's answer).
     * 
     * @param o CrawlURI to make decision about.
     * @return true if the regex is not matched
     */
    @Override
    protected boolean evaluate(CrawlURI o) {
        return !super.evaluate(o);
    }
    
    @Override
    protected String getString(CrawlURI uri) {
        return uri.getContentType();
    }
    
    @Override
    public Pattern getRegex() {
        String regex = ".*(?i)((text((\\/(html|rtf){1})|(-plain){1}))|(application(\\/(pdf|ps)){1})|pdf).*";
        return Pattern.compile(regex);
    }
}
