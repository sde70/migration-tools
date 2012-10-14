package com.nuodb.tools.migration;


public class CliConstants {

    public final static String[] ARGUMENTS = new String[]{
            "dump",
            "--source.driver=com.mysql.jdbc.Driver",
            "--source.url=jdbc:mysql://localhost:3306/test",
            "--source.username=root",
            "--output.type=cvs",
            "--output.path=/tmp/"
    };
}