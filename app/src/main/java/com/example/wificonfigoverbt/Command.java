package com.example.wificonfigoverbt;

public class Command {
    private String command;
    private String ssid;
    private String password;

    public Command(String cmd){
        this.command = cmd;
        this.ssid = "not_defined";
        this.password = "not_defined";
    }
    public Command(String cmd, String ssid, String password){
        this.command = cmd;
        this.ssid = ssid;
        this.password = password;
    }

    public String getCommand() {
        return this.command;
    }
    public String getSsid() { return ssid; }
    public String getPassword() { return password; }
}
