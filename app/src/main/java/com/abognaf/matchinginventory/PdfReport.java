package com.abognaf.matchinginventory;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class PdfReport {
    private PdfReport(){}

    private static String status(double d, String fuel) {
        long x = Math.round(Math.abs(d));
        if (d > 0) return "يوجد زيادة في " + fuel + " " + x + " لتر";
        if (d < 0) return "لديك عجز في " + fuel + " " + x + " لتر";
        return fuel + " مطابق";
    }

    public static void writeAudit(Context ctx, OutputStream out, Cursor c, java.util.List<Tank> tanks, String stationName) throws IOException {
        if (!c.moveToFirst()) return;
        PdfDocument pdf = new PdfDocument();
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
        Canvas cv = page.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        p.setTextAlign(Paint.Align.RIGHT);

        int y=50;
        p.setTextSize(22); p.setFakeBoldText(true);
        cv.drawText(stationName == null || stationName.isEmpty() ? "مطابقة الجرد" : stationName, 555, y, p);
        y+=32;
        p.setTextSize(18); cv.drawText("تقرير مطابقة الجرد",555,y,p);
        y+=30; p.setFakeBoldText(false); p.setTextSize(13);
        String dt = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(c.getLong(c.getColumnIndexOrThrow("created_at"))));
        cv.drawText("التاريخ: " + dt,555,y,p); y+=32;

        p.setFakeBoldText(true);
        cv.drawText("الفرق",555,y,p); cv.drawText("في الحساب",450,y,p); cv.drawText("الفعلي",335,y,p); cv.drawText("التمتير",225,y,p); cv.drawText("الخزان",120,y,p);
        y+=12; cv.drawLine(40,y,555,y,p); y+=24; p.setFakeBoldText(false);
        for(int i=0;i<4;i++){
            int n=i+1;
            long h=Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_h")));
            long a=Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_actual")));
            long b=Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_book")));
            long d=Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_diff")));
            cv.drawText(String.valueOf(d),555,y,p); cv.drawText(String.valueOf(b),450,y,p); cv.drawText(String.valueOf(a),335,y,p); cv.drawText(String.valueOf(h),225,y,p); cv.drawText(tanks.get(i).name,120,y,p);
            y+=30;
        }

        y+=15; p.setFakeBoldText(true); p.setTextSize(15);
        double dd=c.getDouble(c.getColumnIndexOrThrow("diesel_diff"));
        double pd=c.getDouble(c.getColumnIndexOrThrow("petrol_diff"));
        cv.drawText(status(dd,"الديزل"),555,y,p); y+=28;
        cv.drawText(status(pd,"البترول"),555,y,p); y+=35;

        p.setFakeBoldText(false); p.setTextSize(13);
        String notes=c.getString(c.getColumnIndexOrThrow("notes"));
        if(notes!=null && !notes.isEmpty()) cv.drawText("ملاحظات: "+notes,555,y,p);

        pdf.finishPage(page);
        pdf.writeTo(out);
        pdf.close();
    }

    public static void writePeriod(OutputStream out, Cursor cur, String stationName, String title) throws IOException {
        PdfDocument pdf = new PdfDocument();
        int pageNo=1, y=60;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
        Canvas cv = page.getCanvas();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL)); p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(20); p.setFakeBoldText(true); cv.drawText(stationName,555,y,p); y+=32;
        p.setTextSize(16); cv.drawText(title,555,y,p); y+=35;
        p.setTextSize(12); p.setFakeBoldText(false);
        double sumDiesel=0,sumPetrol=0; int count=0;
        while(cur.moveToNext()){
            if(y>790){
                pdf.finishPage(page);
                pageNo++; y=60;
                page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
                cv=page.getCanvas();
            }
            long ts=cur.getLong(1); double dd=cur.getDouble(2), pd=cur.getDouble(3);
            sumDiesel+=dd; sumPetrol+=pd; count++;
            String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(ts));
            cv.drawText(dt+"   ديزل: "+Math.round(dd)+"   بترول: "+Math.round(pd),555,y,p);
            y+=24;
        }
        y+=20; p.setFakeBoldText(true); p.setTextSize(14);
        cv.drawText("عدد الجردات: "+count,555,y,p); y+=26;
        cv.drawText("صافي فرق الديزل: "+Math.round(sumDiesel)+" لتر",555,y,p); y+=26;
        cv.drawText("صافي فرق البترول: "+Math.round(sumPetrol)+" لتر",555,y,p);
        pdf.finishPage(page);
        pdf.writeTo(out); pdf.close();
    }
}