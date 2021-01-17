import java.io.*;
import java.util.Set;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class HtmlCrawler extends WebCrawler {

    private final static Pattern EXCLUSIONS = Pattern.compile(".*(\\.(css|js|xml|gif|jpg|png|mp3|mp4|zip|gz|pdf))$");
    private static int counter = 0;

    private CrawlerStatistics stats;

    public HtmlCrawler(CrawlerStatistics stats) {
        this.stats = stats;
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String urlString = url.getURL().toLowerCase();
        return !EXCLUSIONS.matcher(urlString).matches()
                && urlString.startsWith("https://www.bbc.com/");
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        stats.incrementProcessedPageCount();

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String title = htmlParseData.getTitle();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();
            stats.incrementTotalLinksCount(links.size());

            FileWriter myWriter = null;
            BufferedWriter bw = null;
            PrintWriter pw = null;

            //print output to file
            try {
                myWriter = new FileWriter("file3.txt", true);
                bw = new BufferedWriter(myWriter);
                pw = new PrintWriter(bw);
                pw.println(title);
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.printf("Page with title '%s' %n", title);
            System.out.printf("    Text length: %d %n", text.length());
            System.out.printf("    HTML length: %d %n", html.length());
            System.out.printf("    %d outbound links %n", links.size());
        }
    }

    public void store(Page page) {
        if (page.getParseData() instanceof HtmlParseData) {
            try {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                String content = new String(page.getContentData(), page.getContentCharset());

                //System.out.println("<DOC> " + htmlParseData.getText());
                fileWrite(htmlParseData.getTitle());
                counter ++;
                /*insertKeyStatement.setString(1, htmlParseData.getHtml());
                insertKeyStatement.setString(2, htmlParseData.getText());
                insertKeyStatement.setString(3, page.getWebURL().getURL());
                insertKeyStatement.setTimestamp(4, new Timestamp(new java.util.Date().getTime()));
                insertKeyStatement.executeUpdate();*/
            } catch (Exception e) {
                logger.error("Exception while storing webpage for url'{}'", page.getWebURL().getURL(), e);
                throw new RuntimeException(e);
            }
        }
    }


    public void fileWrite(String content) {
        try {
            File myObj = new File("file3.xml");
                if (myObj.createNewFile()) {
                    System.out.println("File created: " + myObj.getName());
                    try {
                        FileWriter myWriter = new FileWriter("file2.xml", true);
                        myWriter.write("<DOC> <DOCNO> " +counter + " </DOCNO> " + content + " </DOC>\n");
                        myWriter.close();
                    } catch (IOException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("File already exists.");
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
        }
    }


}
