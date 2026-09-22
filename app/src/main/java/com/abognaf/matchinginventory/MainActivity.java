package com.abognaf.matchinginventory;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int BLUE = Color.rgb(8,79,155);
    static final int BLUE2 = Color.rgb(18,119,205);
    static final int BG = Color.rgb(245,248,252);
    static final int INK = Color.rgb(18,45,74);
    static final int MUTED = Color.rgb(98,121,147);
    static final int LINE = Color.rgb(222,232,242);
    static final int WHITE = Color.WHITE;

    DbHelper db;
    LinearLayout page, bottom;
    List<Tank> tanks;
    EditText[] h = new EditText[4];
    EditText dieselBook, petrolBook;
    TextView[] actual = new TextView[4];
    TextView summary;
    EditText notes;
    long exportAuditId = -1;
    boolean exportPdf = true;
    SharedPreferences prefs;
    static final int CREATE_DOC=501, PICK_LOGO=502, CREATE_PERIOD_PDF=503;
    long reportStart=0, reportEnd=Long.MAX_VALUE;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BLUE);
        getWindow().setNavigationBarColor(Color.WHITE);
        db=new DbHelper(this); tanks=db.getTanks();
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        buildShell();
        showHome();
    }

    int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    TextView title(String s){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(22); t.setTextColor(INK);
        t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); t.setPadding(dp(16),dp(12),dp(16),dp(12));
        t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }

    TextView small(String s){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(13); t.setTextColor(MUTED);
        t.setGravity(Gravity.RIGHT); return t;
    }

    GradientDrawable shape(int color,float radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp((int)radius)); return g;
    }

    GradientDrawable strokeShape(int color,float radius,int strokeColor){
        GradientDrawable g=shape(color,radius); g.setStroke(dp(1),strokeColor); return g;
    }

    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(WHITE);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setGravity(Gravity.CENTER); b.setMinHeight(dp(54));
        b.setBackground(shape(BLUE2,16)); b.setPadding(dp(12),dp(8),dp(12),dp(8));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.setMargins(0,dp(6),0,dp(6)); b.setLayoutParams(lp);
        b.setStateListAnimator(null); return b;
    }

    Button ghostBtn(String s){
        Button b=btn(s); b.setTextColor(BLUE); b.setBackground(strokeShape(Color.WHITE,16,BLUE2)); return b;
    }

    EditText input(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(Color.rgb(145,160,177)); e.setTextColor(INK);
        e.setTextSize(17); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        e.setPadding(dp(16),dp(6),dp(16),dp(6)); e.setSingleLine(true);
        e.setBackground(strokeShape(Color.WHITE,14,LINE));
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54)); lp.setMargins(0,dp(5),0,dp(5)); e.setLayoutParams(lp);
        return e;
    }

    void buildShell(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); root.setBackgroundColor(BG);

        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16),dp(12),dp(16),dp(14)); page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG); scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(8),dp(6),dp(8),dp(6)); bottom.setBackgroundColor(Color.WHITE); bottom.setElevation(dp(10));
        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(70)));
        setContentView(root);
        renderBottom("الرئيسية");
    }

    void renderBottom(String active){
        bottom.removeAllViews();
        String[][] items={{"⌂","الرئيسية"},{"▥","التقارير"},{"↻","سجل الجرد"},{"⚙","الإعدادات"}};
        for(String[] it:items){
            LinearLayout tab=new LinearLayout(this); tab.setOrientation(LinearLayout.VERTICAL); tab.setGravity(Gravity.CENTER);
            boolean on=it[1].equals(active);
            if(on) tab.setBackground(shape(Color.rgb(232,243,255),18));
            TextView ic=new TextView(this); ic.setText(it[0]); ic.setTextSize(22); ic.setGravity(Gravity.CENTER);
            ic.setTextColor(on?BLUE2:MUTED);
            TextView tx=new TextView(this); tx.setText(it[1]); tx.setTextSize(11); tx.setGravity(Gravity.CENTER);
            tx.setTextColor(on?BLUE:MUTED); if(on) tx.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            tab.addView(ic,new LinearLayout.LayoutParams(-1,dp(30))); tab.addView(tx,new LinearLayout.LayoutParams(-1,dp(22)));
            bottom.addView(tab,new LinearLayout.LayoutParams(0,-1,1));
            if(it[1].equals("الرئيسية")) tab.setOnClickListener(v->showHome());
            else if(it[1].equals("التقارير")) tab.setOnClickListener(v->showReports());
            else if(it[1].equals("سجل الجرد")) tab.setOnClickListener(v->showArchive());
            else tab.setOnClickListener(v->showSettingsPage());
        }
    }

    void clearPage(){ page.removeAllViews(); page.setBackgroundColor(BG); }

    void addTopBar(String text){
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(6),dp(4),dp(6),dp(8));
        TextView t=title(text); t.setTextSize(24); bar.addView(t,new LinearLayout.LayoutParams(0,dp(60),1));
        TextView drop=new TextView(this); drop.setText("◉"); drop.setTextSize(25); drop.setTextColor(BLUE2); drop.setGravity(Gravity.CENTER);
        bar.addView(drop,new LinearLayout.LayoutParams(dp(48),dp(48))); page.addView(bar);
    }

    View actionCard(String icon,String name,String sub,View.OnClickListener click,boolean featured){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackground(strokeShape(featured?Color.rgb(235,246,255):Color.WHITE,20,featured?Color.rgb(190,222,248):LINE));
        card.setElevation(dp(2)); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(92)); cp.setMargins(0,dp(6),0,dp(6)); card.setLayoutParams(cp);

        TextView arrow=new TextView(this); arrow.setText("‹"); arrow.setTextSize(34); arrow.setTextColor(BLUE); arrow.setGravity(Gravity.CENTER);
        card.addView(arrow,new LinearLayout.LayoutParams(dp(44),-1));

        LinearLayout texts=new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); texts.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        TextView n=title(name); n.setTextSize(19); n.setPadding(0,0,0,0);
        TextView st=small(sub); st.setTextSize(13);
        texts.addView(n); texts.addView(st); card.addView(texts,new LinearLayout.LayoutParams(0,-1,1));

        TextView ico=new TextView(this); ico.setText(icon); ico.setTextSize(28); ico.setTextColor(Color.WHITE); ico.setGravity(Gravity.CENTER);
        ico.setBackground(shape(BLUE2,16)); card.addView(ico,new LinearLayout.LayoutParams(dp(62),dp(62)));
        card.setOnClickListener(click); return card;
    }

    LinearLayout panel(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(14),dp(12),dp(14),dp(14));
        l.setBackground(strokeShape(Color.WHITE,18,LINE)); l.setElevation(dp(2));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(6),0,dp(8)); l.setLayoutParams(lp); return l;
    }

    void showHome(){
        clearPage(); renderBottom("الرئيسية");

        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setGravity(Gravity.RIGHT);
        hero.setPadding(dp(22),dp(20),dp(22),dp(18));
        GradientDrawable hg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(4,65,139),Color.rgb(15,123,211)});
        hg.setCornerRadius(dp(28)); hero.setBackground(hg); hero.setElevation(dp(4));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(215)); hp.setMargins(0,0,0,dp(14)); hero.setLayoutParams(hp);

        TextView brand=new TextView(this); brand.setText("◉  مطابقة الجرد"); brand.setTextColor(Color.WHITE); brand.setTextSize(28);
        brand.setGravity(Gravity.RIGHT); brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD); hero.addView(brand);

        TextView sub=new TextView(this); sub.setText("إدارة جرد خزانات الوقود بدقة وسهولة"); sub.setTextColor(Color.rgb(218,236,255));
        sub.setTextSize(15); sub.setGravity(Gravity.RIGHT); sub.setPadding(0,dp(4),0,dp(12)); hero.addView(sub);

        String station=prefs.getString("station_name","محطة الأمير"); if(station.trim().isEmpty()) station="محطة الأمير";
        TextView stationPill=new TextView(this); stationPill.setText("⌖  "+station); stationPill.setTextSize(16); stationPill.setTextColor(BLUE);
        stationPill.setGravity(Gravity.CENTER); stationPill.setTypeface(Typeface.DEFAULT,Typeface.BOLD); stationPill.setBackground(shape(Color.rgb(229,242,255),24));
        hero.addView(stationPill,new LinearLayout.LayoutParams(-1,dp(46)));

        TextView slogan=new TextView(this); slogan.setText("دقة في الجرد .. ثقة في التشغيل"); slogan.setTextColor(Color.WHITE);
        slogan.setTextSize(14); slogan.setGravity(Gravity.RIGHT); slogan.setPadding(0,dp(14),0,0); hero.addView(slogan);
        page.addView(hero);

        page.addView(actionCard("＋","بدء جرد جديد","بدء عملية جرد جديدة للخزانات",v->showAudit(),true));
        page.addView(actionCard("↻","سجل الجرد","عرض جميع عمليات الجرد السابقة",v->showArchive(),false));
        page.addView(actionCard("▥","التقارير","مراجعة وتحليل نتائج الجرد",v->showReports(),false));
        page.addView(actionCard("⚙","الإعدادات","تخصيص التطبيق والخزانات",v->showSettingsPage(),false));

        LinearLayout info=panel(); info.setGravity(Gravity.CENTER_VERTICAL);
        TextView x=title("⛽  نحو إدارة أفضل للمخزون"); x.setTextSize(16); x.setPadding(0,0,0,0); info.addView(x);
        TextView y=small("دقة في البيانات .. كفاءة في التشغيل"); y.setPadding(0,dp(4),0,0); info.addView(y); page.addView(info);

        LinearLayout sig=new LinearLayout(this); sig.setOrientation(LinearLayout.VERTICAL); sig.setGravity(Gravity.CENTER);
        sig.setPadding(0,dp(8),0,dp(6));
        TextView s1=new TextView(this); s1.setText("△  أبو قناف للأتمتة"); s1.setTextSize(16); s1.setTextColor(BLUE);
        s1.setTypeface(Typeface.DEFAULT,Typeface.BOLD); s1.setGravity(Gravity.CENTER); sig.addView(s1);
        TextView s2=new TextView(this); s2.setText("معًا نصنع التشغيل الذكي"); s2.setTextSize(11); s2.setTextColor(MUTED); s2.setGravity(Gravity.CENTER); sig.addView(s2);
        page.addView(sig);
    }

    void showAudit(){
        clearPage(); renderBottom("الرئيسية"); tanks=db.getTanks(); addTopBar("بدء جرد جديد");
        LinearLayout steps=panel(); steps.setOrientation(LinearLayout.HORIZONTAL); steps.setGravity(Gravity.CENTER);
        String[] stepNames={"① المعلومات","② قراءات الخزانات","③ مراجعة"};
        for(String z:stepNames){ TextView tv=new TextView(this); tv.setText(z); tv.setGravity(Gravity.CENTER); tv.setTextSize(12); tv.setTextColor(z.startsWith("②")?BLUE:MUTED); if(z.startsWith("②")) tv.setTypeface(Typeface.DEFAULT,Typeface.BOLD); steps.addView(tv,new LinearLayout.LayoutParams(0,dp(42),1)); }
        page.addView(steps);
        TextView ds=title("الخزانات — الديزل"); ds.setTextSize(19); page.addView(ds);
        for(int i=0;i<4;i++){
            if(i==3){ TextView ps=title("الخزانات — البترول"); ps.setTextSize(19); page.addView(ps); }
            final int idx=i; Tank t=tanks.get(i);
            LinearLayout card=panel(); card.setPadding(dp(14),dp(10),dp(14),dp(12));
            TextView nm=title(t.name); nm.setTextSize(17); card.addView(nm);
            TextView dims=small("الطول "+fmt(t.lengthCm)+" سم  •  القطر "+fmt(t.diameterCm)+" سم");
            card.addView(dims);
            h[i]=input("التمتير بالسنتيمتر");
            actual[i]=new TextView(this); actual[i].setGravity(Gravity.RIGHT); actual[i].setTextSize(16); actual[i].setText("الكمية الفعلية: —");
            card.addView(h[i]); card.addView(actual[i]);
            android.text.TextWatcher w=new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int c,int a){}
                public void onTextChanged(CharSequence s,int st,int b,int c){ recalcOne(idx,false); updateSummary(); }
                public void afterTextChanged(android.text.Editable e){}
            };
            h[i].addTextChangedListener(w);
            page.addView(card,new LinearLayout.LayoutParams(-1,-2));
        }

        TextView balances=title("الأرصدة الإجمالية في الحساب"); balances.setTextSize(19); page.addView(balances);
        LinearLayout totals=panel();
        dieselBook=input("إجمالي رصيد الديزل في الحساب باللتر"); totals.addView(dieselBook);
        petrolBook=input("إجمالي رصيد البترول في الحساب باللتر"); totals.addView(petrolBook); page.addView(totals);
        android.text.TextWatcher totalWatcher=new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ updateSummary(); }
            public void afterTextChanged(android.text.Editable e){}
        };
        dieselBook.addTextChangedListener(totalWatcher);
        petrolBook.addTextChangedListener(totalWatcher);

        summary=title("الملخص النهائي سيظهر هنا"); summary.setTextSize(17); summary.setBackground(strokeShape(Color.rgb(238,247,255),18,Color.rgb(190,222,248))); page.addView(summary);
        notes=new EditText(this); notes.setHint("ملاحظات عامة (اختيارية)"); notes.setGravity(Gravity.RIGHT); notes.setTextColor(INK); notes.setHintTextColor(Color.rgb(145,160,177)); notes.setMinLines(2); notes.setPadding(dp(14),dp(12),dp(14),dp(12)); notes.setBackground(strokeShape(Color.WHITE,16,LINE)); page.addView(notes);
        Button save=btn("حفظ الجرد"); save.setOnClickListener(v->saveAudit()); page.addView(save);
    }

    boolean recalcOne(int i, boolean warn){
        if(h[i]==null) return false;
        String hs=h[i].getText().toString().trim();
        if(hs.isEmpty()){ actual[i].setText("الكمية الفعلية: —"); return false; }
        try{
            double hv=Double.parseDouble(hs);
            Tank t=tanks.get(i);
            if(hv<0 || hv>t.diameterCm){
                if(warn) toast("التمتير يجب أن يكون بين 0 و "+fmt(t.diameterCm)+" سم");
                actual[i].setText("الكمية الفعلية: خطأ"); return false;
            }
            double av=TankMath.liters(t.lengthCm,t.diameterCm,hv);
            actual[i].setText("الكمية الفعلية: "+Math.round(av)+" لتر");
            return true;
        }catch(Exception e){ if(warn) toast("تحقق من الأرقام"); return false; }
    }

    void updateSummary(){
        if(summary==null || tanks==null || dieselBook==null || petrolBook==null) return;
        double da=0,pa=0;
        try{
            for(int i=0;i<4;i++){
                if(h[i]==null||h[i].getText().toString().trim().isEmpty()) return;
                double hv=Double.parseDouble(h[i].getText().toString());
                if(hv<0||hv>tanks.get(i).diameterCm) return;
                double av=TankMath.liters(tanks.get(i).lengthCm,tanks.get(i).diameterCm,hv);
                if(i<3) da+=av; else pa=av;
            }
            if(dieselBook.getText().toString().trim().isEmpty()||petrolBook.getText().toString().trim().isEmpty()) return;
            double dbk=Double.parseDouble(dieselBook.getText().toString());
            double pbk=Double.parseDouble(petrolBook.getText().toString());
            double dd=da-dbk,pd=pa-pbk;
            summary.setText("ملخص الجرد\nإجمالي الديزل الفعلي: "+Math.round(da)+" لتر\nإجمالي الديزل في الحساب: "+Math.round(dbk)+" لتر\n"+statusFuel(dd,"الديزل")+
                    "\n\nإجمالي البترول الفعلي: "+Math.round(pa)+" لتر\nإجمالي البترول في الحساب: "+Math.round(pbk)+" لتر\n"+statusFuel(pd,"البترول"));
        }catch(Exception ignored){}
    }

    String statusText(double d){ return d>0?"زيادة":d<0?"عجز":"مطابق"; }
    String statusFuel(double d,String fuel){
        if(d>0) return "يوجد زيادة في "+fuel+" "+Math.round(Math.abs(d))+" لتر";
        if(d<0) return "لديك عجز في "+fuel+" "+Math.round(Math.abs(d))+" لتر";
        return fuel+" مطابق";
    }

    void saveAudit(){
        double[] hv=new double[4],av=new double[4];
        for(int i=0;i<4;i++){
            if(h[i].getText().toString().trim().isEmpty()){ toast("يجب إدخال التمتير لكل الخزانات"); return; }
            try{
                hv[i]=Double.parseDouble(h[i].getText().toString());
            }catch(Exception e){ toast("تحقق من قيم التمتير"); return; }
            if(hv[i]<0||hv[i]>tanks.get(i).diameterCm){ toast("تمتير "+tanks.get(i).name+" أكبر من ارتفاع الخزان"); return; }
            av[i]=TankMath.liters(tanks.get(i).lengthCm,tanks.get(i).diameterCm,hv[i]);
        }
        if(dieselBook.getText().toString().trim().isEmpty()||petrolBook.getText().toString().trim().isEmpty()){
            toast("يجب إدخال إجمالي رصيد الديزل وإجمالي رصيد البترول"); return;
        }
        double dieselAccount, petrolAccount;
        try{
            dieselAccount=Double.parseDouble(dieselBook.getText().toString());
            petrolAccount=Double.parseDouble(petrolBook.getText().toString());
        }catch(Exception e){ toast("تحقق من الأرصدة الإجمالية"); return; }
        db.saveAudit(hv,av,dieselAccount,petrolAccount,notes.getText().toString().trim());
        new AlertDialog.Builder(this).setTitle("تم الحفظ بنجاح")
            .setMessage("هل تريد بدء جرد جديد ومسح الحقول؟")
            .setPositiveButton("نعم",(d,w)->showAudit())
            .setNegativeButton("لا",null).show();
    }

    void showArchive(){
        clearPage(); renderBottom("سجل الجرد"); addTopBar("سجل الجرد");
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
                Button b=ghostBtn(line); b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); b.setTextColor(INK); b.setOnClickListener(v->showAuditRecord(id)); list.addView(b);
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
        clearPage(); renderBottom("سجل الجرد"); addTopBar("نتيجة الجرد");
        Cursor c=db.getAudit(id); if(!c.moveToFirst()){c.close();return;}
        String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(c.getLong(c.getColumnIndexOrThrow("created_at"))));
        TextView info=title(dt); info.setTextSize(16); page.addView(info);
        for(int i=0;i<4;i++){
            int n=i+1; TextView t=new TextView(this); t.setGravity(Gravity.RIGHT); t.setTextSize(16); t.setTextColor(INK); t.setPadding(dp(14),dp(14),dp(14),dp(14)); t.setBackground(strokeShape(Color.WHITE,16,LINE));
            t.setText(tanks.get(i).name+"\nالتمتير: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_h")))+" سم | الكمية الفعلية: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_actual")))+" لتر");
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
        clearPage(); renderBottom("التقارير"); addTopBar("التقارير");
        LinearLayout intro=panel(); TextView info=title("تحليل نتائج الجرد"); info.setTextSize(18); info.setPadding(0,0,0,0); intro.addView(info); TextView ii=small("ملخصات الزيادة والعجز وتقارير PDF للفترة"); ii.setPadding(0,dp(4),0,0); intro.addView(ii); page.addView(intro);
        Button all=btn("عرض ملخص كل الفترة"); all.setOnClickListener(v->showReportSummary(0,Long.MAX_VALUE)); page.addView(all);
        Button last30=btn("آخر 30 يوم"); last30.setOnClickListener(v->showReportSummary(System.currentTimeMillis()-30L*24*60*60*1000,Long.MAX_VALUE)); page.addView(last30);
        Button export=btn("تصدير تقرير الفترة PDF"); export.setOnClickListener(v->{
            reportStart=0; reportEnd=Long.MAX_VALUE;
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/pdf"); i.putExtra(Intent.EXTRA_TITLE,"تقرير_فترة_مطابقة_الجرد.pdf"); startActivityForResult(i,CREATE_PERIOD_PDF);
        }); page.addView(export);
    }

    void showReportSummary(long start,long end){
        clearPage(); renderBottom("التقارير"); addTopBar("ملخص التقارير");
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
        clearPage(); renderBottom("الإعدادات"); tanks=db.getTanks(); addTopBar("الإعدادات");
        TextView brandInfo=small("مطابقة الجرد  •  بواسطة أبو قناف للأتمتة"); brandInfo.setGravity(Gravity.CENTER); brandInfo.setPadding(0,0,0,dp(8)); page.addView(brandInfo);
        EditText station=input("اسم المحطة"); station.setInputType(android.text.InputType.TYPE_CLASS_TEXT); station.setText(prefs.getString("station_name","")); page.addView(station);
        Button logo=ghostBtn("اختيار شعار من معرض الهاتف"); logo.setOnClickListener(v->{
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