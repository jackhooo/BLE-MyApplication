package com.example.apple.myapplication;

public class Device {

    String encodedHex = "";
    String hexMessage1 = "";
    String hexMessage2 = "";
    String hexMessage3 = "";

    public void setEncodedHex(){

        encodedHex = hexMessage1 + hexMessage2 + hexMessage3;
    }

    public boolean checkIfAllMessageReceive(){
        if(hexMessage1.length()!=0 && hexMessage2.length()!=0 && hexMessage3.length()!=0){
            return true;
        }
        else {
            return false;
        }
    }

    public void cleanMessage(){
        hexMessage1 = "";
        hexMessage2 = "";
        hexMessage3 = "";
        encodedHex = "";
    }
}
