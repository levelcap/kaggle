package com.brave;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Hello world!
 */
public class App {
    private static final String SEPARATOR = "\n================================================================\n================================================================\n";
    private static final String KAGGLE_URL = "https://www.kaggle.com";
    private static final String DATASETS_FILENAME = "datasets.out";
    private static final String FORUMS_FILENAME = "forums.out";
    private static final String SCRIPTS_JSON = "https://www.kaggle.com/scripts/d/";
    private static final String SCRIPTS_SRC = "https://www.kaggle.com/scripts/sourceurl/";

    public static void main(String[] args) {
        try {
            File f = new File(FORUMS_FILENAME);
            f.createNewFile();

            f = new File(DATASETS_FILENAME);
            f.createNewFile();

            //Do datasets!
            /**
             for (int i = 0; i < 100; i++) {
             List<Map<String, Object>> scripts;
             int page = 0;
             do {
             String scriptUrl = SCRIPTS_JSON + i + "/" + page;
             System.out.println("\n" + scriptUrl);
             URL url = new URL(scriptUrl);
             ObjectMapper mapper = new ObjectMapper();
             scripts = mapper.readValue(url, List.class);

             for (Map<String, Object> script : scripts) {
             handleScriptPage(script.get("scriptUrl") + "/code");

             }
             page += 15;
             } while (scripts.size() > 0);
             }
             **/

            for (int i = 0; i < 350000; i++) {
                Document scriptSrc = Jsoup.connect(SCRIPTS_SRC + i).timeout(0).get();
                String scriptUrl = scriptSrc.select("body").text();
                if (scriptUrl.length() > 0) {
                    handleScriptSource(scriptUrl);
                }
            }

            //Do forums!
            Document forumDoc = Jsoup.connect(KAGGLE_URL + "/forums").timeout(0).get();
            Elements forumList = forumDoc.select("#forumlist");
            Elements forumHeaders = forumList.select("h2");
            ListIterator<Element> hIter = forumHeaders.listIterator();
            while (hIter.hasNext()) {
                Element forumHeader = hIter.next();
                if (forumHeader.text().contains("Competition") || forumHeader.text().contains("Datasets")) {
                    Elements links = forumHeader.nextElementSibling().select("a:not([class])");
                    ListIterator<Element> linkIter = links.listIterator();
                    while (linkIter.hasNext()) {
                        Element link = linkIter.next();
                        handleCompetitionPage(link);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleCompetitionPage(Element link) {
        try {
            Document doc = Jsoup.connect(KAGGLE_URL + link.attr("href")).timeout(0).get();

            ListIterator<Element> topics = doc.select("h3").select("a").listIterator();
            while (topics.hasNext()) {
                Element topic = topics.next();
                if (topic.select(".fa-bar-chart").size() > 0) {
                    handleTopic(topic, true);
                } else {
                    handleTopic(topic, false);
                }
            }

            if (doc.select(".forum-pages").size() > 0) {
                Element nextPage = doc.select(".forum-pages").last().select("a").last();
                Elements disabled = doc.select(".forum-pages").last().select(".disabled");
                if (disabled.size() == 0 || !disabled.last().text().equals(">")) {
                    System.out.println(nextPage.outerHtml());

                    handleCompetitionPage(nextPage);
                } else {
                    System.out.println("No more pages!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleTopic(Element link, Boolean script) {
        try {
            Document doc = Jsoup.connect(KAGGLE_URL + link.attr("href") + "?limit=all").timeout(0).get();
            if (script) {
                String code = doc.select("code").text();
                if (code.length() > 0) {
                    try {
                        Files.write(Paths.get(FORUMS_FILENAME), code.getBytes(), StandardOpenOption.APPEND);
                        Files.write(Paths.get(FORUMS_FILENAME), SEPARATOR.getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                ListIterator<Element> postIter = doc.select(".postbox").listIterator();
                while (postIter.hasNext()) {
                    Element post = postIter.next();
                    ListIterator<Element> preIter = post.select("pre").listIterator();
                    while (preIter.hasNext()) {
                        Element pre = preIter.next();
                        if (pre.select("code").size() == 0) {
                            if (pre.text().length() > 0) {
                                try {
                                    Files.write(Paths.get(FORUMS_FILENAME), pre.text().getBytes(), StandardOpenOption.APPEND);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    String code = post.select("code").text();
                    if (code.length() > 0) {
                        try {
                            Files.write(Paths.get(FORUMS_FILENAME), code.getBytes(), StandardOpenOption.APPEND);
                            Files.write(Paths.get(FORUMS_FILENAME), SEPARATOR.getBytes(), StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleScriptPage(String link) {
        try {
            Document doc = Jsoup.connect(KAGGLE_URL + link).timeout(0).get();
            String code = doc.select("code").text();
            if (code.length() > 0) {
                try {
                    Files.write(Paths.get(DATASETS_FILENAME), code.getBytes(), StandardOpenOption.APPEND);
                    Files.write(Paths.get(DATASETS_FILENAME), SEPARATOR.getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleScriptSource(String link) {
        try {
            System.out.println("Trying script from: " + link);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = null;
            try {
                URL url = new URL(link);
                is = url.openStream();
                byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
                int n;

                while ((n = is.read(byteChunk)) > 0) {
                    baos.write(byteChunk, 0, n);
                }
                Files.write(Paths.get(DATASETS_FILENAME), baos.toByteArray(), StandardOpenOption.APPEND);
                Files.write(Paths.get(DATASETS_FILENAME), SEPARATOR.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
