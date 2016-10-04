/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpavtower;

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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author riccardo
 */
public class NewDumper {

    private final String USER_AGENT = "Mozilla/5.0";

    private String secondURLParam1 = "ArcXMLRequest=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22UTF-8%22+%3F%3E%3CARCXML+version%3D%221.1%22%3E%0D%0A%3CREQUEST%3E%0D%0A%3CGET_FEATURES+outputmode%3D%22xml%22+envelope%3D%22false%22+checkesc+%3D%22true%22+geometry%3D%22false%22+featurelimit%3D%2225%22+beginrecord%3D%22";
    private String secondURLParam2 = "%22%3E%0D%0A%3CLAYER+id%3D%220%22+%2F%3E%3CSPATIALQUERY+subfields%3D%22%23SHAPE%23+IDSITO+NOME+X_SITO+Y_SITO+Z_SITO+CODSITO+GESTORE+INDIRIZZO+COMUNE+PROVINCIA%22%3E%3CSPATIALFILTER+relation%3D%22area_intersection%22+%3E%3CENVELOPE+maxy+maxx+miny+minx+%2F%3E%3C%2FSPATIALFILTER%3E%3C%2FSPATIALQUERY%3E%3C%2FGET_FEATURES%3E%3C%2FREQUEST%3E%3C%2FARCXML%3E&";

    private double maxX = 1826761.68, minX = 1619977.64, maxY = 5184560.83, minY = 4968491.89;
    private double subDivision = 15;
    private double tollerance = 0.2d;
    private double xStep = (maxX - minX) / subDivision;
    private double yStep = (maxY - minY) / subDivision;
    private long WAIT = 0;
    private String fileName = "dump.csv";

    private int dumpedTowers = 0;
    private ArrayList<Integer> torri;

    private void LoadFile() {

        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), charset)) {
            String line = null;
            reader.readLine();
            int idSito = -1;
            while ((line = reader.readLine()) != null) {
                idSito = Integer.parseInt(line.split(",")[0]);
                while (torri.size() < idSito + 1) {
                    torri.add(0);
                }
                torri.set(idSito, 1);

            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    public NewDumper() {

        for (int h = 2; h < 30; h++) {
            subDivision = h;
            xStep = (maxX - minX) / subDivision;
            yStep = (maxY - minY) / subDivision;

            int j = 0;

            String urlParameters;

            torri = new ArrayList();

            LoadFile();

            Charset charset = Charset.forName("UTF-8");
            BufferedWriter writer = null;

            boolean writeHeader = false;
            if (!Files.exists(Paths.get(fileName))) {
                writeHeader = true;
            }

            try {
                writer = Files.newBufferedWriter(Paths.get(fileName), charset, StandardOpenOption.APPEND);
            } catch (IOException ex) {
                Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                if (writeHeader) {
                    writer.write("IDSITO,NOME,COD SITO,INDIRIZZO,Longitudine,Latitudine,Altezza,GESTORE,PROVINCIA,COMUNE\n");
                }
            } catch (IOException ex) {
                Logger.getLogger(NewDumper.class.getName()).log(Level.SEVERE, null, ex);
            }
            String line;
            DataOutputStream wr = null;
            BufferedReader rd = null;
            int startLoop = 0;
            int endLoop = 10000000;
            double coord = 0;
            //double xmi, xma, ymi, yma;

            int x = -1, y = -1;
            int countX = 0, countY = 0;

            for (x = (int) (Math.random() * subDivision); countX < subDivision; countX++) {
                x++;
                countY = 0;
                for (y = (int) (Math.random() * subDivision); countY < subDivision; countY++) {
                    y++;
                    if (x >= subDivision) {
                        x = 0;
                    }
                    if (y >= subDivision) {
                        y = 0;
                    }

                    System.out.println("QUADRANTE " + x + " " + y+" SUBS: "+h);
                    try {
                        for (int i = startLoop; i < endLoop; i++) {
                            Thread.sleep(WAIT);
                            urlParameters = secondURLParam1 + i * 25 + 1 + secondURLParam2;

                            //maxy+maxx+miny+minx e un po' di tolleranza
                            coord = minY + yStep * ((double) (y + 1)) + yStep * tollerance;
                            // yma = coord;
                            urlParameters = urlParameters.replace("maxy", ("maxy%3D%22" + ("" + coord).replace(".", "%2C") + "%22"));
                            coord = minX + xStep * ((double) (x + 1)) + xStep * tollerance;
                            // xma = coord;
                            urlParameters = urlParameters.replace("maxx", ("maxx%3D%22" + ("" + coord).replace(".", "%2C") + "%22"));
                            coord = minY + yStep * ((double) y) - yStep * tollerance;
                            // ymi = coord;
                            urlParameters = urlParameters.replace("miny", ("miny%3D%22" + ("" + coord).replace(".", "%2C") + "%22"));
                            coord = minX + xStep * ((double) x) - xStep * tollerance;
                            // xmi = coord;
                            urlParameters = urlParameters.replace("minx", ("minx%3D%22" + ("" + coord).replace(".", "%2C") + "%22"));

                            /* lon_lat = UTMtoLatLon.toLatLon(xmi, ymi, "N");
                        writer.write("1," + lon_lat[0] + "," + lon_lat[1] + "\n");
                        lon_lat = UTMtoLatLon.toLatLon(xmi, yma, "N");
                        writer.write("2," + lon_lat[0] + "," + lon_lat[1] + "\n");
                        lon_lat = UTMtoLatLon.toLatLon(xma, yma, "N");
                        writer.write("3," + lon_lat[0] + "," + lon_lat[1] + "\n");
                        lon_lat = UTMtoLatLon.toLatLon(xma, ymi, "N");
                        writer.write("4," + lon_lat[0] + "," + lon_lat[1] + "\n");*/
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
                    } catch (InterruptedException ex) {
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
                torri.set(idSito, 1);
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
