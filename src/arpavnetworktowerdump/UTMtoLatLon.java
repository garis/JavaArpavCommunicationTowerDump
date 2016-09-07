package arpavnetworktowerdump;

//da Gauss Boaga a crica UTM Zone 32N a lat e lon
//http://www.geoin.it/coordinate_converter/
public class UTMtoLatLon {
    
    public UTMtoLatLon(){}

    static double X_CORRECTION=-999948.4;
    static double Y_CORRECTION=179.4;
    
    public static double[] toLatLon(double utmX, double utmY, String utmZone)
    {
        utmX=utmX+X_CORRECTION;
        utmY=utmY+Y_CORRECTION;
        
        boolean isNorthHemisphere = true;// utmZone.Last() >= 'N';

        double diflat = -0.00066286966871111111111111111111111111;
        double diflon = -0.0003868060578;

        //https://en.wikipedia.org/wiki/File:LA2-Europe-UTM-zones.png
        double zone = 32;//????????//int.Parse(utmZone.Remove(utmZone.Length - 1));
        double c_sa = 6378137.000000;
        double c_sb = 6356752.314245;
        double e2 = Math.pow((Math.pow(c_sa,2) - Math.pow(c_sb,2)),0.5)/c_sb;
        double e2cuadrada = Math.pow(e2,2);
        double c = Math.pow(c_sa,2) / c_sb;
        double x = utmX - 500000;
        double y = isNorthHemisphere ? utmY : utmY - 10000000;

        double s = ((zone * 6.0) - 183.0);
        double lat = y / (c_sa * 0.9996);
        double v = (c / Math.pow(1 + (e2cuadrada * Math.pow(Math.cos(lat), 2)), 0.5)) * 0.9996;
        double a = x / v;
        double a1 = Math.sin(2 * lat);
        double a2 = a1 * Math.pow((Math.cos(lat)), 2);
        double j2 = lat + (a1 / 2.0);
        double j4 = ((3 * j2) + a2) / 4.0;
        double j6 = ((5 * j4) + Math.pow(a2 * (Math.cos(lat)), 2)) / 3.0;
        double alfa = (3.0 / 4.0) * e2cuadrada;
        double beta = (5.0 / 3.0) * Math.pow(alfa, 2);
        double gama = (35.0 / 27.0) * Math.pow(alfa, 3);
        double bm = 0.9996 * c * (lat - alfa * j2 + beta * j4 - gama * j6);
        double b = (y - bm) / v;
        double epsi = ((e2cuadrada * Math.pow(a, 2)) / 2.0) * Math.pow((Math.cos(lat)), 2);
        double eps = a * (1 - (epsi / 3.0));
        double nab = (b * (1 - epsi)) + lat;
        double senoheps = (Math.exp(eps) - Math.exp(-eps)) / 2.0;
        double delt  = Math.atan(senoheps/(Math.cos(nab) ) );
        double tao = Math.atan(Math.cos(delt) * Math.tan(nab));

        double longitude = ((delt * (180.0 / Math.PI)) + s) + diflon;
        double latitude = ((lat + (1 + e2cuadrada * Math.pow(Math.cos(lat), 2) - (3.0 / 2.0) * e2cuadrada * Math.sin(lat) * Math.cos(lat) * (tao - lat)) * (tao - lat)) * (180.0 / Math.PI)) + diflat;
        
        double[] result=new double[]{longitude,latitude};
        return result;
        
    }
}
