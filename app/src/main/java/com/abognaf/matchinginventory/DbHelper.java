package com.abognaf.matchinginventory;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB = "matching_inventory.db";
    private static final int VER = 1;

    public DbHelper(Context c) { super(c, DB, null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tanks(id INTEGER PRIMARY KEY, name TEXT NOT NULL, fuel TEXT NOT NULL, length_cm REAL NOT NULL, diameter_cm REAL NOT NULL)");
        db.execSQL("CREATE TABLE audits(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
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

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

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

    public long saveAudit(double[] h, double[] actual, double[] book, String notes) {
        ContentValues v = new ContentValues();
        v.put("created_at", System.currentTimeMillis());
        v.put("notes", notes == null ? "" : notes);
        double da=0, db=0, pa=actual[3], pb=book[3];
        for (int i=0;i<4;i++) {
            int n=i+1;
            v.put("t"+n+"_h",h[i]); v.put("t"+n+"_actual",actual[i]); v.put("t"+n+"_book",book[i]); v.put("t"+n+"_diff",actual[i]-book[i]);
            if (i<3) { da += actual[i]; db += book[i]; }
        }
        v.put("diesel_actual",da); v.put("diesel_book",db); v.put("diesel_diff",da-db);
        v.put("petrol_actual",pa); v.put("petrol_book",pb); v.put("petrol_diff",pa-pb);
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