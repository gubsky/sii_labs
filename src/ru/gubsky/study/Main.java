/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.gubsky.study;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryd6l1f
 */
public class Main
{
    public static void main(String[] arg)
    {
        boolean needCrawl = false;
        

        if (needCrawl) {
            try {
//            Crawler crawler = new Crawler(null, 0, null, null, "db_mysql");
//            Crawler crawler = new Crawler("localhost", 3306, "study_user", "12345", "study_db");
                Crawler crawler = new Crawler("localhost", 3306, "root", "12345", "lab2_db");
                crawler.calculatePageRank();
//                String[] pages = new String[] {"http://nstu.ru", "http://ngs.ru"};
//
//                try {
//                    crawler.crawl(pages, 2);
//                } catch (MalformedURLException ex) {
//                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (IOException ex) {
//                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//                }
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {

            ///////////////
            // search
            ///////////////

//            String searchStr = "вышел блоги словари";
        String searchStr = "вышел";
            try {
                Searcher searcher = new Searcher("localhost", 3306, "root", "12345", "lab2_db");
                searcher.query(searchStr);
            } catch (SQLException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
