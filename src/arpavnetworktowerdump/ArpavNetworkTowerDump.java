package arpavnetworktowerdump;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArpavNetworkTowerDump {

    public static int TARGET_ID = 35000; //ID massimo a cui arrivare

    public static int THREAD_NUMBER = 1;
    public static int THREAD_SLEEP_MS = 150; //quanto riposa il thread prima di ricominciare
    public static int THREAD_SLEEP_ERROR = 500;//tempo di attesa dopo una ecezzione
    public static int FOUNDED = 0;
    public static int RANDOMIZE_TIME = 0;
    public static int RETRY = 10;

    public static int threadCompleted = 0;

    public static void main(String[] args) throws IOException {
        ArpavNetworkTowerDump thisClass = new ArpavNetworkTowerDump();
        thisClass.setAndStart();
    }

    public class Writer {

        BufferedWriter writer = null;
        boolean writer_closed = false;
        Charset charset = Charset.forName("UTF-8");

        public Writer() {
            try {
                writer = Files.newBufferedWriter(Paths.get("towers.csv"), charset);
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }
            try {
                writer.write("ID,Codice Sito,Nome,Gestore,Indirizzo,Longitudine,Latitudine,Quota al suolo,Postazione,Altezza antenna da suolo\n");
            } catch (IOException ex) {
                Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public synchronized void write(String str) throws IOException {
            if (!writer_closed) {
                str = str.replaceAll(",", " ");
                str = str.replaceAll(";", " ");
                str = str.replaceAll("%", ",");
                writer.write(str.concat("\n"));
                FOUNDED++;
                if (FOUNDED % 10 == 0) {
                    writer.flush();
                }
            } else {
                System.out.println("Writer closed");
            }
        }

        public synchronized void close() throws IOException {
            System.out.println("Closing writer");
            writer.flush();
            writer.close();
            writer_closed = true;
        }
    }

    public void setAndStart() {
        Writer writerClass = new Writer();

        DumperThread[] vectorThread = new DumperThread[THREAD_NUMBER];
        int workForThread = TARGET_ID / THREAD_NUMBER;
        for (int i = 0; i < THREAD_NUMBER; i++) {
            vectorThread[i] = new DumperThread();
            vectorThread[i].setWriter(writerClass, workForThread * i, workForThread * i + workForThread, i);
            vectorThread[i].start();
        }
    }

    public class DumperThread extends Thread {

        Writer writer = null;
        int start = 0, end = 0;
        int name = -1;

        public void run() {
            if (writer != null) {
                for (int i = start; i <= end; i++) {
                    try {
                        initializeDump(writer, i);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        Thread.sleep((long) (THREAD_SLEEP_MS + Math.random() * RANDOMIZE_TIME));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                threadCompleted++;
                try {
                    if (threadCompleted == THREAD_NUMBER) {
                        writer.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public void setWriter(Writer wr, int startPos, int endPos, int threadNumber) {
            writer = wr;
            start = startPos;
            end = endPos;
            name = threadNumber;
        }
    }

    public void initializeDump(Writer writer, int number) throws IOException, InterruptedException {

        String url = "http://map.arpa.veneto.it/contents/agenti_fisici/htm/scheda.jsp?id_sito=" + number;
        int numberTried = 0;
        String error = "";
        while (numberTried < RETRY) {
            try {
                getPage(writer, url, number);
                numberTried = RETRY + 1;       //successo, RETRY+1 evita il sucessivo if
            } catch (IOException ex) {
                numberTried++;
                error = ex.getLocalizedMessage();
                Thread.sleep((long) (THREAD_SLEEP_ERROR + Math.random() * RANDOMIZE_TIME));
            }
        }
        if (numberTried == RETRY) {
            System.out.println(number + " " + (RETRY + 1) + " error: " + error);
        }
    }

    public void getPage(Writer writer, String pageAddress, int number) throws MalformedURLException, IOException {
        URL towerPage = new URL(pageAddress);
        BufferedReader inBuffer = null;
        inBuffer = new BufferedReader(
                new InputStreamReader(towerPage.openStream()));

        String[] pageLine = new String[8];
        String temp;
        int i = -1;
        while (i < 8 && (temp = inBuffer.readLine()) != null) {
            //scarta tutto cio che c'e prima di "Codice Sito:"
            if (temp.contains("Codice Sito:")) {
                i++;
            }
            if (i >= 0 && i < pageLine.length) {
                pageLine[i] = temp;
                i++;
            }
            //scarta tutto cio che c'e dopo di "Altezza centro elettrico dal suolo"
            if (temp.contains("Altezza centro elettrico dal suolo")) {
                i++;
            }
        }
        inBuffer.close();
        if (i > 0) {
            dumpInfo(writer, pageLine, number);
        } else {
            System.out.println(number + "       not found");
        }
    }

    public void dumpInfo(Writer writer, String[] pageData, int number) {
        String[] temp1;

        String line = ("" + number).concat("%");

        if (pageData.length == 8) {
            for (int i = 0; i < pageData.length; i++) {
                //Codice Sito:
                if (i == 0) {
                    temp1 = pageData[i].split("</b>");
                    temp1 = temp1[1].split("</h2>");
                    line = line + temp1[0].concat("%");
                }
                //Nome
                if (i == 1) {
                    temp1 = pageData[i].split("</b>: ");
                    line = line + temp1[1].concat("%");
                }
                //Gestore, Indirizzo, Quota al suolo, Postazione
                if (i == 2 || i == 3 || i == 6) {
                    temp1 = pageData[i].split("</b>: ");
                    if (i == 3) {
                        temp1[1] = temp1[1].replaceAll("&quot;", "");
                    }
                    line = line + temp1[1].concat("%");
                }
                if (i == 5) {
                    temp1 = pageData[i].split(": ");
                    temp1 = temp1[1].split("</p>");
                    temp1 = temp1[0].split(" m s.l.m.");
                    line = line + temp1[0].concat("%");
                }
                //Coordinate
                if (i == 4) {
                    double x = 0, y = 0;
                    temp1 = pageData[i].split(": ");
                    temp1 = temp1[1].split(" x; ");
                    x = Double.parseDouble(temp1[0]);
                    temp1 = temp1[1].split(" y");
                    y = Double.parseDouble(temp1[0]);

                    double[] lat_lon = UTMtoLatLon.toLatLon(x, y, "N");
                    line = line + lat_lon[0] + "%" + lat_lon[1] + "%";

                }
                //Altezza
                if (i == 7) {
                    temp1 = pageData[i].split(": ");
                    temp1 = temp1[1].split("</p>");
                    line = line + temp1[0].concat("%");
                }
            }
            try {
                writer.write(line);
            } catch (IOException ex) {
                Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(number + "       dumped");
        } else {
            System.out.println(number + "       problem");
        }
    }
}
