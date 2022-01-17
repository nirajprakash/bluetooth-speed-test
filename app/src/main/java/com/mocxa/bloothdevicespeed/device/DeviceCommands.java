package com.mocxa.bloothdevicespeed.device;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Niraj on 03-01-2022.
 */
public class DeviceCommands {

    public static String INITIAL_HEART_BEAT = "28,13,10";
    public static String HEART_BEAT = "28,32,32,104,13";
    public static String TRANSMISSION = "28,32,32,119,32,48,101,13,10";
    public static String END_ONGOING = "28,32,32,119,32,38,33,13";
    public static String IMPEDENCE_OP = "28,32,32,119,32,39,33,13";
    public static String STOP = "28,32,32,119,32,48,32,13,10";
    public static int channel_nos = 24;//16;//8 //24;


    public static String deciData1(){

        int NoOfChnls=channel_nos;
        int N=channel_nos;
        String nonacsii = "ÿ";         //1 byte
        String company = "BIOSEMI";     //2-8
        String patient_id = "Anonymous Patient"; // 80

        //current date:

        Date c = Calendar.getInstance().getTime();
//        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yy", Locale.getDefault());
        String formattedDate = df.format(c);
        Log.i("Mocxa", "Current Date: "+formattedDate);
        String patient_record = "Recording no."+ "1"; //80

        String strartDate = formattedDate; //8

        //Current time:

        SimpleDateFormat dft = new SimpleDateFormat("hh.mm.ss", Locale.getDefault());
        String formattedTime = dft.format(c);
        Log.i("Mocxa", "Current Time: "+formattedTime);

        String strartTime = formattedTime;//8
        //String no_of_bytes = "        "; //8
        String no_of_bytes = Integer.toString((N+1)*256)+"    "; //8
        String version = "24BIT"; //44
        String no_of_record = "0";//8
        String duration_of_records = "1";//8
        String no_of_channels =channel_nos + "  ";//4

        // String no_of_samples = "2048";//68*8

        StringBuilder sb2 = new StringBuilder();
        sb2.append(nonacsii).append(company).append(emptySpaces80(patient_id)).append(emptySpaces80(patient_record)).append(emptySpaces8(strartDate));
        sb2.append(emptySpaces8(strartTime)).append(no_of_bytes).append(emptySpaces44(version)).append(emptySpaces8(no_of_record)).append(emptySpaces8(duration_of_records)).append(no_of_channels);
        // LiveGraphActivity obj= new LiveGraphActivity();
        String data1 = sb2.toString();
        String hexData = stringToHexadecimal(data1);
        String deciData1Str= HexatoDecimal(hexData);
        int index=NoOfChnls+32;
        return "28,32,32,119,33,"+ index +"," + deciData1Str + "13";
    }

    public static String deciData2(){

        StringBuilder sb_da1 = new StringBuilder();
        sb_da1.append(channelLabel());
        String data2 = sb_da1.toString();
        String hexData2 = stringToHexadecimal(data2);
        String deciData2Str= HexatoDecimal(hexData2);
        //EDF Signals.Label (ParamBlock = 2, Index = No. of Rec. Chans(n))
        deciData2Str= "28,32,32,119,34,"+ channelIndex() +"," +deciData2Str+"13";

        return  deciData2Str;
    }

    public static String deciData3(){
        String transduser_type = "AgAgCl";//68*80
        int N=channel_nos;

        StringBuilder sb_da2 = new StringBuilder();
        sb_da2.append(emptySpacesNx80Transducer(transduser_type, N));
        String data3 = sb_da2.toString();
        String hexData3 = stringToHexadecimal(data3);
        String deciData3=HexatoDecimal(hexData3);

        deciData3= "28,32,32,119,35,"+ channelIndex() +"," +deciData3+"13";
        return deciData3;
    }

    public static String deciData4(){

        String physicalDimension_of_channel = "uV";//68*8
        int N=channel_nos;
        StringBuilder sb_da3 = new StringBuilder();
        sb_da3.append(repeat68(physicalDimension_of_channel, N));
        String data4 = sb_da3.toString();
        String hexData4= stringToHexadecimal(data4);
        String deciData4=HexatoDecimal(hexData4);
        deciData4= "28,32,32,119,36,"+ channelIndex() +"," +deciData4+"13";
        return deciData4;
    }

    public static String deciData5(){
        int N=channel_nos;
        String minimun_units = "-375000";//68*8
        StringBuilder sb_da4 = new StringBuilder();
        sb_da4.append(repeat68(minimun_units, N));
        String data5 = sb_da4.toString();
        String hexData5= stringToHexadecimal(data5);
        String deciData5=HexatoDecimal(hexData5);

        deciData5= "28,32,32,119,37,"+ channelIndex() +"," +deciData5+"13";
        return deciData5;
    }

    public static String deciData6(){
        int N=channel_nos;
        String maximum_units = "375000";//68*8
        StringBuilder sb_da5 = new StringBuilder();
        sb_da5.append(repeat68(maximum_units, N));
        String data6 = sb_da5.toString();
        String hexData6= stringToHexadecimal(data6);
        String deciData6=HexatoDecimal(hexData6);

        deciData6= "28,32,32,119,38,"+ channelIndex() +"," +deciData6+"13";

        return deciData6;
    }

    public static String deciData7(){
        String digital_minimum = "-8388608";//68*8
        int N=channel_nos;
        StringBuilder sb_da6 = new StringBuilder();
        sb_da6.append(repeat68(digital_minimum, N));
        String data7 = sb_da6.toString();
        String hexData7= stringToHexadecimal(data7);
        String deciData7=HexatoDecimal(hexData7);

        deciData7= "28,32,32,119,39,"+ channelIndex() +"," +deciData7+"13";
        return deciData7;
    }

    public static String deciData8(){
        StringBuilder sb_da7 = new StringBuilder();
        int N=channel_nos;
        String digital_maximum = "8388607";//68*8
        sb_da7.append(repeat68(digital_maximum, N));
        String data8 = sb_da7.toString();
        String hexData8= stringToHexadecimal(data8);
        String deciData8=HexatoDecimal(hexData8);

        deciData8= "28,32,32,119,40,"+ channelIndex() +"," +deciData8+"13";

        return deciData8;
    }

    public static String deciData9(){
        int N=channel_nos;
        String prefiltering = "HP:DC,LP:70Hz";//68*80
        StringBuilder sb_da8 = new StringBuilder();
        sb_da8.append(emptySpacesNx80Transducer(prefiltering, N));
        String data9 = sb_da8.toString();
        String hexData9= stringToHexadecimal(data9);
        String deciData9=HexatoDecimal(hexData9);

        deciData9= "28,32,32,119,41,"+ channelIndex() +"," +deciData9+"13";

        return deciData9;
    }


    public static String deciData10(){
        int N=channel_nos;
        String no_of_samples = "250";//68*8
        StringBuilder sb_da9 = new StringBuilder();
        sb_da9.append(repeat68(no_of_samples, N));
        String data10 = sb_da9.toString();
        String hexData10= stringToHexadecimal(data10);
        String deciData10=HexatoDecimal(hexData10);

        deciData10= "28,32,32,119,42,"+ channelIndex() +"," +deciData10+"13";

        return deciData10;
    }

    public static String deciData11(){
        int N=channel_nos;
        String reserved = "H/W CHAN=";  //68*32

        StringBuilder sb_da10 = new StringBuilder();
        sb_da10.append(repeat68of32bytes(reserved, N));
        String data11 = sb_da10.toString();
        String hexData11= stringToHexadecimal(data11);
        String deciData11=HexatoDecimal(hexData11);

        deciData11= "28,32,32,119,43,"+ channelIndex() +"," +deciData11+"13";

        return deciData11;
    }




        /* ****************************************************************************************
     *                                       channel Data
     */

    private static int channelIndex(){
        int index=channel_nos+32;
        return index;
    }

    private static String channelLabel(){
        String bit8_empty = "        ";
        String bit7_empty = "       ";
        int NoOfChnls=channel_nos;
        StringBuilder sb1 = new StringBuilder();
        int j=2;
        int t=2;
        int k=0;
        int l=6;
        int m=2;
        int n=2;
        int p=0;
        int q=0;
        for(int i=0; i<NoOfChnls+1;i++)

        {

            if(NoOfChnls==8) {
                if(i < 2)
                    sb1.append("Fp").append(i + 1).append("-Ref");

                if (i>1 && i< 4)
                    sb1.append("C").append(i + 1).append("-Ref");

                if (i>3 && i< 6)
                    sb1.append("O").append(++k).append("-Ref");
                if (i>5 && i< 8)
                    sb1.append("T").append(++t).append("-Ref");

                if (i < 9) {
                    sb1.append(bit8_empty);
                } else {
                    sb1.append(bit7_empty);
                }
            }

            if(NoOfChnls==16) {
                if(i < 2)
                    sb1.append("Fp").append(i + 1).append("-Ref");

                if (i>1 && i< 4)
                    sb1.append("F").append(i + 1).append("-Ref");

                if (i>3 && i< 6)
                    sb1.append("C").append(++n).append("-Ref");

                if (i>5 && i< 8)
                    sb1.append("P").append(++j).append("-Ref");


                if (i>7 && i< 10)
                    sb1.append("O").append(++k).append("-Ref");

                if (i>9 && i< 12)
                    sb1.append("F").append(++l).append("-Ref");

               /* if (i>8 && i< 11)
                    sb1.append("F").append(++l).append("-Ref");*/

                if (i>11 && i< 16)
                    sb1.append("T").append(++m).append("-Ref");

                if (i < 9) {
                    sb1.append(bit8_empty);
                } else {
                    sb1.append(bit7_empty);
                }
            }

            if(NoOfChnls==24) {

                if(i < 2)
                    sb1.append("Fp").append(i + 1).append("-Ref");

                if (i>1 && i< 4)
                    sb1.append("F").append(i + 1).append("-Ref");

                if (i>3 && i< 6)
                    sb1.append("C").append(++n).append("-Ref");

                if (i>5 && i< 8)
                    sb1.append("P").append(++j).append("-Ref");

                if (i>7 && i< 10)
                    sb1.append("O").append(++k).append("-Ref");

                if (i>9 && i< 12)
                    sb1.append("F").append(++l).append("-Ref");

               /* if (i>8 && i< 11)
                    sb1.append("F").append(++l).append("-Ref");*/

                if (i>11 && i< 16)
                    sb1.append("T").append(++m).append("-Ref");

                if (i>15 && i< 18)
                    sb1.append("A").append(++p).append("-Ref");

                if (i>17 && i< 19)
                    sb1.append("F").append("z").append("-Ref");
                if (i>18 && i< 20)
                    sb1.append("C").append("z").append("-Ref");
                if (i>19 && i< 21)
                    sb1.append("P").append("z").append("-Ref");
                if (i>20 && i< 22)
                    sb1.append("O").append("z").append("-Ref");
                if (i>21 && i< 24) {
                    sb1.append("PG").append(++q).append("-Ref");
                   /* if(i==23) {
                        sb1.append(bit8_empty);
                        sb1.append("SP1").append("-Ref");
                    }*/
                }



                if (i < 9) {
                    sb1.append(bit8_empty);
                } else {
                    sb1.append(bit7_empty);
                }

            }
        }
        String channel_Label = sb1.toString(); //N*16  68*16
        return  channel_Label;
    }

    /* *************************************************************************************
     *                                       other
     */


    private static String emptySpaces80(String s) {
        StringBuilder s1 = new StringBuilder(s);
        for (int i = 0; i < 80 - s.length(); i++) {
            s1.append(" ");
        }
        return s1.toString();
    }

    private static String emptySpaces8(String s) {
        StringBuilder s1 = new StringBuilder(s);
        for (int i = 0; i < 8 - s.length(); i++) {
            s1.append(" ");
        }
        return s1.toString();
    }


    private static String emptySpaces44(String s) {
        StringBuilder s1 = new StringBuilder(s);
        Integer len = s.length();
        for (int i = 0; i < 44 - s.length(); i++) {
            s1.append(" ");
        }
        return s1.toString();
    }


    private static String stringToHexadecimal(String strvalue) {
        StringBuilder sb = new StringBuilder();
        //Converting string to character array
        char ch[] = strvalue.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            sb.append("0x");
            String hexString = Integer.toHexString(ch[i]);
            sb.append(hexString);
        }
        String result = sb.toString();
//        System.out.println(result);
        return result;
    }

    private static String HexatoDecimal(String strvalue) {
        StringBuilder sb1 = new StringBuilder();
        //Converting string to character array
        //char ch[] = strvalue.toCharArray();

        String ch[] = strvalue.split("0x");
        for (int i = 1; i < ch.length; i++) {
            //sb.append("0x");
            int hexString1 = Integer.parseInt(ch[i],16);
            sb1.append(hexString1+",");
        }
        String result = sb1.toString();
//        System.out.println(result);
        return result;
    }

    private static String emptySpacesNx80(String s, Integer N) {
        StringBuilder s1 = new StringBuilder(s);
        Integer len = s.length();
        for (int i = 0; i < 80- s.length(); i++) {
            s1.append(" ");
        }
        return s1.toString();
    }

    private static String emptySpacesNx80Transducer(String s, Integer N) {
        StringBuilder s1 = new StringBuilder();
        for (int i = 0; i < N; i++) {
            s1.append(emptySpacesNx80(s, N));
        }
        return  s1.toString();

    }

    private static String emptySpacesNx8(String s, Integer N) {
        StringBuilder s1 = new StringBuilder(s);
        Integer len = s.length();
        for (int i = 0; i < 8- s.length(); i++) {
            s1.append(" ");
        }
        return s1.toString();
    }

    private static  String repeat68(String s, Integer N) {
        StringBuilder s1 = new StringBuilder();
        for (int i = 0; i < N; i++) {
            s1.append(emptySpacesNx8(s, N));
        }
        return  s1.toString();

    }

    private static String emptySpacesNx32(String s, Integer N) {
        StringBuilder s1 = new StringBuilder(s);
        Integer len = s.length();
        for (int i = 0; i < 32- s.length(); i++) {
            s1.append(" ");
        }
        return s1.toString();
    }

    private static String repeat68of32bytes(String s, Integer N) {
        StringBuilder s1 = new StringBuilder();
        int dt=24;
        for (int i = 0; i < N; i++) {
            if(i != (channel_nos) ) {
                s1.append(emptySpacesNx32(s + String.valueOf(dt + i), N));
            } else if(i == channel_nos ){
                s1.append(emptySpacesNx32(s + String.valueOf(65), N));
            }
        }
        //s1.append(emptySpacesNx32(s + String.valueOf(65), N));
        return  s1.toString();

    }


}
