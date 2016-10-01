package arpavnetworktowerdump;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArpavNetworkTowerDump {

    public static int TARGET_ID = 35000; //ID massimo a cui arrivare

    public static int THREAD_NUMBER = 1;
    public static int THREAD_SLEEP_MS = 500; //quanto riposa il thread prima di ricominciare
    public static int THREAD_SLEEP_ERROR = 50;//tempo di attesa dopo una ecezzione
    public static int FOUNDED = 0;
    public static int RANDOMIZE_TIME = 0;
    public static int RETRY = 2;
    public static boolean INCREMENTAL_WAIT = false;

    public static int MIN_ID = 0;
    public static int MAX_ID = 0;

    public static int TARGET_THREAD_SLEEP_MS = 500;

    public static int threadCompleted = 0;

    public static void main(String[] args) throws IOException {
        ArpavNetworkTowerDump a=new ArpavNetworkTowerDump();
        a.LaunchNewDumper();
        /*
        System.out.println("THR_SLEEP #THR FROM_ID TO_ID INCREMENTAL_WAIT");
        THREAD_SLEEP_MS = Integer.parseInt(args[0]);
        if (args.length >= 3) {
            THREAD_NUMBER = 1;
            System.out.println("# THREAD:       " + args[0]);
            THREAD_SLEEP_MS = Integer.parseInt(args[1]);
            if (THREAD_SLEEP_MS <= 0) {
                System.out.println("THREAD_SLEEP_MS <= 0 not allowed");
                return;
            }
            System.out.println("THREAD_SLEEP_MS:    " + THREAD_SLEEP_MS);
            TARGET_THREAD_SLEEP_MS = THREAD_SLEEP_MS;
            System.out.println("FROM:       " + args[2]);
            System.out.println("TO:         " + args[3]);
            INCREMENTAL_WAIT = Boolean.parseBoolean(args[4]);
            System.out.println("Incremental:         " + INCREMENTAL_WAIT);
            MIN_ID = Integer.parseInt(args[2]);
            MAX_ID = Integer.parseInt(args[3]);
            ArpavNetworkTowerDump thisClass = new ArpavNetworkTowerDump();
            thisClass.setAndStart();
        } else {
            ArpavNetworkTowerDump thisClass = new ArpavNetworkTowerDump();
            thisClass.setAndStart();
        }*/
    }

    public void LaunchNewDumper()
    {
        NewDumper nd=new NewDumper();
    }
    
    public class Writer {

        BufferedWriter writer = null;
        boolean writer_closed = false;
        Charset charset = Charset.forName("UTF-8");

        public Writer() {
            try {
                writer = Files.newBufferedWriter(Paths.get("towersFROM" + MIN_ID + "TO" + MAX_ID + ".csv"), charset);
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

        if (THREAD_NUMBER == 1) {
            vectorThread[0] = new DumperThread();
            vectorThread[0].setWriter(writerClass, MIN_ID, MAX_ID, 0);
            vectorThread[0].start();

        } else {
            int workForThread = TARGET_ID / THREAD_NUMBER;
            for (int i = 0; i < THREAD_NUMBER; i++) {
                vectorThread[i] = new DumperThread();
                vectorThread[i].setWriter(writerClass, workForThread * i, workForThread * i + workForThread, i);
                vectorThread[i].start();
            }
        }
    }

    public class DumperThread extends Thread {

        Writer writer = null;
        int start = 0, end = 0;
        int name = -1;

        public void run() {
            ArrayList<failedID> elements = new ArrayList<>();
            if (writer != null) {
                for (int i = start; i <= end; i++) {
                    if (initializeDump(writer, i) != 0) {
                        elements.add(new failedID(i, true));
                    }
                    try {
                        Thread.sleep((long) (THREAD_SLEEP_MS + Math.random() * RANDOMIZE_TIME));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                boolean loopAgain = true;
                while (loopAgain == true) {
                    loopAgain = false;
                    for (failedID idRedo : elements) {
                        if (idRedo.isFailed() == true) {
                            if (initializeDump(writer, idRedo.getID()) != 0) {
                                loopAgain = true;
                            } else {
                                idRedo.isFailed(false);
                            }
                        }
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

    public class failedID {

        boolean failed;
        int id;

        public failedID() {
        }

        public failedID(int number, boolean esito) {
            id = number;
            failed = esito;
        }

        public int getID() {
            return id;
        }

        public boolean isFailed() {
            return failed;
        }

        public void setID(int number) {
            id = number;
        }

        public void isFailed(boolean result) {
            failed = result;
        }
    }

    //ritorna 0 se successo, altro se c'e stato un errore
    public int initializeDump(Writer writer, int number) {

        String url = "http://map.arpa.veneto.it/contents/agenti_fisici/htm/scheda.jsp?id_sito=" + number;
        int numberTried = 0;
        String error = "";
        while (numberTried < RETRY) {
            try {
                getPage(writer, url, number);
                numberTried = RETRY + 1;       //successo, RETRY+1 evita il sucessivo if
                THREAD_SLEEP_MS = THREAD_SLEEP_MS / 2;
                if (THREAD_SLEEP_MS < TARGET_THREAD_SLEEP_MS && INCREMENTAL_WAIT == true) {
                    THREAD_SLEEP_MS = TARGET_THREAD_SLEEP_MS;
                }
                return 0;
            } catch (IOException ex) {
                numberTried++;
                error = ex.getLocalizedMessage();
                try {
                    Thread.sleep((long) (THREAD_SLEEP_ERROR + Math.random() * RANDOMIZE_TIME));
                } catch (InterruptedException ex1) {
                    System.out.println(number + " " + (RETRY + 1) + " error: " + ex1.getLocalizedMessage());
                }
            }
        }
        if (numberTried == RETRY) {
            System.out.println(number + " " + (RETRY + 1) + " error: " + error);
        }
        if (INCREMENTAL_WAIT == true) {
            THREAD_SLEEP_MS = THREAD_SLEEP_MS * 2;
        }
        return 2;
    }

    public void getPage(Writer writer, String pageAddress, int number) throws MalformedURLException, IOException {
        URL towerPage = new URL(pageAddress);
        BufferedReader inBuffer;
        inBuffer = new BufferedReader(
                new InputStreamReader(towerPage.openStream()));

        String page = "";
        String temp;
        int i = 0;
        while (i != 2 && (temp = inBuffer.readLine()) != null) {
            //scarta tutto cio che c'e prima di "Codice Sito:"
            if (temp.contains("Codice Sito:")) {
                i++;
            }
            if (i == 1) {
                page = page + temp;
            }
            //scarta tutto cio che c'e dopo di "Altezza centro elettrico dal suolo"
            if (temp.contains("Altezza centro elettrico dal suolo")) {
                i++;
            }
        }
        inBuffer.close();
        if (i == 2) {
            dumpInfo(writer, page, number);
        } else {
            System.out.println(number + "       not found");
        }
    }

    public void dumpInfo(Writer writer, String pageData, int number) {
        String[] temp1;
        String[] temp2;

        String line = ("" + number).concat("%");

        try {
            temp1 = pageData.split("<b>");

            //Codice Sito
            temp2 = temp1[1].split("</b>");
            temp2 = temp2[1].split("</h2>");
            line = line + temp2[0].concat("%");

            //Nome
            temp2 = temp1[2].split("</b>: ");
            temp2 = temp2[1].split("</p>");
            line = line + temp2[0].concat("%");

            //Gestore
            temp2 = temp1[3].split("</b>: ");
            temp2 = temp2[1].split("</p>");
            line = line + temp2[0].concat("%");

            //Indirizzo
            temp2 = temp1[4].split("</b>: ");
            temp2 = temp2[1].split("</p>");
            line = line + temp2[0].concat("%");

            //Coordinate
            double x = 0, y = 0;
            temp2 = temp1[5].split(": ");
            temp2 = temp2[1].split(" x; ");
            x = Double.parseDouble(temp2[0]);
            temp2 = temp2[1].split(" y");
            y = Double.parseDouble(temp2[0]);
            double[] lon_lat = UTMtoLatLon.toLatLon(x, y, "N");

            //Correzione temporanea
            lon_lat[0] = lon_lat[0] - 0.0007761;
            lon_lat[1] = lon_lat[1] - 0.0010879;

            line = line + lon_lat[0] + "%" + lon_lat[1] + "%";

            //Quota al suolo
            temp2 = temp1[6].split("</b>: ");
            temp2 = temp2[1].split("m s.l.m.");
            temp2[0]=temp2[0].replace(" ", "");
            line = line + "\"" + temp2[0].concat("\"%");

            //Postazione
            temp2 = temp1[7].split("</b>: ");
            temp2 = temp2[1].split("</p>");
            line = line + temp2[0].concat("%");

            //Altezza centro elettrico dal suolo
            temp2 = temp1[8].split(": ");
            temp2 = temp2[1].split("</p>");
            line = line + "\"" + temp2[0].concat("\"%");

            System.out.println(number + "       Dumped");

            try {
                writer.write(line);
            } catch (IOException ex) {
                Logger.getLogger(ArpavNetworkTowerDump.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception ex) {
            System.out.println(number + "       Errore");
        }
    }
}
