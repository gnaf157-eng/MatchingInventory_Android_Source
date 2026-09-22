package com.abognaf.matchinginventory;

import android.database.Cursor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

public final class XlsxWriter {
    private XlsxWriter(){}

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private static void put(ZipOutputStream z, String name, String txt) throws IOException {
        z.putNextEntry(new ZipEntry(name)); z.write(txt.getBytes(StandardCharsets.UTF_8)); z.closeEntry();
    }

    private static String c(String ref, String value) {
        return "<c r=\""+ref+"\" t=\"inlineStr\"><is><t>"+esc(value)+"</t></is></c>";
    }

    private static String n(String ref, double value) {
        return "<c r=\""+ref+"\"><v>"+value+"</v></c>";
    }

    public static void writeAudit(OutputStream out, Cursor cur, Cursor items, java.util.List<Tank> legacyTanks, String stationName) throws IOException {
        if (!cur.moveToFirst()) return;
        ZipOutputStream z = new ZipOutputStream(out);
        put(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
        put(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        put(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
        put(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"نتيجة الجرد\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");

        StringBuilder rows=new StringBuilder(); int r=1;
        rows.append("<row r=\""+r+"\">").append(c("A"+r,stationName)).append(c("B"+r,"مطابقة الجرد")).append("</row>"); r++;
        long auditId=cur.getLong(cur.getColumnIndexOrThrow("id"));
        long ts=cur.getLong(cur.getColumnIndexOrThrow("created_at"));
        String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(ts));
        String auditNo="JRD-"+new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date(ts))+"-"+String.format(Locale.US,"%03d",auditId);
        rows.append("<row r=\""+r+"\">").append(c("A"+r,"رقم الجرد")).append(c("B"+r,auditNo)).append("</row>"); r++;
        rows.append("<row r=\""+r+"\">").append(c("A"+r,"التاريخ")).append(c("B"+r,dt)).append("</row>"); r++;
        rows.append("<row r=\""+r+"\">").append(c("A"+r,"الخزان")).append(c("B"+r,"النوع")).append(c("C"+r,"التمتير سم")).append(c("D"+r,"الفعلي لتر")).append("</row>"); r++;

        if(items!=null && items.getCount()>0){
            while(items.moveToNext()){
                rows.append("<row r=\""+r+"\">").append(c("A"+r,items.getString(0))).append(c("B"+r,items.getString(1)))
                    .append(n("C"+r,items.getDouble(4))).append(n("D"+r,items.getDouble(5))).append("</row>"); r++;
            }
        }else{
            for(int i=0;i<4&&i<legacyTanks.size();i++){
                int nn=i+1;
                rows.append("<row r=\""+r+"\">").append(c("A"+r,legacyTanks.get(i).name)).append(c("B"+r,legacyTanks.get(i).fuelType))
                    .append(n("C"+r,cur.getDouble(cur.getColumnIndexOrThrow("t"+nn+"_h"))))
                    .append(n("D"+r,cur.getDouble(cur.getColumnIndexOrThrow("t"+nn+"_actual")))).append("</row>"); r++;
            }
        }

        rows.append("<row r=\""+r+"\">").append(c("A"+r,"إجمالي الديزل")).append(n("D"+r,cur.getDouble(cur.getColumnIndexOrThrow("diesel_actual")))).append(c("E"+r,"الرصيد في الحساب")).append(n("F"+r,cur.getDouble(cur.getColumnIndexOrThrow("diesel_book")))).append(c("G"+r,"الفرق")).append(n("H"+r,cur.getDouble(cur.getColumnIndexOrThrow("diesel_diff")))).append("</row>"); r++;
        rows.append("<row r=\""+r+"\">").append(c("A"+r,"إجمالي البترول")).append(n("D"+r,cur.getDouble(cur.getColumnIndexOrThrow("petrol_actual")))).append(c("E"+r,"الرصيد في الحساب")).append(n("F"+r,cur.getDouble(cur.getColumnIndexOrThrow("petrol_book")))).append(c("G"+r,"الفرق")).append(n("H"+r,cur.getDouble(cur.getColumnIndexOrThrow("petrol_diff")))).append("</row>"); r++;
        rows.append("<row r=\""+r+"\">").append(c("A"+r,"ملاحظات")).append(c("B"+r,cur.getString(cur.getColumnIndexOrThrow("notes")))).append("</row>");

        put(z,"xl/worksheets/sheet1.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"+rows+"</sheetData></worksheet>");
        z.finish(); z.flush();
    }
}