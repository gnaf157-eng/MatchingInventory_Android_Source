package com.abognaf.matchinginventory;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB = "matching_inventory.db";
    private static final int VER = 2;

    public DbHelper(Context c) { super(c, DB, null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tanks(id INTEGER PRIMARY KEY, name TEXT NOT NULL, fuel TEXT NOT NULL, length_cm REAL NOT NULL, diameter_cm REAL NOT NULL)");
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
        // Dimensions adopted from the provided tank screenshot, converted to cm.
        db.execSQL("INSERT INTO tanks VALUES(1,'ديزل رئيسي','ديزل',739,276)");
        db.execSQL("INSERT INTO tanks VALUES(2,'ديزل احتياط 1','ديزل',676,276)");
        db.execSQL("INSERT INTO tanks VALUES(3,'ديزل احتياط 2','ديزل',676,310)");
        db.execSQL("INSERT INTO tanks VALUES(4,'بترول','بترول',739,276)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE audits ADD COLUMN audit_uid TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_audits_uid ON audits(audit_uid)");
        }
    }

    public List<Tank> getTanks() {
        ArrayList<Tank> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,name,fuel,length_cm,diameter_cm FROM tanks ORDER BY id", null);
        while (c.moveToNext()) out.add(new Tank(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4)));
        c.close();
        return out;
    }

    public void updateTank(Tank t) {
        ContentValues v = new ContentValues();
        v.put("name", t.name); v.put("fuel", t.fuelType);
        v.put("length_cm", t.lengthCm); v.put("diameter_cm", t.diameterCm);
        getWritableDatabase().update("tanks", v, "id=?", new String[]{String.valueOf(t.id)});
    }

    public long saveAudit(String auditUid, double[] h, double[] actual, double dieselBook, double petrolBook, String notes) {
        long now = System.currentTimeMillis();

        Cursor byUid = getReadableDatabase().rawQuery("SELECT id FROM audits WHERE audit_uid=? LIMIT 1", new String[]{auditUid});
        try {
            if (byUid.moveToFirst()) return byUid.getLong(0);
        } finally {
            byUid.close();
        }

        Cursor last = getReadableDatabase().rawQuery(
            "SELECT id,created_at,t1_h,t2_h,t3_h,t4_h,diesel_book,petrol_book,notes FROM audits ORDER BY id DESC LIMIT 1", null);
        try {
            if (last.moveToFirst()) {
                long lastTime = last.getLong(1);
                String lastNotes = last.getString(8) == null ? "" : last.getString(8);
                String newNotes = notes == null ? "" : notes;
                boolean same = now-lastTime < 10000
                    && Double.compare(last.getDouble(2), h[0])==0
                    && Double.compare(last.getDouble(3), h[1])==0
                    && Double.compare(last.getDouble(4), h[2])==0
                    && Double.compare(last.getDouble(5), h[3])==0
                    && Double.compare(last.getDouble(6), dieselBook)==0
                    && Double.compare(last.getDouble(7), petrolBook)==0
                    && lastNotes.equals(newNotes);
                if (same) return last.getLong(0);
            }
        } finally {
            last.close();
        }

        ContentValues v = new ContentValues();
        v.put("audit_uid", auditUid);
        v.put("created_at", now);
        v.put("notes", notes == null ? "" : notes);
        double dieselActual=0, petrolActual=actual[3];
        for (int i=0;i<4;i++) {
            int n=i+1;
            v.put("t"+n+"_h",h[i]);
            v.put("t"+n+"_actual",actual[i]);
            v.put("t"+n+"_book",0);
            v.put("t"+n+"_diff",0);
            if (i<3) dieselActual += actual[i];
        }
        v.put("diesel_actual",dieselActual);
        v.put("diesel_book",dieselBook);
        v.put("diesel_diff",dieselActual-dieselBook);
        v.put("petrol_actual",petrolActual);
        v.put("petrol_book",petrolBook);
        v.put("petrol_diff",petrolActual-petrolBook);
        return getWritableDatabase().insertOrThrow("audits", null, v);
    }

    public Cursor getAudit(long id) {
        return getReadableDatabase().rawQuery("SELECT * FROM audits WHERE id=?", new String[]{String.valueOf(id)});
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