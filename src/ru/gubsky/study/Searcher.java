/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.gubsky.study;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryd6l1f
 */
public class Searcher
{
    private Connection conn_;
    private Statement stat_;

    public Searcher(Properties connectionProperties) throws SQLException
    {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Crawler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String host = connectionProperties.getProperty("server");
        String port = connectionProperties.getProperty("port");
        String login = connectionProperties.getProperty("user");
        String passw = connectionProperties.getProperty("pass");
        String db = connectionProperties.getProperty("db");
        
        Properties properties = new Properties();
        properties.setProperty("useUnicode", "true");
        properties.setProperty("characterEncoding", "utf8");

        String urlConnection = "jdbc:mysql://" + host + ":" + port + "/"
                + db + "?user=" + login + "&password=" + passw;
        conn_ = DriverManager.getConnection(urlConnection, properties);
        stat_ = conn_.createStatement();
        String setnames = "set names \'utf8\';";
        stat_.execute(setnames);
    }

    public void query(String q) throws SQLException
    {
        String[] words = Utils.separateWords(q);
        int[] urls = getMatchRows(words);
        HashMap sortedUrls = getSortedList(urls, words);

        // print
        System.out.println("===== result =====");
        Set keys;
        try {
            keys = sortedUrls.keySet();
        } catch (Exception e) {
            System.out.println("not found");
            return;
        }
        Iterator iterator = keys.iterator();

        while (iterator.hasNext()) {
            int key = (int) iterator.next();
            double score = (double) sortedUrls.get(key);
            System.out.println("Score: " + score + ";\tURL: " + getUrlName(key));
        }
    }

    private int[] getMatchRows(String[] words) throws SQLException
    {
        String queryString = getQueryString(words.length);
        PreparedStatement ps = getPreparedStatement(queryString, words);
//        System.out.println(ps);
        ResultSet rs = ps.executeQuery();

        int size = Utils.getSizeOfResultSet(rs);
//        System.out.println(size);
        int[] resUrls = new int[size];
        for (int i = 0; rs.next(); i++) {
            resUrls[i] = rs.getInt(1);
//            System.out.println(resUrls[i]);)
        }

        return resUrls;
    }

    private String getQueryString(int wordsCount)
    {
        if (wordsCount < 1) {
            return null;
        }
        String resultString = "SELECT distinct u.row_id FROM url_list u, ("
                + "SELECT w.url_id FROM word_location w";
        String whereString = " where w.word = ?";
        for (int i = 1; i < wordsCount; i++) {
            resultString += ", (SELECT * FROM word_location WHERE word = ?) AS t" + i;
            whereString += " AND w.url_id = t" + i + ".url_id";
        }
        resultString += whereString + ") AS e WHERE u.row_id = e.url_id;";
        return resultString;
    }

    private PreparedStatement getPreparedStatement(String queryString, String[] words) throws SQLException
    {
        PreparedStatement ps = conn_.prepareStatement(queryString);
        for (int i = 0; i < words.length; i++) {
            ps.setString(i + 1, words[i]);
        }
        return ps;
    }

    private HashMap getSortedList(int[] urls, String[] words) throws SQLException
    {
//        HashMap urlsWithScores = frequencyScore(urls, words);
        HashMap urlsWithScores = inBoundLinkScore(urls);
//        HashMap urlsWithScores = rankScore(urls);

        HashMap sortedUrls = Utils.sortByComparator(urlsWithScores);
        return sortedUrls;
    }

    private String getUrlName(int id) throws SQLException
    {
        String sqlSelect = "SELECT url FROM url_list WHERE row_id = ?";
        PreparedStatement preps = conn_.prepareStatement(sqlSelect);
        preps.setInt(1, id);
        ResultSet rs = preps.executeQuery();

        if (rs.next()) {
            return rs.getString(1);
        } else {
            return null;
        }
    }

    private HashMap normalizeScores(HashMap scores, boolean smallIsBetter)
    {
        HashMap resultHash = new HashMap(scores.size());
        Set keys = scores.keySet();
        Iterator iterator = keys.iterator();

        double max = -1;
        while (iterator.hasNext()) {
            Object key = iterator.next();
            double w;
            if (scores.get(key) instanceof Double) {
                w = (double) scores.get(key);
            } else {
                w = ((Integer) scores.get(key)).doubleValue();
            }
            if (max == -1) {
                max = w;
            }
            if (max < w) {
                max = w;
            }
        }
        iterator = keys.iterator();
        while (iterator.hasNext()) {
            Object key = iterator.next();
            double w;
            if (scores.get(key) instanceof Double) {
                w = (double) scores.get(key);
            } else {
                w = ((Integer) scores.get(key)).doubleValue();
            }
            if (smallIsBetter) {
                w = 1 - w / max;
            } else {
                w = w / max;
            }
            resultHash.put(key, w);
        }


        System.out.println(resultHash.keySet());
        System.out.println(resultHash.values());
        return resultHash;
    }

    private HashMap frequencyScore(int[] urls, String[] words) throws SQLException
    {
        if (urls.length < 1 || words.length < 1) {
            return null;
        }
        String query = "";

        //@todo: optimize query 
        for (int i = 0; i < urls.length; i++) {
            if (i == 0) {
                query += "select count(*) as count, w.url_id from word_location w where"
                        + " (w.word = ?";
                for (int j = 1; j < words.length; j++) {
                    query += " or w.word = ?";
                }
                query += ") and w.url_id = ?";
                continue;
            }
            query += " union all select count(*) as count, w.url_id from word_location w where"
                    + " (w.word = ?";
            for (int j = 1; j < words.length; j++) {
                query += " or w.word = ?";
            }
            query += ") and w.url_id = ?";
        }
        query += ";";

        PreparedStatement ps = conn_.prepareStatement(query);
        for (int i = 0; i < urls.length; i++) {
            for (int j = 0; j < words.length; j++) {
                ps.setString(i * (words.length + 1) + j + 1, words[j]);
            }
            System.out.println(((i + 1) * (words.length + 1)) + ": " + urls[i]);
            ps.setInt((i + 1) * (words.length + 1), urls[i]);
        }
        System.out.println(ps);

        ResultSet rs = ps.executeQuery();
        HashMap scores = new HashMap();
        while (rs.next()) {
            int count = rs.getInt("count");
            int urlId = rs.getInt("url_id");
            scores.put(urlId, count);
        }
        System.out.println(scores.values());
        System.out.println(scores.keySet());

        //вернуть нормализованный результат
        return normalizeScores(scores, false);
    }

    private HashMap rankScore(int[] urls) throws SQLException
    {
        if (urls.length < 1) {
            return null;
        }

        String query = "select * from pagerank where url_id = ?";
        for (int i = 1; i < urls.length; i++) {
            query += " or url_id = ?";
        }
        query += ";";
        PreparedStatement ps = conn_.prepareStatement(query);
        for (int i = 0; i < urls.length; i++) {
            ps.setInt(i + 1, urls[i]);
        }
        System.out.println(ps);
        ResultSet rs = ps.executeQuery();
        HashMap scores = new HashMap();
        while (rs.next()) {
            int urlId = rs.getInt("url_Id");
            double pr = rs.getDouble("pr");
            scores.put(urlId, pr);
        }

        return normalizeScores(scores, false);
    }

    private HashMap inBoundLinkScore(int[] urls) throws SQLException
    {
        if (urls.length < 1) {
            return null;
        }
        //пройтись по всем урлам и посчитать количество ссылок на урл
        String query = "SELECT count(*) as count, to_id from"
                + " link where to_id = ?";
        for (int i = 1; i < urls.length; i++) {
            query += " or to_id = ?";
        }
        query += " group by to_id order by count;";

        PreparedStatement ps = conn_.prepareStatement(query);
        for (int i = 0; i < urls.length; i++) {
            ps.setInt(i + 1, urls[i]);
        }
        System.out.println(ps);

        ResultSet rs = ps.executeQuery();
        HashMap scores = new HashMap();
        while (rs.next()) {
            int count = rs.getInt("count");
            int urlId = rs.getInt("to_id");
            scores.put(urlId, count);
        }
        System.out.println(scores.values());
        System.out.println(scores.keySet());

        //нормализовать ранги (чем больше, тем лучше)
        return normalizeScores(scores, false);
    }
}
