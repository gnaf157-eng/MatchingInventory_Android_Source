package com.abognaf.matchinginventory;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB = "matching_inventory.db";
    private static final int VER = 3;

    public DbHelper(Context c) { super(c, DB, null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tanks(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, fuel TEXT NOT NULL, length_cm REAL NOT NULL, diameter_cm REAL NOT NULL)");
        db.execSQL("CREATE TABLE audits(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "audit_uid TEXT UNIQUE," +
                "created_at INTEGER NOT NULL, notes TEXT," +
                "t1_h REAL,t1_actual REAL,t1_book REAL,t1_diff REAL," +
                "t2_h REAL,t2_actual REAL,t2_book REAL,t2_diff REAL," +
                "t3_h REAL,t3_actual REAL,t3_book REAL,t3_diff REAL," +
                "t4_h REAL,t4_actual REAL,t4_book REAL,t4_diff REAL," +
                "diesel_actual REAL,diesel_book REAL,diesel_diff REAL," +
                "petrol_actual REAL,petrol_book REAL,petrol_diff REAL)");
        db.execSQL("CREATE TABLE audit_items(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "audit_id INTEGER NOT NULL," +
                "tank_id INTEGER," +
                "tank_name TEXT NOT NULL," +
                "fuel TEXT NOT NULL," +
                "length_cm REAL NOT NULL," +
                "diameter_cm REAL NOT NULL," +
                "dip_cm REAL NOT NULL," +
                "actual_liters REAL NOT NULL)");
        insertDefaults(db);
    }

    private void insertDefaults(SQLiteDatabase db){
        db.execSQL("INSERT INTO tanks(name,fuel,length_cm,diameter_cm) VALUES('ديزل رئيسي','ديزل',676,276)");
        db.execSQL("INSERT INTO tanks(name,fuel,length_cm,diameter_cm) VALUES('ديزل احتياط 1','ديزل',676,310)");
        db.execSQL("INSERT INTO tanks(name,fuel,length_cm,diameter_cm) VALUES('ديزل احتياط 2','ديزل',739,276)");
        db.execSQL("INSERT INTO tanks(name,fuel,length_cm,diameter_cm) VALUES('بترول','بترول',739,276)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE audits ADD COLUMN audit_uid TEXT"); } catch(Exception ignored){}
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_audits_uid ON audits(audit_uid)");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS audit_items(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "audit_id INTEGER NOT NULL," +
                    "tank_id INTEGER," +
                    "tank_name TEXT NOT NULL," +
                    "fuel TEXT NOT NULL," +
                    "length_cm REAL NOT NULL," +
                    "diameter_cm REAL NOT NULL," +
                    "dip_cm REAL NOT NULL," +
                    "actual_liters REAL NOT NULL)");
            // Apply the requested default dimensions only to the original four tanks.
            db.execSQL("UPDATE tanks SET name='ديزل رئيسي',fuel='ديزل',length_cm=676,diameter_cm=276 WHERE id=1");
            db.execSQL("UPDATE tanks SET name='ديزل احتياط 1',fuel='ديزل',length_cm=676,diameter_cm=310 WHERE id=2");
            db.execSQL("UPDATE tanks SET name='ديزل احتياط 2',fuel='ديزل',length_cm=739,diameter_cm=276 WHERE id=3");
            db.execSQL("UPDATE tanks SET name='بترول',fuel='بترول',length_cm=739,diameter_cm=276 WHERE id=4");
        }
    }

    public List<Tank> getTanks() {
        ArrayList<Tank> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,fuel,length_cm,diameter_cm FROM tanks ORDER BY id", null);
        while (c.moveToNext()) out.add(new Tank(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4)));
        c.close();
        return out;
    }

    public long addTank(String name,String fuel,double length,double diameter){
        ContentValues v=new ContentValues();
        v.put("name",name); v.put("fuel",fuel); v.put("length_cm",length); v.put("diameter_cm",diameter);
        return getWritableDatabase().insertOrThrow("tanks",null,v);
    }

    public void updateTank(Tank t) {
        ContentValues v = new ContentValues();
        v.put("name", t.name); v.put("fuel", t.fuelType);
        v.put("length_cm", t.lengthCm); v.put("diameter_cm", t.diameterCm);
        getWritableDatabase().update("tanks", v, "id=?", new String[]{String.valueOf(t.id)});
    }

    public void deleteTank(long id){
        getWritableDatabase().delete("tanks","id=?",new String[]{String.valueOf(id)});
    }

    public long saveAudit(String auditUid, List<Tank> tanks, double[] h, double[] actual, double dieselBook, double petrolBook, String notes) {
        SQLiteDatabase db=getWritableDatabase();
        Cursor byUid = db.rawQuery("SELECT id FROM audits WHERE audit_uid=? LIMIT 1", new String[]{auditUid});
        try { if (byUid.moveToFirst()) return byUid.getLong(0); } finally { byUid.close(); }

        double dieselActual=0, petrolActual=0;
        for(int i=0;i<tanks.size();i++){
            if("ديزل".equals(tanks.get(i).fuelType)) dieselActual+=actual[i];
            else if("بترول".equals(tanks.get(i).fuelType)) petrolActual+=actual[i];
        }

        db.beginTransaction();
        try{
            ContentValues v=new ContentValues();
            v.put("audit_uid",auditUid); v.put("created_at",System.currentTimeMillis());
            v.put("notes",notes==null?"":notes);
            v.put("diesel_actual",dieselActual); v.put("diesel_book",dieselBook); v.put("diesel_diff",dieselActual-dieselBook);
            v.put("petrol_actual",petrolActual); v.put("petrol_book",petrolBook); v.put("petrol_diff",petrolActual-petrolBook);
            long auditId=db.insertOrThrow("audits",null,v);

            for(int i=0;i<tanks.size();i++){
                Tank t=tanks.get(i);
                ContentValues x=new ContentValues();
                x.put("audit_id",auditId); x.put("tank_id",t.id); x.put("tank_name",t.name); x.put("fuel",t.fuelType);
                x.put("length_cm",t.lengthCm); x.put("diameter_cm",t.diameterCm); x.put("dip_cm",h[i]); x.put("actual_liters",actual[i]);
                db.insertOrThrow("audit_items",null,x);
            }
            db.setTransactionSuccessful();
            return auditId;
        } finally { db.endTransaction(); }
    }

    public Cursor getAudit(long id) {
        return getReadableDatabase().rawQuery("SELECT * FROM audits WHERE id=?", new String[]{String.valueOf(id)});
    }

    public Cursor getAuditItems(long auditId){
        return getReadableDatabase().rawQuery(
            "SELECT tank_name,fuel,length_cm,diameter_cm,dip_cm,actual_liters FROM audit_items WHERE audit_id=? ORDER BY id",
            new String[]{String.valueOf(auditId)});
    }

    public Cursor listAudits() {
        return getReadableDatabase().rawQuery("SELECT id,created_at,diesel_diff,petrol_diff FROM audits ORDER BY created_at DESC", null);
    }

    public Cursor listAuditsBetween(long start, long end) {
        return getReadableDatabase().rawQuery(
            "SELECT id,created_at,diesel_diff,petrol_diff FROM audits WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC",
            new String[]{String.valueOf(start), String.valueOf(end)});
    }
}