package com.abognaf.matchinginventory;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
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
    List<Tank> tanks=new ArrayList<>();
    List<EditText> dipInputs=new ArrayList<>();
    List<TextView> actualViews=new ArrayList<>();
    EditText dieselBook, petrolBook, notes;
    TextView summary;
    long exportAuditId=-1;
    boolean exportPdf=true;
    SharedPreferences prefs;
    static final int CREATE_DOC=501, PICK_LOGO=502, CREATE_PERIOD_PDF=503;
    long reportStart=0, reportEnd=Long.MAX_VALUE;
    boolean savingAudit=false;
    String currentAuditUid=null;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BLUE);
        getWindow().setNavigationBarColor(Color.WHITE);
        db=new DbHelper(this);
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
            TextView ic=new TextView(this); ic.setText(it[0]); ic.setTextSize(22); ic.setGravity(Gravity.CENTER); ic.setTextColor(on?BLUE2:MUTED);
            TextView tx=new TextView(this); tx.setText(it[1]); tx.setTextSize(11); tx.setGravity(Gravity.CENTER); tx.setTextColor(on?BLUE:MUTED);
            if(on) tx.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
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

    LinearLayout panel(){
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(14),dp(12),dp(14),dp(14));
        l.setBackground(strokeShape(Color.WHITE,18,LINE)); l.setElevation(dp(2));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(6),0,dp(8)); l.setLayoutParams(lp); return l;
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

        LinearLayout info=panel();
        TextView x=title("⛽  نحو إدارة أفضل للمخزون"); x.setTextSize(16); x.setPadding(0,0,0,0); info.addView(x);
        TextView y=small("دقة في البيانات .. كفاءة في التشغيل"); y.setPadding(0,dp(4),0,0); info.addView(y); page.addView(info);
        TextView s1=new TextView(this); s1.setText("△  أبو قناف للأتمتة\nمعًا نصنع التشغيل الذكي"); s1.setTextSize(14); s1.setTextColor(BLUE);
        s1.setTypeface(Typeface.DEFAULT,Typeface.BOLD); s1.setGravity(Gravity.CENTER); s1.setPadding(0,dp(8),0,dp(6)); page.addView(s1);
    }

    void showAudit(){
        currentAuditUid=UUID.randomUUID().toString();
        savingAudit=false;
        tanks=db.getTanks();
        dipInputs.clear(); actualViews.clear();
        clearPage(); renderBottom("الرئيسية"); addTopBar("بدء جرد جديد");

        if(tanks.isEmpty()){
            TextView no=title("لا توجد خزانات. أضف خزانًا من الإعدادات."); no.setTextSize(17); page.addView(no); return;
        }

        addFuelSection("ديزل");
        addFuelSection("بترول");

        TextView balances=title("الأرصدة الإجمالية في الحساب"); balances.setTextSize(19); page.addView(balances);
        LinearLayout totals=panel();
        dieselBook=input("إجمالي رصيد الديزل في الحساب باللتر"); totals.addView(dieselBook);
        petrolBook=input("إجمالي رصيد البترول في الحساب باللتر"); totals.addView(petrolBook); page.addView(totals);
        android.text.TextWatcher tw=new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ updateSummary(); }
            public void afterTextChanged(android.text.Editable e){}
        };
        dieselBook.addTextChangedListener(tw); petrolBook.addTextChangedListener(tw);

        summary=title("الملخص النهائي سيظهر هنا"); summary.setTextSize(17);
        summary.setBackground(strokeShape(Color.rgb(238,247,255),18,Color.rgb(190,222,248))); page.addView(summary);
        notes=new EditText(this); notes.setHint("ملاحظات عامة (اختيارية)"); notes.setGravity(Gravity.RIGHT);
        notes.setTextColor(INK); notes.setHintTextColor(Color.rgb(145,160,177)); notes.setMinLines(2);
        notes.setPadding(dp(14),dp(12),dp(14),dp(12)); notes.setBackground(strokeShape(Color.WHITE,16,LINE)); page.addView(notes);
        Button save=btn("حفظ الجرد"); save.setOnClickListener(v->saveAudit()); page.addView(save);
    }

    void addFuelSection(String fuel){
        ArrayList<Integer> idxs=new ArrayList<>();
        for(int i=0;i<tanks.size();i++) if(fuel.equals(tanks.get(i).fuelType)) idxs.add(i);
        if(idxs.isEmpty()) return;
        TextView sec=title("الخزانات — "+fuel); sec.setTextSize(19); page.addView(sec);
        for(int index:idxs){
            Tank t=tanks.get(index);
            LinearLayout card=panel(); card.setPadding(dp(14),dp(10),dp(14),dp(12));
            TextView nm=title(t.name); nm.setTextSize(17); card.addView(nm);
            TextView dims=small("الطول "+fmt(t.lengthCm)+" سم  •  القطر "+fmt(t.diameterCm)+" سم"); card.addView(dims);
            EditText dip=input("التمتير بالسنتيمتر");
            TextView av=new TextView(this); av.setGravity(Gravity.RIGHT); av.setTextColor(INK); av.setTextSize(16); av.setText("الكمية الفعلية: —");
            dipInputs.add(dip); actualViews.add(av);
            final int visualIndex=dipInputs.size()-1;
            final int tankIndex=index;
            dip.addTextChangedListener(new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int c,int a){}
                public void onTextChanged(CharSequence s,int st,int b,int c){ recalcOne(visualIndex,tankIndex,false); updateSummary(); }
                public void afterTextChanged(android.text.Editable e){}
            });
            card.setTag(index);
            card.addView(dip); card.addView(av); page.addView(card);
        }
    }

    int visualIndexForTank(int tankIndex){
        int v=0;
        for(int i=0;i<tanks.size();i++){
            if("ديزل".equals(tanks.get(i).fuelType)){
                if(i==tankIndex) return v;
                v++;
            }
        }
        for(int i=0;i<tanks.size();i++){
            if("بترول".equals(tanks.get(i).fuelType)){
                if(i==tankIndex) return v;
                v++;
            }
        }
        return -1;
    }

    boolean recalcOne(int visualIndex,int tankIndex,boolean warn){
        if(visualIndex<0||visualIndex>=dipInputs.size()) return false;
        String hs=dipInputs.get(visualIndex).getText().toString().trim();
        TextView out=actualViews.get(visualIndex);
        if(hs.isEmpty()){ out.setText("الكمية الفعلية: —"); return false; }
        try{
            double hv=Double.parseDouble(hs); Tank t=tanks.get(tankIndex);
            if(hv<0||hv>t.diameterCm){
                if(warn) toast("التمتير يجب أن يكون بين 0 و "+fmt(t.diameterCm)+" سم");
                out.setText("الكمية الفعلية: خطأ"); return false;
            }
            double av=TankMath.liters(t.lengthCm,t.diameterCm,hv);
            out.setText("الكمية الفعلية: "+Math.round(av)+" لتر");
            return true;
        }catch(Exception e){ if(warn) toast("تحقق من الأرقام"); return false; }
    }

    void updateSummary(){
        if(summary==null||dieselBook==null||petrolBook==null) return;
        try{
            double da=0,pa=0;
            for(int i=0;i<tanks.size();i++){
                int vi=visualIndexForTank(i); if(vi<0) continue;
                String hs=dipInputs.get(vi).getText().toString().trim(); if(hs.isEmpty()) return;
                double hv=Double.parseDouble(hs); Tank t=tanks.get(i); if(hv<0||hv>t.diameterCm) return;
                double av=TankMath.liters(t.lengthCm,t.diameterCm,hv);
                if("ديزل".equals(t.fuelType)) da+=av; else if("بترول".equals(t.fuelType)) pa+=av;
            }
            if(dieselBook.getText().toString().trim().isEmpty()||petrolBook.getText().toString().trim().isEmpty()) return;
            double dbk=Double.parseDouble(dieselBook.getText().toString()), pbk=Double.parseDouble(petrolBook.getText().toString());
            summary.setText("ملخص الجرد\nإجمالي الديزل الفعلي: "+Math.round(da)+" لتر\nإجمالي الديزل في الحساب: "+Math.round(dbk)+" لتر\n"+statusFuel(da-dbk,"الديزل")+
                    "\n\nإجمالي البترول الفعلي: "+Math.round(pa)+" لتر\nإجمالي البترول في الحساب: "+Math.round(pbk)+" لتر\n"+statusFuel(pa-pbk,"البترول"));
        }catch(Exception ignored){}
    }

    String statusFuel(double d,String fuel){
        if(d>0) return "يوجد زيادة في "+fuel+" "+Math.round(Math.abs(d))+" لتر";
        if(d<0) return "لديك عجز في "+fuel+" "+Math.round(Math.abs(d))+" لتر";
        return fuel+" مطابق";
    }

    void saveAudit(){
        if(savingAudit) return;
        double[] hv=new double[tanks.size()], av=new double[tanks.size()];
        for(int i=0;i<tanks.size();i++){
            int vi=visualIndexForTank(i);
            if(vi<0||dipInputs.get(vi).getText().toString().trim().isEmpty()){ toast("يجب إدخال التمتير لكل الخزانات"); return; }
            try{ hv[i]=Double.parseDouble(dipInputs.get(vi).getText().toString()); }
            catch(Exception e){ toast("تحقق من قيم التمتير"); return; }
            if(hv[i]<0||hv[i]>tanks.get(i).diameterCm){ toast("تمتير "+tanks.get(i).name+" غير صحيح"); return; }
            av[i]=TankMath.liters(tanks.get(i).lengthCm,tanks.get(i).diameterCm,hv[i]);
        }
        if(dieselBook.getText().toString().trim().isEmpty()||petrolBook.getText().toString().trim().isEmpty()){
            toast("يجب إدخال إجمالي رصيد الديزل وإجمالي رصيد البترول"); return;
        }
        try{
            double dieselAccount=Double.parseDouble(dieselBook.getText().toString());
            double petrolAccount=Double.parseDouble(petrolBook.getText().toString());
            savingAudit=true;
            long savedId=db.saveAudit(currentAuditUid,tanks,hv,av,dieselAccount,petrolAccount,notes.getText().toString().trim());
            if(savedId<0){ savingAudit=false; toast("تعذر حفظ الجرد"); return; }
            new AlertDialog.Builder(this).setTitle("تم الحفظ بنجاح").setMessage("هل تريد بدء جرد جديد ومسح الحقول؟")
                .setPositiveButton("نعم",(d,w)->{savingAudit=false;showAudit();})
                .setNegativeButton("لا",(d,w)->savingAudit=false)
                .setOnCancelListener(d->savingAudit=false).show();
        }catch(Exception e){ savingAudit=false; toast("تعذر حفظ الجرد"); }
    }

    void showArchive(){
        clearPage(); renderBottom("سجل الجرد"); addTopBar("سجل الجرد");
        EditText dateSearch=input("بحث بالتاريخ مثل 2026/09/22"); dateSearch.setInputType(android.text.InputType.TYPE_CLASS_TEXT); page.addView(dateSearch);
        Spinner fuel=new Spinner(this); fuel.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"الكل","ديزل","بترول"})); page.addView(fuel);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); page.addView(list);
        Runnable render=()->{
            list.removeAllViews(); Cursor c=db.listAudits(); String q=dateSearch.getText().toString().trim(); String ff=(String)fuel.getSelectedItem();
            while(c.moveToNext()){
                long id=c.getLong(0),ts=c.getLong(1); double dd=c.getDouble(2),pd=c.getDouble(3);
                String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(ts));
                if(!q.isEmpty()&&!dt.contains(q)) continue;
                String line=auditNumber(id,ts)+"\n"+dt+"\n";
                if(ff.equals("الكل")||ff.equals("ديزل")) line+="ديزل: "+Math.round(dd)+" لتر  ";
                if(ff.equals("الكل")||ff.equals("بترول")) line+="بترول: "+Math.round(pd)+" لتر";
                Button b=ghostBtn(line); b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); b.setTextColor(INK);
                b.setOnClickListener(v->showAuditRecord(id)); list.addView(b);
            }
            c.close();
        };
        render.run();
        dateSearch.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){render.run();} public void afterTextChanged(android.text.Editable e){}
        });
        fuel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> a,View v,int p,long id){render.run();} public void onNothingSelected(android.widget.AdapterView<?> a){}
        });
    }

    void showAuditRecord(long id){
        clearPage(); renderBottom("سجل الجرد"); addTopBar("نتيجة الجرد");
        Cursor c=db.getAudit(id); if(!c.moveToFirst()){c.close();return;}
        long createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));
        String dt=new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(createdAt));
        TextView num=title("رقم الجرد: "+auditNumber(id,createdAt)); num.setTextSize(17); page.addView(num);
        TextView info=title(dt); info.setTextSize(16); page.addView(info);

        Cursor items=db.getAuditItems(id);
        if(items.getCount()>0){
            while(items.moveToNext()){
                TextView t=title(items.getString(0)+"\nالتمتير: "+Math.round(items.getDouble(4))+" سم | الكمية الفعلية: "+Math.round(items.getDouble(5))+" لتر");
                t.setTextSize(15); t.setBackground(strokeShape(Color.WHITE,16,LINE)); page.addView(t);
            }
        }else{
            List<Tank> legacy=db.getTanks();
            for(int i=0;i<4&&i<legacy.size();i++){
                int n=i+1;
                TextView t=title(legacy.get(i).name+"\nالتمتير: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_h")))+" سم | الكمية الفعلية: "+Math.round(c.getDouble(c.getColumnIndexOrThrow("t"+n+"_actual")))+" لتر");
                t.setTextSize(15); t.setBackground(strokeShape(Color.WHITE,16,LINE)); page.addView(t);
            }
        }
        items.close();

        double dd=c.getDouble(c.getColumnIndexOrThrow("diesel_diff")),pd=c.getDouble(c.getColumnIndexOrThrow("petrol_diff"));
        TextView sum=title(statusFuel(dd,"الديزل")+"\n"+statusFuel(pd,"البترول")); sum.setTextSize(18); page.addView(sum);
        String nt=c.getString(c.getColumnIndexOrThrow("notes")); if(nt!=null&&!nt.isEmpty()){TextView n=title("ملاحظات: "+nt);n.setTextSize(15);page.addView(n);}
        c.close();
        Button pdf=btn("تصدير PDF"); pdf.setOnClickListener(v->requestAuditExport(id,true)); page.addView(pdf);
        Button xls=btn("تصدير Excel"); xls.setOnClickListener(v->requestAuditExport(id,false)); page.addView(xls);
    }

    void requestAuditExport(long id,boolean pdf){
        exportAuditId=id; exportPdf=pdf;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE,"مطابقة_الجرد_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date())+(pdf?".pdf":".xlsx"));
        startActivityForResult(i,CREATE_DOC);
    }

    void showReports(){
        clearPage(); renderBottom("التقارير"); addTopBar("التقارير");
        LinearLayout intro=panel(); TextView info=title("تحليل نتائج الجرد"); info.setTextSize(18); info.setPadding(0,0,0,0); intro.addView(info);
        TextView ii=small("ملخصات الزيادة والعجز وتقارير PDF للفترة"); ii.setPadding(0,dp(4),0,0); intro.addView(ii); page.addView(intro);
        Button all=btn("عرض ملخص كل الفترة"); all.setOnClickListener(v->showReportSummary(0,Long.MAX_VALUE)); page.addView(all);
        Button last30=btn("آخر 30 يوم"); last30.setOnClickListener(v->showReportSummary(System.currentTimeMillis()-30L*24*60*60*1000,Long.MAX_VALUE)); page.addView(last30);
        Button export=btn("تصدير تقرير الفترة PDF"); export.setOnClickListener(v->{
            reportStart=0; reportEnd=Long.MAX_VALUE; Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_TITLE,"تقرير_فترة_مطابقة_الجرد.pdf"); startActivityForResult(i,CREATE_PERIOD_PDF);
        }); page.addView(export);
    }

    void showReportSummary(long start,long end){
        clearPage(); renderBottom("التقارير"); addTopBar("ملخص التقارير");
        Cursor c=db.listAuditsBetween(start,end); int count=0; double posD=0,negD=0,posP=0,negP=0;
        while(c.moveToNext()){count++;double d=c.getDouble(2),p=c.getDouble(3);if(d>=0)posD+=d;else negD+=d;if(p>=0)posP+=p;else negP+=p;}
        c.close();
        TextView x=title("عدد الجردات: "+count+"\n\nالديزل\nإجمالي الزيادة: "+Math.round(posD)+" لتر\nإجمالي العجز: "+Math.round(Math.abs(negD))+" لتر"+
                "\n\nالبترول\nإجمالي الزيادة: "+Math.round(posP)+" لتر\nإجمالي العجز: "+Math.round(Math.abs(negP))+" لتر");
        x.setTextSize(18); page.addView(x);
        Button back=btn("رجوع للتقارير"); back.setOnClickListener(v->showReports()); page.addView(back);
    }

    void showSettingsPage(){
        clearPage(); renderBottom("الإعدادات"); tanks=db.getTanks(); addTopBar("الإعدادات");
        TextView brandInfo=small("مطابقة الجرد  •  بواسطة أبو قناف للأتمتة"); brandInfo.setGravity(Gravity.CENTER); brandInfo.setPadding(0,0,0,dp(8)); page.addView(brandInfo);
        EditText station=input("اسم المحطة"); station.setInputType(android.text.InputType.TYPE_CLASS_TEXT); station.setText(prefs.getString("station_name","")); page.addView(station);
        Button saveStation=btn("حفظ اسم المحطة"); saveStation.setOnClickListener(v->{prefs.edit().putString("station_name",station.getText().toString().trim()).apply();toast("تم حفظ اسم المحطة");}); page.addView(saveStation);

        Button logo=ghostBtn("اختيار شعار من معرض الهاتف"); logo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_LOGO);}); page.addView(logo);

        TextView th=title("إدارة الخزانات"); th.setTextSize(20); page.addView(th);
        for(Tank t:new ArrayList<>(tanks)) addTankSettingsCard(t);

        Button add=btn("＋ إضافة خزان جديد"); add.setOnClickListener(v->showAddTankDialog()); page.addView(add);
    }

    void addTankSettingsCard(Tank t){
        LinearLayout card=panel();
        EditText name=input("اسم الخزان"); name.setInputType(android.text.InputType.TYPE_CLASS_TEXT); name.setText(t.name); card.addView(name);
        Spinner fuel=new Spinner(this); fuel.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ديزل","بترول"}));
        fuel.setSelection("بترول".equals(t.fuelType)?1:0); card.addView(fuel);

        Spinner mode=new Spinner(this);
        mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"أعرف السعة والارتفاع","أعرف الطول والقطر"}));
        card.addView(mode);

        EditText capacity=input("سعة الخزان باللتر");
        double cap=Math.PI*Math.pow(t.diameterCm/2.0,2)*t.lengthCm/1000.0;
        capacity.setText(String.valueOf(Math.round(cap))); card.addView(capacity);

        EditText height=input("ارتفاع الخزان سم"); height.setText(fmt(t.diameterCm)); card.addView(height);

        EditText len=input("الطول سم"); len.setText(fmt(t.lengthCm)); card.addView(len);
        EditText dia=input("القطر سم"); dia.setText(fmt(t.diameterCm)); card.addView(dia);

        Runnable applyMode=()->{
            boolean byCapacity=mode.getSelectedItemPosition()==0;
            capacity.setVisibility(byCapacity?View.VISIBLE:View.GONE);
            height.setVisibility(byCapacity?View.VISIBLE:View.GONE);
            len.setVisibility(byCapacity?View.GONE:View.VISIBLE);
            dia.setVisibility(byCapacity?View.GONE:View.VISIBLE);
        };
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){applyMode.run();}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        applyMode.run();

        Button save=ghostBtn("حفظ الخزان"); save.setOnClickListener(v->{
            try{
                t.name=name.getText().toString().trim(); t.fuelType=(String)fuel.getSelectedItem();
                if(mode.getSelectedItemPosition()==0){
                    double capL=Double.parseDouble(capacity.getText().toString());
                    double hCm=Double.parseDouble(height.getText().toString());
                    if(capL<=0||hCm<=0){toast("تحقق من السعة والارتفاع");return;}
                    t.diameterCm=hCm;
                    t.lengthCm=derivedLengthCm(capL,hCm);
                }else{
                    t.lengthCm=Double.parseDouble(len.getText().toString());
                    t.diameterCm=Double.parseDouble(dia.getText().toString());
                }
                if(t.name.isEmpty()||t.lengthCm<=0||t.diameterCm<=0){toast("تحقق من بيانات الخزان");return;}
                db.updateTank(t); toast("تم حفظ الخزان"); showSettingsPage();
            }catch(Exception e){toast("تحقق من بيانات الخزان");}
        }); card.addView(save);

        Button del=ghostBtn("حذف الخزان"); del.setTextColor(Color.rgb(185,40,40)); del.setOnClickListener(v->
            new AlertDialog.Builder(this).setTitle("حذف الخزان").setMessage("سيتم حذف الخزان من الجرد القادم فقط، ولن تتأثر الجردات السابقة. هل تريد المتابعة؟")
                .setPositiveButton("حذف",(d,w)->{db.deleteTank(t.id);showSettingsPage();}).setNegativeButton("إلغاء",null).show()
        ); card.addView(del);
        page.addView(card);
    }

    void showAddTankDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(8),dp(18),0);
        EditText name=input("اسم الخزان"); name.setInputType(android.text.InputType.TYPE_CLASS_TEXT); box.addView(name);

        Spinner fuel=new Spinner(this); fuel.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ديزل","بترول"})); box.addView(fuel);

        TextView typeInfo=small("شكل الخزان: أسطواني أفقي"); typeInfo.setPadding(0,dp(8),0,dp(8)); box.addView(typeInfo);

        Spinner mode=new Spinner(this);
        mode.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"أعرف السعة والارتفاع","أعرف الطول والقطر"}));
        box.addView(mode);

        EditText capacity=input("سعة الخزان باللتر"); box.addView(capacity);
        EditText height=input("ارتفاع الخزان سم"); box.addView(height);
        EditText len=input("الطول سم"); box.addView(len);
        EditText dia=input("القطر سم"); box.addView(dia);

        Runnable applyMode=()->{
            boolean byCapacity=mode.getSelectedItemPosition()==0;
            capacity.setVisibility(byCapacity?View.VISIBLE:View.GONE);
            height.setVisibility(byCapacity?View.VISIBLE:View.GONE);
            len.setVisibility(byCapacity?View.GONE:View.VISIBLE);
            dia.setVisibility(byCapacity?View.GONE:View.VISIBLE);
        };
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){applyMode.run();}
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        applyMode.run();

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("إضافة خزان جديد").setView(box).setPositiveButton("إضافة",null).setNegativeButton("إلغاء",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                String n=name.getText().toString().trim();
                if(n.isEmpty()){toast("أدخل اسم الخزان");return;}

                double lengthCm,diameterCm;
                if(mode.getSelectedItemPosition()==0){
                    double capL=Double.parseDouble(capacity.getText().toString());
                    double hCm=Double.parseDouble(height.getText().toString());
                    if(capL<=0||hCm<=0){toast("تحقق من السعة والارتفاع");return;}
                    diameterCm=hCm;
                    lengthCm=derivedLengthCm(capL,hCm);
                }else{
                    lengthCm=Double.parseDouble(len.getText().toString());
                    diameterCm=Double.parseDouble(dia.getText().toString());
                    if(lengthCm<=0||diameterCm<=0){toast("تحقق من الطول والقطر");return;}
                }

                db.addTank(n,(String)fuel.getSelectedItem(),lengthCm,diameterCm);
                dialog.dismiss(); showSettingsPage();
            }catch(Exception e){toast("تحقق من بيانات الخزان");}
        }));
        dialog.show();
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(req==PICK_LOGO){
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            prefs.edit().putString("logo_uri",uri.toString()).apply(); toast("تم حفظ الشعار"); return;
        }
        try(OutputStream out=getContentResolver().openOutputStream(uri)){
            String station=prefs.getString("station_name","مطابقة الجرد");
            if(req==CREATE_DOC){
                Cursor c=db.getAudit(exportAuditId); Cursor items=db.getAuditItems(exportAuditId);
                if(exportPdf) PdfReport.writeAudit(this,out,c,items,db.getTanks(),station);
                else XlsxWriter.writeAudit(out,c,items,db.getTanks(),station);
                items.close(); c.close(); toast("تم إنشاء الملف");
            }else if(req==CREATE_PERIOD_PDF){
                Cursor c=db.listAuditsBetween(reportStart,reportEnd);
                PdfReport.writePeriod(out,c,station,"تقرير فترة مطابقة الجرد"); c.close(); toast("تم إنشاء تقرير الفترة");
            }
        }catch(Exception e){toast("فشل التصدير: "+e.getMessage());}
    }

    double derivedLengthCm(double capacityLiters,double heightCm){
        double r=heightCm/2.0;
        return (capacityLiters*1000.0)/(Math.PI*r*r);
    }

    String auditNumber(long id,long ts){
        String d=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date(ts));
        return String.format(Locale.US,"JRD-%s-%03d",d,id);
    }

    String fmt(double x){ return x==Math.rint(x)?String.valueOf((long)x):String.valueOf(x); }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}