package com.abognaf.matchinginventory;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    DbHelper db;
    LinearLayout page, bottom;
    List<Tank> tanks;
    EditText[] h = new EditText[4];
    EditText[] book = new EditText[4];
    TextView[] actual = new TextView[4];
    TextView[] diff = new TextView[4];
    TextView summary;
    EditText notes;
    long exportAuditId = -1;
    boolean exportPdf = true;
    SharedPreferences prefs;
    static final int CREATE_DOC=501, PICK_LOGO=502, CREATE_PERIOD_PDF=503;
    long reportStart=0, reportEnd=Long.MAX_VALUE;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(11,111,164));
        db=new DbHelper(this); tanks=db.getTanks();
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        buildShell();
        showHome();
    }

    TextView title(String s){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(22); t.setTextColor(Color.rgb(20,40,55));
        t.setGravity(Gravity.RIGHT); t.setPadding(20,18,20,18); t.setTypeface(null,1); return t;
    }
    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setMinHeight(60); return b;
    }
    EditText input(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(18); e.setGravity(Gravity.RIGHT);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }
    void buildShell(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(16,10,16,10); page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView scroll=new ScrollView(this); scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.CENTER);
        String[] names={"الرئيسية","الجرد","السجل","التقارير","الإعدادات"};
        for(String n:names){
            Button b=btn(n); bottom.addView(b,new LinearLayout.LayoutParams(0,64,1));
            if(n.equals("الرئيسية")) b.setOnClickListener(v->showHome());
            else if(n.equals("الجرد")) b.setOnClickListener(v->showAudit());
            else if(n.equals("السجل")) b.setOnClickListener(v->showArchive());
            else if(n.equals("التقارير")) b.setOnClickListener(v->showReports());
            else b.setOnClickListener(v->showSettingsPage());
        }
        root.addView(bottom,new LinearLayout.LayoutParams(-1,64));
        setContentView(root);
    }
    void clearPage(){ page.removeAllViews(); }

    void showHome(){
        clearPage(); page.addView(title("مطابقة الجرد"));
        String station=prefs.getString("station_name","");
        if(!station.isEmpty()){ TextView s=title(station); s.setTextSize(18); page.addView(s); }
        Button a=btn("بدء جرد جديد"); a.setOnClickListener(v->showAudit()); page.addView(a);
        Button ar=btn("سجل الجرد"); ar.setOnClickListener(v->showArchive()); page.addView(ar);
        Button r=btn("التقارير"); r.setOnClickListener(v->showReports()); page.addView(r);
        Button st=btn("الإعدادات"); st.setOnClickListener(v->showSettingsPage()); page.addView(st);
    }

    void showAudit(){
        clearPage(); tanks=db.getTanks(); page.addView(title("جرد جديد"));
        TextView ds=title("الديزل"); ds.setTextSize(19); page.addView(ds);
        for(int i=0;i<4;i++){
            if(i==3){ TextView ps=title("البترول"); ps.setTextSize(19); page.addView(ps); }
            final int idx=i; Tank t=tanks.get(i);
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(16,12,16,18);
            GradientDrawableBg.apply(card, i==3 ? 0xFFEAF3FF : 0xFFF3FAF4, 18);
            TextView nm=title(t.name); nm.setTextSize(17); card.addView(nm);
            TextView dims=new TextView(this); dims.setGravity(Gravity.RIGHT); dims.setText("الطول "+fmt(t.lengthCm)+" سم  |  القطر "+fmt(t.diameterCm)+" سم");
            card.addView(dims);
            h[i]=input("التمتير بالسنتيمتر");
            book[i]=input("الرصيد في الحساب باللتر");
            actual[i]=new TextView(this); actual[i].setGravity(Gravity.RIGHT); actual[i].setTextSize(16); actual[i].setText("الكمية الفعلية: —");
            diff[i]=new TextView(this); diff[i].setGravity(Gravity.RIGHT); diff[i].setTextSize(17); diff[i].setText("الفرق: —");
            card.addView(h[i]); card.addView(book[i]); card.addView(actual[i]); card.addView(diff[i]);
            Button recalc=btn("إعادة حساب هذا الخزان");
            recalc.setOnClickListener(v->recalcOne(idx,true)); card.addView(recalc);
            android.text.TextWatcher w=new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int c,int a){}
                public void onTextChanged(CharSequence s,int st,int b,int c){ recalcOne(idx,false); updateSummary(); }
                public void afterTextChanged(android.text.Editable e){}
            };
            h[i].addTextChangedListener(w); book[i].addTextChangedListener(w);
            page.addView(card,new LinearLayout.LayoutParams(-1,-2));
        }
        summary=title("الملخص النهائي سيظهر هنا"); summary.setTextSize(18); page.addView(summary);
        notes=new EditText(this); notes.setHint("ملاحظات عامة (اختيارية)"); notes.setGravity(Gravity.RIGHT); notes.setMinLines(2); page.addView(notes);
        Button save=btn("حفظ الجرد"); save.setOnClickListener(v->saveAudit()); page.addView(save);
    }

    boolean recalcOne(int i, boolean warn){
        if(h[i]==null||book[i]==null) return false;
        String hs=h[i].getText().toString().trim(), bs=book[i].getText().toString().trim();
        if(hs.isEmpty()||bs.isEmpty()){ actual[i].setText("الكمية الفعلية: —"); diff[i].setText("الفرق: —"); diff[i].setTextColor(Color.DKGRAY); return false; }
        try{
            double hv=Double.parseDouble(hs), bv=Double.parseDouble(bs);
            Tank t=tanks.get(i);
            if(hv<0 || hv>t.diameterCm){
                if(warn) toast("التمتير يجب أن يكون بين 0 و "+fmt(t.diameterCm)+" سم");
                actual[i].setText("الكمية الفعلية: خطأ"); diff[i].setText("الفرق: —"); return false;
            }
            double av=TankMath.liters(t.lengthCm,t.diameterCm,hv);
            double d=av-bv;
            actual[i].setText("الكمية الفعلية: "+Math.round(av)+" لتر");
            diff[i].setText("الفرق: "+Math.round(d)+" لتر — "+statusText(d));
            diff[i].setTextColor(d>0?Color.rgb(20,100,210):d<0?Color.rgb(190,30,30):Color.rgb(20,130,60));
            return true;
        }catch(Exception e){ if(warn) toast("تحقق من الأرقام"); return false; }
    }

    void updateSummary(){
        if(summary==null || tanks==null) return;
        double da=0,dbk=0,pa=0,pbk=0;
        try{
            for(int i=0;i<4;i++){
                if(h[i]==null||book[i]==null||h[i].getText().toString().trim().isEmpty()||book[i].getText().toString().trim().isEmpty()) return;
                double hv=Double.parseDouble(h[i].getText().toString()), bv=Double.parseDouble(book[i].getText().toString());
                if(hv<0||hv>tanks.get(i).diameterCm) return;
                double av=TankMath.liters(tanks.get(i).lengthCm,tanks.get(i).diameterCm,hv);
                if(i<3){da+=av; dbk+=bv;} else {pa=av; pbk=bv;}
            }
            double dd=da-dbk,pd=pa-pbk;
            summary.setText("ملخص الجرد\nإجمالي الديزل الفعلي: "+Math.round(da)+" لتر\nفي الحساب: "+Math.round(dbk)+" لتر\n"+statusFuel(dd,"الديزل")+
                    "\n\nإجمالي البترول الفعلي: "+Math.round(pa)+" لتر\nفي الحساب: "+Math.round(pbk)+" لتر\n"+statusFuel(pd,"البترول"));
        }catch(Exception ignored){}
    }

    String statusText(double d){ return d>0?"زيادة":d<0?"عجز":"مطابق"; }
    String statusFuel(double d,String fuel){
        if(d>0) return "يوجد زيادة في "+fuel+" "+Math.round(Math.abs(d))+" لتر";
        if(d<0) return "لديك عجز في "+fuel+" "+Math.round(Math.abs(d))+" لتر";
        return fuel+" مطابق";
    }

    void saveAudit(){
        double[] hv=new double[4],av=new double[4],bv=new double[4];
        for(int i=0;i<4;i++){
            if(h[i].getText().toString().trim().isEmpty()||book[i].getText().toString().trim().isEmpty()){ toast("يجب إدخال التمتير والرصيد لكل الخزانات"); return; }
            try{
                hv[i]=Double.parseDouble(h[i].getText().toString()); bv[i]=Double.parseDouble(book[i].getText().toString());
            }catch(Exception e){ toast("تحقق من القيم المدخلة"); return; }
            if(hv[i]<0||hv[i]>tanks.get(i).diameterCm){ toast("تمتير "+tanks.get(i).name+" أكبر من ارتفاع الخزان"); return; }
            av[i]=TankMath.liters(tanks.get(i).lengthCm,tanks.get(i).diameterCm,hv[i]);
        }
        db.saveAudit(hv,av,bv,notes.getText().toString().trim());
        new AlertDialog.Builder(this).setTitle("تم الحفظ بنجاح")
            .setMessage("هل تريد بدء جرد جديد ومسح الحقول؟")
            .setPositiveButton("نعم",(d,w)->showAudit())
            .setNegativeButton("لا",null).show();
    }

    void showArchive(){
        clearPage(); page.addView(title("سجل الجرد"));
        EditText dateSearch=input("بحث بالتاريخ مثل 2026/09/22"); dateSearch.setInputType(android.text.InputType.TYPE_CLASS_TEXT); page.addView(dateSearch);
        Spinner fuel=new Spinner(this); fuel.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"الكل","ديزل","بترول"})); page.addView(fuel);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); page.addView(list);
        Runnable render=()->{
            list.removeAllViews(); Cursor c=db.listAudits();
            String q=dateSearch.getText().toString().trim();
            String f=(String)fuel.getSelectedItem();
            while(c.moveToNext()){
                long id=c.getLong(0),ts=c.getLong(1); double dd=c.getDouble(2),pd=c.getDouble(3);
                String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(ts));
                if(!q.isEmpty()&&!dt.contains(q)) continue;
                String line=dt+"\n";
                if(f.equals("الكل")||f.equals("ديزل")) line+="ديزل: "+Math.round(dd)+" لتر  ";
                if(f.equals("الكل")||f.equals("بترول")) line+="بترول: "+Math.round(pd)+" لتر";
                Button b=btn(line); b.setOnClickListener(v->showAuditRecord(id)); list.addView(b);
            }
            c.close();
        };
        render.run();
        dateSearch.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){render.run();}
            public void afterTextChanged(android.text.Editable e){}
        });
        fuel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> a,View v,int p,long id){render.run();}
            public void onNothingSelected(android.widget.AdapterView<?> a){}
        });
    }

    void showAuditRecord(long id){
        clearPage(); page.addView(title("نتيجة الجرد"));
        Cursor c=db.getAudit(id); if(!c.moveToFirst()){c.close();return;}
        String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(c.getLong(c.getColumnIndexOrThrow("created_at"))));
        TextView info=title(dt); info.setTextSize(16); page.addView(info);
        for(int i=0;i<4;i++){
            int n=i+1; TextView t=new TextView(this); t.setGravity(Gravity.RIGHT); t.setTextSize(17); t.setPadding(12,14,12,14);
            double d=c.getDouble(c.getColumnIndexOrThrow("t"+n+"_diff"));
            t.setText(tanks.get(i).name+"\nالتمتير: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_h")))+" سم | الفعلي: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_actual")))+" لتر | الحساب: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_book")))+" لتر | الفرق: "+Math.round(d)+" لتر");
            page.addView(t);
        }
        double dd=c.getDouble(c.getColumnIndexOrThrow("diesel_diff")),pd=c.getDouble(c.getColumnIndexOrThrow("petrol_diff"));
        TextView sum=title(statusFuel(dd,"الديزل")+"\n"+statusFuel(pd,"البترول")); sum.setTextSize(18); page.addView(sum);
        String nt=c.getString(c.getColumnIndexOrThrow("notes")); if(nt!=null&&!nt.isEmpty()){TextView n=title("ملاحظات: "+nt);n.setTextSize(15);page.addView(n);}
        c.close();
        Button pdf=btn("تصدير PDF"); pdf.setOnClickListener(v->requestAuditExport(id,true)); page.addView(pdf);
        Button xls=btn("تصدير Excel"); xls.setOnClickListener(v->requestAuditExport(id,false)); page.addView(xls);
    }

    void requestAuditExport(long id, boolean pdf){
        exportAuditId=id; exportPdf=pdf;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE,"مطابقة_الجرد_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date())+(pdf?".pdf":".xlsx"));
        startActivityForResult(i,CREATE_DOC);
    }

    void showReports(){
        clearPage(); page.addView(title("التقارير"));
        TextView info=title("التقرير يعرض سجل الجرد مع إجمالي الزيادة والعجز، ويمكن تصدير تقرير الفترة PDF."); info.setTextSize(15); page.addView(info);
        Button all=btn("عرض ملخص كل الفترة"); all.setOnClickListener(v->showReportSummary(0,Long.MAX_VALUE)); page.addView(all);
        Button last30=btn("آخر 30 يوم"); last30.setOnClickListener(v->showReportSummary(System.currentTimeMillis()-30L*24*60*60*1000,Long.MAX_VALUE)); page.addView(last30);
        Button export=btn("تصدير تقرير الفترة PDF"); export.setOnClickListener(v->{
            reportStart=0; reportEnd=Long.MAX_VALUE;
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/pdf"); i.putExtra(Intent.EXTRA_TITLE,"تقرير_فترة_مطابقة_الجرد.pdf"); startActivityForResult(i,CREATE_PERIOD_PDF);
        }); page.addView(export);
    }

    void showReportSummary(long start,long end){
        clearPage(); page.addView(title("ملخص التقارير"));
        Cursor c=db.listAuditsBetween(start,end); int count=0; double posD=0,negD=0,posP=0,negP=0;
        while(c.moveToNext()){count++;double d=c.getDouble(2),p=c.getDouble(3);if(d>=0)posD+=d;else negD+=d;if(p>=0)posP+=p;else negP+=p;}
        c.close();
        TextView x=title("عدد الجردات: "+count+
                "\n\nالديزل\nإجمالي الزيادة: "+Math.round(posD)+" لتر\nإجمالي العجز: "+Math.round(Math.abs(negD))+" لتر"+
                "\n\nالبترول\nإجمالي الزيادة: "+Math.round(posP)+" لتر\nإجمالي العجز: "+Math.round(Math.abs(negP))+" لتر");
        x.setTextSize(18); page.addView(x);
        Button back=btn("رجوع للتقارير"); back.setOnClickListener(v->showReports()); page.addView(back);
    }

    void showSettingsPage(){
        clearPage(); tanks=db.getTanks(); page.addView(title("الإعدادات"));
        EditText station=new EditText(this); station.setHint("اسم المحطة"); station.setGravity(Gravity.RIGHT); station.setText(prefs.getString("station_name","")); page.addView(station);
        Button logo=btn("اختيار شعار من معرض الهاتف"); logo.setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_LOGO);
        }); page.addView(logo);
        EditText[] names=new EditText[4],lens=new EditText[4],dias=new EditText[4];
        for(int i=0;i<4;i++){
            Tank t=tanks.get(i); TextView tt=title(t.fuelType+" — الخزان "+(i+1)); tt.setTextSize(16);page.addView(tt);
            names[i]=new EditText(this); names[i].setGravity(Gravity.RIGHT); names[i].setText(t.name); page.addView(names[i]);
            lens[i]=input("الطول سم"); lens[i].setText(fmt(t.lengthCm)); page.addView(lens[i]);
            dias[i]=input("القطر سم"); dias[i].setText(fmt(t.diameterCm)); page.addView(dias[i]);
        }
        Button save=btn("حفظ الإعدادات"); save.setOnClickListener(v->{
            try{
                prefs.edit().putString("station_name",station.getText().toString().trim()).apply();
                for(int i=0;i<4;i++){
                    Tank t=tanks.get(i); t.name=names[i].getText().toString().trim();
                    t.lengthCm=Double.parseDouble(lens[i].getText().toString()); t.diameterCm=Double.parseDouble(dias[i].getText().toString());
                    db.updateTank(t);
                }
                toast("تم حفظ الإعدادات"); tanks=db.getTanks();
            }catch(Exception e){toast("تحقق من الأبعاد");}
        }); page.addView(save);
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(req==PICK_LOGO){
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            prefs.edit().putString("logo_uri",uri.toString()).apply(); toast("تم حفظ الشعار");
            return;
        }
        try(OutputStream out=getContentResolver().openOutputStream(uri)){
            String station=prefs.getString("station_name","مطابقة الجرد");
            if(req==CREATE_DOC){
                Cursor c=db.getAudit(exportAuditId);
                if(exportPdf) PdfReport.writeAudit(this,out,c,db.getTanks(),station);
                else XlsxWriter.writeAudit(out,c,db.getTanks(),station);
                c.close(); toast("تم إنشاء الملف");
            }else if(req==CREATE_PERIOD_PDF){
                Cursor c=db.listAuditsBetween(reportStart,reportEnd);
                PdfReport.writePeriod(out,c,station,"تقرير فترة مطابقة الجرد");
                c.close(); toast("تم إنشاء تقرير الفترة");
            }
        }catch(Exception e){toast("فشل التصدير: "+e.getMessage());}
    }

    String fmt(double x){ return x==Math.rint(x)?String.valueOf((long)x):String.valueOf(x); }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    static class GradientDrawableBg {
        static void apply(View v,int color,float radius){
            android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
            g.setColor(color); g.setCornerRadius(radius); g.setStroke(1,0xFFD8E0E6); v.setBackground(g);
        }
    }
}