/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpavnetworktowerdump;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author riccardo
 */
public class NewDumper {

    private final String USER_AGENT = "Mozilla/5.0";

    private String firstURLParam = "ArcXMLRequest=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22UTF-8%22+%3F%3E%3CARCXML+version%3D%221.1%22%3E%0D%0A%3CREQUEST%3E%0D%0A%3CGET_FEATURES+outputmode%3D%22xml%22+envelope%3D%22false%22+checkesc+%3D%22true%22+geometry%3D%22false%22+featurelimit%3D%2225%22%3E%0D%0A%3CLAYER+id%3D%220%22+%2F%3E%3CSPATIALQUERY+subfields%3D%22%23SHAPE%23+IDSITO+NOME+X_SITO+Y_SITO+Z_SITO+CODSITO+GESTORE+INDIRIZZO+COMUNE+PROVINCIA%22%3E%3CSPATIALFILTER+relation%3D%22area_intersection%22+%3E%3CENVELOPE+maxy+maxx+miny+minx+%2F%3E%3C%2FSPATIALFILTER%3E%3C%2FSPATIALQUERY%3E%3C%2FGET_FEATURES%3E%3C%2FREQUEST%3E%3C%2FARCXML%3E&";
    private String secondURLParam1 = "ArcXMLRequest=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22UTF-8%22+%3F%3E%3CARCXML+version%3D%221.1%22%3E%0D%0A%3CREQUEST%3E%0D%0A%3CGET_FEATURES+outputmode%3D%22xml%22+envelope%3D%22false%22+checkesc+%3D%22true%22+geometry%3D%22false%22+featurelimit%3D%2225%22+beginrecord%3D%22";
    private String secondURLParam2 = "%22%3E%0D%0A%3CLAYER+id%3D%220%22+%2F%3E%3CSPATIALQUERY+subfields%3D%22%23SHAPE%23+IDSITO+NOME+X_SITO+Y_SITO+Z_SITO+CODSITO+GESTORE+INDIRIZZO+COMUNE+PROVINCIA%22%3E%3CSPATIALFILTER+relation%3D%22area_intersection%22+%3E%3CENVELOPE+maxy+maxx+miny+minx+%2F%3E%3C%2FSPATIALFILTER%3E%3C%2FSPATIALQUERY%3E%3C%2FGET_FEATURES%3E%3C%2FREQUEST%3E%3C%2FARCXML%3E&";

    //maxy%3D%225541286%2C76875152%22+maxx
    //private double maxX = 1988669.0020340548, minX = 1463256.1432436276, maxY = 5541286.76875152, minY = 4257920.483346367;
    private double maxX = 1826761.68, minX = 1619977.64, maxY = 5184560.83, minY = 4968491.89;
    private double subDivision = 45;
    private double tollerance = 1 / 5;
    private double xStep = (maxX - minX) / subDivision;
    private double yStep = (maxY - minY) / subDivision;

    private int dumpedTowers = 0;
    private ArrayList<Integer> torri;

    public NewDumper() {

        int j = 0;
        //send first post

        String urlParameters;

        Charset charset = Charset.forName("UTF-8");
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(Paths.get("dump.csv"), charset);
        } catch (IOException ex) {
            Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            writer.write("IDSITO,NOME,COD SITO,INDIRIZZO,Longitudine,Latitudine,Altezza,GESTORE,PROVINCIA,COMUNE\n");
        } catch (IOException ex) {
            Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
        }
        String line;
        DataOutputStream wr = null;
        BufferedReader rd = null;
        int startLoop = 0;
        int endLoop = 10000000;

        int x = 0, y = 0;

        torri = new ArrayList();
        for (x = 0; x < subDivision; x++) {
            for (y = 0; y < subDivision; y++) {
                System.out.println("QUADRANTE " + x + " " + y);
                try {
                    for (int i = startLoop; i < endLoop; i++) {

                        if (i > 0) {
                            urlParameters = secondURLParam1 + i * 25 + 1 + secondURLParam2;
                        } else {
                            urlParameters = firstURLParam;
                        }
                        //maxy+maxx+miny+minx e un po' di tolleranza
                        urlParameters = urlParameters.replace("maxy", ("maxy%3D%22" + ("" + (minY + yStep * (((double) y) + 1)) + yStep * tollerance).replace(".", "%2C") + "%22"));
                        urlParameters = urlParameters.replace("maxx", ("maxx%3D%22" + ("" + (minX + xStep * (((double) x) + 1)) + xStep * tollerance).replace(".", "%2C") + "%22"));
                        urlParameters = urlParameters.replace("miny", ("miny%3D%22" + ("" + (minY + yStep * (((double) y)) - yStep * tollerance)).replace(".", "%2C") + "%22"));
                        urlParameters = urlParameters.replace("minx", ("minx%3D%22" + ("" + (minX + xStep * (((double) x)) - xStep * tollerance)).replace(".", "%2C") + "%22"));
                        // Send post request
                        String url = "http://map.arpa.veneto.it/servlet/com.esri.esrimap.Esrimap?ServiceName=etere_new&CustomService=Query&ClientVersion=4.0&Form=True&Encode=False";
                        URL obj = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        //add reuqest header
                        con.setRequestMethod("POST");
                        con.setRequestProperty("User-Agent", USER_AGENT);
                        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        con.setDoOutput(true);

                        wr = new DataOutputStream(con.getOutputStream());
                        wr.writeBytes(urlParameters);
                        wr.flush();

                        // Get the response
                        rd = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));

                        j = 0;
                        while ((line = rd.readLine()) != null) {
                            if (j == 2) {
                                if (line.contains("hasmore=\"false\"")) {
                                    i = endLoop;
                                    break;
                                }
                                writer.write(LineParser(line));
                            }
                            j++;
                        }
                        writer.flush();
                    }
                    rd.close();
                    wr.close();

                } catch (MalformedURLException ex) {
                    Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ProtocolException ex) {
                    Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        try {
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String LineParser(String lineToParse) {
        String line = "";
        lineToParse = lineToParse.replaceAll("&apos", "'");
        lineToParse = lineToParse.replaceAll(",", ".");
        lineToParse = lineToParse.replaceAll(";", " ");
        lineToParse = lineToParse.replaceAll("%", ",");
        String[] parts = lineToParse.split("<FEATURE>");
        parts[parts.length - 1] = parts[parts.length - 1].split("<FEATURECOUNT")[0];
        String[] temp1;
        double[] lon_lat;
        int idSito;
        for (int i = 1; i < parts.length; i++) {
            //IDSITO
            temp1 = parts[i].split("IDSITO=\"");
            temp1 = temp1[1].split("\"");

            //evita di inserire doppioni
            idSito = Integer.parseInt(temp1[0]);
            while (torri.size() < idSito + 1) {
                torri.add(0);
            }

            if (torri.get(idSito) == 0) {
                torri.set(idSito,1);
                lon_lat = UTMtoLatLon.toLatLon(Double.parseDouble(temp1[8]), Double.parseDouble(temp1[10]), "N");

                //Correzione temporanea
                lon_lat[0] = lon_lat[0] - 0.0007761;
                lon_lat[1] = lon_lat[1] - 0.0010879;

                line = line + temp1[0] + ","
                        + temp1[2] + ","
                        + temp1[4] + ","
                        + temp1[6] + ","
                        + lon_lat[0] + ","
                        + lon_lat[1] + ","
                        + temp1[12] + ","
                        + temp1[14] + ","
                        + temp1[16] + ","
                        + temp1[18] + "\n";
                dumpedTowers++;
                System.out.println("DUMPED " + dumpedTowers + " TOWERS.   LAST ID: " + temp1[0]);
            } else {
                System.out.println("DUMPED " + dumpedTowers + " TOWERS.   LAST ID: " + temp1[0] + " ALREADY PRESENT");
            }
        }
        return line;
    }
}
