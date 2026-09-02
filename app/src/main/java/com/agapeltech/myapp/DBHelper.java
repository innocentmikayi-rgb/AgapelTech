package com.agapeltech.myapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "materialsDB";
    private static final int DB_VERSION = 9;

    public DBHelper(Context context){
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Products Inventory
        db.execSQL("CREATE TABLE materials (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_name TEXT UNIQUE, " +
                "buying_price REAL, " +
                "selling_price REAL, " +
                "firebase_key TEXT, " +
                "synced INTEGER DEFAULT 0, " +
                "stock_qty INTEGER DEFAULT 0, " +
                "low_stock_threshold INTEGER DEFAULT 5, " +
                "category TEXT DEFAULT 'Uncategorized')");

        // 2. Sales Ledger
        db.execSQL("CREATE TABLE sales_table (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sale_date TEXT, " +
                "particulars TEXT, " +
                "qty INTEGER, " +
                "buying_price REAL, " +
                "selling_price REAL, " +
                "expected_amount REAL, " +
                "expected_profit REAL, " +
                "actual_amount REAL, " +
                "actual_profit REAL, " +
                "balance REAL, " +
                "status_tag TEXT, " +
                "customer_name TEXT, " +
                "customer_phone TEXT, " +
                "sale_time TEXT, " +
                "firebase_key TEXT, " +
                "synced INTEGER DEFAULT 0)");

        // 3. Expenses
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "exp_date TEXT, " +
                "description TEXT, " +
                "amount REAL, " +
                "category TEXT, " +
                "firebase_key TEXT, " +
                "synced INTEGER DEFAULT 0)");

        // 4. Users (Offline Authentication)
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, " +
                "password TEXT, " +
                "role TEXT)");

        // Seed default users
        db.execSQL("INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', '1234', 'MANAGER')");
        db.execSQL("INSERT OR IGNORE INTO users (username, password, role) VALUES ('staff', '0000', 'STAFF')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE materials ADD COLUMN firebase_key TEXT");
                db.execSQL("ALTER TABLE materials ADD COLUMN synced INTEGER DEFAULT 0");
            } catch (Exception e) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE sales_table ADD COLUMN customer_name TEXT DEFAULT 'Walk-in'");
                db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "exp_date TEXT, " +
                        "description TEXT, " +
                        "amount REAL, " +
                        "category TEXT)");
            } catch (Exception e) {}
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE sales_table ADD COLUMN firebase_key TEXT");
                db.execSQL("ALTER TABLE sales_table ADD COLUMN synced INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE expenses ADD COLUMN firebase_key TEXT");
                db.execSQL("ALTER TABLE expenses ADD COLUMN synced INTEGER DEFAULT 0");
            } catch (Exception e) {}
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE materials ADD COLUMN stock_qty INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE materials ADD COLUMN category TEXT DEFAULT 'Uncategorized'");
            } catch (Exception e) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE sales_table ADD COLUMN customer_phone TEXT");
            } catch (Exception e) {}
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE materials ADD COLUMN low_stock_threshold INTEGER DEFAULT 5");
            } catch (Exception e) {}
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT UNIQUE, " +
                        "password TEXT, " +
                        "role TEXT)");
                db.execSQL("INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', '1234', 'MANAGER')");
                db.execSQL("INSERT OR IGNORE INTO users (username, password, role) VALUES ('staff', '0000', 'STAFF')");
            } catch (Exception e) {}
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE sales_table ADD COLUMN sale_time TEXT");
            } catch (Exception e) {}
        }
    }

    // ================= INVENTORY METHODS =================

    public double getSingleBuyingPrice(String itemName) {
        SQLiteDatabase db = this.getReadableDatabase();
        double bp = 0.0;
        Cursor cursor = db.rawQuery("SELECT buying_price FROM materials WHERE item_name = ?", new String[]{itemName});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                bp = cursor.getDouble(0);
            }
            cursor.close();
        }
        return bp;
    }

    public void insertOrUpdate(String name, double buy, double sell, int qty, String cat, int threshold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("item_name", name);
        cv.put("buying_price", buy);
        cv.put("selling_price", sell);
        cv.put("stock_qty", qty);
        cv.put("category", cat);
        cv.put("low_stock_threshold", threshold);
        cv.put("synced", 0);
        db.insertWithOnConflict("materials", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void reduceStock(String itemName, int qtySold) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE materials SET stock_qty = stock_qty - " + qtySold + ", synced = 0 WHERE item_name = ?", new String[]{itemName});
    }

    public void updateFirebaseKey(String name, String key){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("firebase_key", key);
        cv.put("synced", 1);
        db.update("materials", cv, "item_name=?", new String[]{name});
    }

    public void updateSaleFirebaseKey(int id, String key){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("firebase_key", key);
        cv.put("synced", 1);
        db.update("sales_table", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void updateExpenseFirebaseKey(int id, String key){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("firebase_key", key);
        cv.put("synced", 1);
        db.update("expenses", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM materials WHERE synced != -1", null);
    }

    public Cursor getUnsyncedData(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM materials WHERE synced = 0", null);
    }

    public Cursor getUnsyncedSales(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM sales_table WHERE synced = 0", null);
    }

    public Cursor getUnsyncedExpenses(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM expenses WHERE synced = 0", null);
    }

    public Cursor getPendingDeletionData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM materials WHERE synced = -1", null);
    }

    public Cursor getPendingDeletionSales() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM sales_table WHERE synced = -1", null);
    }

    public Cursor getPendingDeletionExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM expenses WHERE synced = -1", null);
    }

    public String getFirebaseKey(String name){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT firebase_key FROM materials WHERE item_name=?", new String[]{name});
        String key = null;
        if(cursor.moveToFirst()){
            key = cursor.getString(0);
        }
        cursor.close();
        return key;
    }

    public void markAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("synced", 1);
        db.update("materials", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void markForDeletion(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("synced", -1);
        db.update("materials", cv, "item_name=?", new String[]{name});
    }

    public void markSaleAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("synced", 1);
        db.update("sales_table", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void markSaleForDeletion(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("synced", -1);
        db.update("sales_table", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void markExpenseAsSynced(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("synced", 1);
        db.update("expenses", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void markExpenseForDeletion(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("synced", -1);
        db.update("expenses", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteItemPermanently(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("materials", "item_name=?", new String[]{name});
    }

    public void deleteSalePermanently(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("sales_table", "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteExpensePermanently(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("expenses", "id=?", new String[]{String.valueOf(id)});
    }

    // ================= SALES METHODS =================

    public boolean insertSaleLog(String date, String part, int qty, double bp, double sp, 
                                 double expAmt, double expProf, double actAmt, double actProf, double bal, String tag, String customer, String phone, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sale_date", date);
        cv.put("particulars", part);
        cv.put("qty", qty);
        cv.put("buying_price", bp);
        cv.put("selling_price", sp);
        cv.put("expected_amount", expAmt);
        cv.put("expected_profit", expProf);
        cv.put("actual_amount", actAmt);
        cv.put("actual_profit", actProf);
        cv.put("balance", bal);
        cv.put("status_tag", tag);
        cv.put("customer_name", customer);
        cv.put("customer_phone", phone);
        cv.put("sale_time", time);
        cv.put("synced", 0);
        
        long res = db.insert("sales_table", null, cv);
        return res != -1;
    }

    public void upsertSaleFromFirebase(String key, String date, String part, int qty, double bp, double sp, 
                                      double actualPaid, String customer, String phone, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        double expectedAmount = qty * sp;
        double expectedProfit = (sp - bp) * qty;
        double balance = expectedAmount - actualPaid;
        double actualProfit = expectedProfit - balance;
        
        ContentValues cv = new ContentValues();
        cv.put("sale_date", date);
        cv.put("particulars", part);
        cv.put("qty", qty);
        cv.put("buying_price", bp);
        cv.put("selling_price", sp);
        cv.put("expected_amount", expectedAmount);
        cv.put("expected_profit", expectedProfit);
        cv.put("actual_amount", actualPaid);
        cv.put("actual_profit", actualProfit);
        cv.put("balance", balance);
        cv.put("customer_name", customer);
        cv.put("customer_phone", phone);
        cv.put("sale_time", time);
        cv.put("firebase_key", key);
        cv.put("synced", 1);
        
        Cursor c = db.rawQuery("SELECT id FROM sales_table WHERE firebase_key = ?", new String[]{key});
        if (c.moveToFirst()) {
            db.update("sales_table", cv, "id=?", new String[]{String.valueOf(c.getInt(0))});
        } else {
            db.insert("sales_table", null, cv);
        }
        c.close();
    }

    public void upsertExpenseFromFirebase(String key, String date, String desc, double amount, String cat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("exp_date", date);
        cv.put("description", desc);
        cv.put("amount", amount);
        cv.put("category", cat);
        cv.put("firebase_key", key);
        cv.put("synced", 1);

        Cursor c = db.rawQuery("SELECT id FROM expenses WHERE firebase_key = ?", new String[]{key});
        if (c.moveToFirst()) {
            db.update("expenses", cv, "id=?", new String[]{String.valueOf(c.getInt(0))});
        } else {
            db.insert("expenses", null, cv);
        }
        c.close();
    }

    public void updateSaleRecord(int id, String particulars, int qty, double bp, double sp, 
                                 double expAmt, double expProf, double actAmt, double actProf, double bal, String tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("particulars", particulars);
        cv.put("qty", qty);
        cv.put("buying_price", bp);
        cv.put("selling_price", sp);
        cv.put("expected_amount", expAmt);
        cv.put("expected_profit", expProf);
        cv.put("actual_amount", actAmt);
        cv.put("actual_profit", actProf);
        cv.put("balance", bal);
        cv.put("status_tag", tag);
        cv.put("synced", 0);
        
        db.update("sales_table", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor getMonthlySalesRecords(String monthQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM sales_table WHERE sale_date LIKE ? ORDER BY id DESC", new String[]{"%" + monthQuery});
    }

    public Cursor getAllSalesRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM sales_table WHERE synced != -1 ORDER BY id DESC", null);
    }

    public HashMap<String, Double> getDailyTotals(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        HashMap<String, Double> totals = new HashMap<>();
        totals.put("sales", 0.0);
        totals.put("profit", 0.0);
        totals.put("credit", 0.0);

        Cursor cursor = db.rawQuery("SELECT SUM(actual_amount), SUM(actual_profit), SUM(balance) FROM sales_table WHERE sale_date = ?", new String[]{date});
        if (cursor != null && cursor.moveToFirst()) {
            totals.put("sales", cursor.getDouble(0));
            totals.put("profit", cursor.getDouble(1));
            totals.put("credit", cursor.getDouble(2));
            cursor.close();
        }
        return totals;
    }

    public double getPeriodSales(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        // Note: This simple date comparison works if format is YYYY-MM-DD. 
        // Our format is dd/MM/yyyy which is BAD for comparison.
        // I should probably change the date format to YYYY-MM-DD in the database.
        return total;
    }

    public double getSalesForQuery(String query, String[] args) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(actual_amount) FROM sales_table WHERE " + query, args);
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    // ================= EXPENSES METHODS =================

    public boolean insertExpense(String date, String desc, double amount, String cat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("exp_date", date);
        cv.put("description", desc);
        cv.put("amount", amount);
        cv.put("category", cat);
        cv.put("synced", 0);
        return db.insert("expenses", null, cv) != -1;
    }

    public void updateExpenseRecord(int id, String desc, double amount, String cat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("description", desc);
        cv.put("amount", amount);
        cv.put("category", cat);
        cv.put("synced", 0);
        db.update("expenses", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public double getMonthlyExpenses(String monthQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM expenses WHERE exp_date LIKE ?", new String[]{"%" + monthQuery});
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public Cursor getAllExpenses() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM expenses WHERE synced != -1 ORDER BY id DESC", null);
    }

    // ================= CRM METHODS =================

    public Cursor getCustomersWithDebt() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT customer_name, SUM(balance) as total_debt, " +
                "(SELECT customer_phone FROM sales_table s2 WHERE s2.customer_name = sales_table.customer_name AND s2.customer_phone != '' AND s2.customer_phone IS NOT NULL ORDER BY id DESC LIMIT 1) as phone " +
                "FROM sales_table WHERE balance > 0 GROUP BY customer_name", null);
    }

    public String getCustomerNameForSale(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT customer_name FROM sales_table WHERE id = ?", new String[]{String.valueOf(id)});
        String name = "Walk-in";
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(0);
            cursor.close();
        }
        return name;
    }

    // ================= ANALYTICS METHODS =================

    public HashMap<String, Double> getCategoryWiseProfit() {
        SQLiteDatabase db = this.getReadableDatabase();
        HashMap<String, Double> categoryProfit = new HashMap<>();
        Cursor cursor = db.rawQuery(
            "SELECT m.category, SUM(s.actual_profit) " +
            "FROM sales_table s " +
            "JOIN materials m ON s.particulars = m.item_name " +
            "GROUP BY m.category", null);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                categoryProfit.put(cursor.getString(0), cursor.getDouble(1));
            }
            cursor.close();
        }
        return categoryProfit;
    }

    public double getPreviousMonthProfit(String monthQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(actual_profit) FROM sales_table WHERE sale_date LIKE ?", new String[]{"%" + monthQuery});
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public Cursor getLowStockItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT item_name, stock_qty FROM materials WHERE stock_qty <= low_stock_threshold AND synced != -1", null);
    }

    public Cursor getTopSellingProducts(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT particulars, SUM(qty) as total_qty FROM sales_table GROUP BY particulars ORDER BY total_qty DESC LIMIT " + limit, null);
    }

    public double getAverageTransactionValue(String monthQuery) {
        SQLiteDatabase db = this.getReadableDatabase();
        double avg = 0;
        Cursor cursor = db.rawQuery("SELECT AVG(actual_amount) FROM sales_table WHERE sale_date LIKE ?", new String[]{"%" + monthQuery});
        if (cursor != null && cursor.moveToFirst()) {
            avg = cursor.getDouble(0);
            cursor.close();
        }
        return avg;
    }

    // ================= USER METHODS =================

    public String checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM users WHERE username = ? AND password = ?", new String[]{username, password});
        String role = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                role = cursor.getString(0);
            }
            cursor.close();
        }
        return role;
    }

    public boolean addUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password);
        cv.put("role", role);
        long result = db.insert("users", null, cv);
        return result != -1;
    }
}
