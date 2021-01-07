package com.example.wificonfigoverbt;

public class WifiNetwork {

    private String ssid;
//    private String password;
//    private String mac_address;

    public WifiNetwork(String SSID){
        this.ssid = SSID;
//        this.password = pw;
//        this.mac_address = "default mac";
    }

    public String getSsid(){
        return this.ssid;
    }
/*
    public String getPassword(){
        return this.password;
    }
    public java.lang.String getMac_address() {
        return this.mac_address;
    }
*/
    public String toString(){
        return this.ssid;
    }
}
