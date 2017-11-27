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
package org.archive.crawler.frontier.precedence;

import org.archive.modules.CrawlURI;

/**
 *  Precedence based on language of the page where link was found
 *  Get precedence from Curi
 * @author Heidi Jauhiainen
 */
public class LanguageUriPrecedencePolicy extends BaseUriPrecedencePolicy {
    private static final long serialVersionUID = 1L;

    @Override
    protected int calculatePrecedence(CrawlURI curi) {
        if (curi.getPrecedence() > 0) {
            return curi.getPrecedence();
        }
        return super.calculatePrecedence(curi);
    }
}
