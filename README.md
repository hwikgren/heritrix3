# Heritrix for language identification

This is a Heritrix implementation used in [Finno-Ugric languages and the Internet project](http://suki.ling.helsinki.fi/eng/project.html) with the language identifier developped in the same project. The Heritrix used was forked from [Internet archive's github page](https://github.com/internetarchive/heritrix3) on November 22, 2017, but most of the changes were already made to version Heritrix-3.1.1.

## How to use this version of Heritrix

The default location of the language identifier is localhost port 8082. You should start the language identifier to listen to this port or change the location in org.archive.extractor.LanguageTester.java in the commons module.

You can change the wanted language list, but the language codes used have to be in the ISO 639-3 standard. You can print the text of all the pages to the warc files by changing the basePresedence for HighestUriQueuePresedencePolicy (BdbFrontier in crawler-beans.cxml) to 1.

The tests of the original heritrix release do not work with the changes, so you have to build the project with tests turned off.

## Changes to the original Heritrix

Using the crawler-beans.cxml file provided, this version of Heritrix crawls for text files only. Links however are extracted also from scripts. Texts are stripped from all code and send to the language identifier. In LanguageTester.java file there is a list of ISO language codes that specify the wanted languages. The text is only written to file if the language is on this list. The links from these wanted pages are given a precedence when added to the queue. From pages whose language is not in the list only language identification code, WARC-Target-URI, and date are written on a warc file.

This heritrix uses a modified webarchive-commons.jar, the code of which can be found here. Its modified files WARCLanguageWriter and WARCLanguageWriterPool are used to write the text and the language identification results to warc files.

### Added files:

org.archive.modules.extractor.ExtractorTextHTML.java 
org.archive.modules.extractor.ExtractorTextPDF.java org.archive.modules.extractor.LanguageTester.java 
org.archive.modules.deciderules.ContentTypeNotMatchesTextDecideRule.java
org.archive.modules.deciderules.MatchesTextOnlyDecideRule.java
org.archive.crawler.frontier.precedence.LanguagePrecedencePolicy.java 
org.archive.modules.LanguagePrecedenceProcessor.java

### Modified files (with added/modified methods):

org.archive.crawler.frontier.WorkQueueFrontier.java
- getAllQueuesCount()
- getRemainingQueueCount()
- getRetiredQueueCount()

org.archive.crawler.framework.Frontier.java
- getAllQueuesCount()
- getRemainingQueueCount()
- getRetiredQueueCount()

org.archive.modules.CrawlURI.java
- setLanguage(String language)
- getLanguage()
- setContentText(String contentText)
- getContentText()

org.archive.crawler.reporting.StatisticTracker.java
- progressStatisticsLegend()

org.archive.crawler.reporting.CrawlStatSnapShot.java
- collect(CrawlController controller, StatisticsTracker stats)
- getProgressStatisticsLine()

FetchHTTP.java
- setCharacterEncoding(CrawlURI curi, final Recorder rec, final HttpResponse response)

RobotsDirectives.java
- allows(String path)
- longestPrefixLength2(ConcurrentSkipListSet<String> prefixSet, String str)


________________________________________________

## The Readme file of the original Heritrix fork:

Readme for Heritrix

====================

Introduction

Crawl Operators!

Getting Started

Developer Documentation

Release History

License

### Introduction

Heritrix is the Internet Archive's open-source, extensible, web-scale, archival-quality web crawler project. Heritrix (sometimes spelled heretrix, or misspelled or missaid as heratrix/heritix/heretix/heratix) is an archaic word for heiress (woman who inherits). Since our crawler seeks to collect and preserve the digital artifacts of our culture for the benefit of future researchers and generations, this name seemed apt.

### Crawl Operators!
Heritrix is designed to respect the robots.txt http://www.robotstxt.org/wc/robots.html exclusion directives and META robots tags http://www.robotstxt.org/wc/exclusion.html#meta. Please consider the load your crawl will place on seed sites and set politeness policies accordingly. Also, always identify your crawl with contact information in the User-Agent so sites that may be adversely affected by your crawl can contact you or adapt their server behavior accordingly.

### Getting Started
See the User Manual at https://webarchive.jira.com/wiki/display/Heritrix/Heritrix+3.0+and+3.1+User+Guide

### Developer Documentation
See http://crawler.archive.org/articles/developer_manual/index.html. For API documentation, see https://webarchive.jira.com/wiki/display/Heritrix/Heritrix+3.x+API+Guide and http://builds.archive.org/javadoc/heritrix-3.2.0/

### Release History
See the Heritrix Release Notes at https://webarchive.jira.com/wiki/display/Heritrix/Release+Notes+-+Heritrix+3.2.0

### License
Heritrix is free software; you can redistribute it and/or modify it under the terms of the Apache License, Version 2.0:

http://www.apache.org/licenses/LICENSE-2.0

Some individual source code files are subject to or offered under other licenses. See the included LICENSE.txt file for more information.

Heritrix is distributed with the libraries it depends upon. The libraries can be found under the 'lib' directory, and are used under the terms of their respective licenses, which are included alongside the libraries in the 'lib' directory.
