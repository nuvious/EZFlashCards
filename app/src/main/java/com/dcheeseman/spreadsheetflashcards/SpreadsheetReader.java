package com.dcheeseman.spreadsheetflashcards;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by DavidR on 2016-01-15.
 */
public class SpreadsheetReader {

    private static final String IMAGE_SHEET_NAME = "Images";

    public enum SpreadSheetFormat {
        XLSX,
        XLS,
        ODS,
        CSV
    }

    private static HSSFWorkbook getHSSFWorkbook(InputStream is) {
        try {
            return new HSSFWorkbook(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static XSSFWorkbook getXSSFWorkbook(InputStream is) {
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, List<List<String>>> readCSV(String filename, InputStream is) {
        List<List<String>> resultList = new ArrayList<List<String>>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                List<String> lr = new ArrayList<String>();
                String[] row = csvLine.split(",");

                for (String s : row) {
                    lr.add(s);
                }
                resultList.add(lr);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: " + ex);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: " + e);
            }
        }

        Map<String, List<List<String>>> ret = new HashMap<String, List<List<String>>>();
        ret.put(filename, resultList);
        return ret;
    }

    public static Map<String, List<List<String>>> readHSSF(HSSFWorkbook wb) {
        Map<String, List<List<String>>> ret = new HashMap<String, List<List<String>>>();

        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            List<List<String>> topic = new ArrayList<List<String>>();

            // Get first sheet from the workbook
            HSSFSheet sheet = wb.getSheetAt(i);

            Log.d("XLS", "Loaded sheet " + sheet.getSheetName());

            Row row;
            Cell cell;

            // Iterate through each rows from first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();

                Iterator cells = row.cellIterator();
                List<String> n = new ArrayList<String>();

                while (cells.hasNext()) {
                    cell = (HSSFCell) cells.next();
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    n.add(cell.getStringCellValue());
                }
                topic.add(n);
            }

            ret.put(sheet.getSheetName(), topic);
        }

        Log.d("XLS", String.valueOf(ret.size()) + " sheets loaded.");
        return ret;
    }

    public static Map<Integer, HSSFPicture> getPictureHSSF(HSSFWorkbook wb) {
        HSSFSheet sheet = wb.getSheet(IMAGE_SHEET_NAME);
        if (sheet == null) {
            return new HashMap<Integer, HSSFPicture>();
        }

        //Make a mapping of Interger and XSSFPictureData
        Map<Integer, HSSFPicture> map = new HashMap<Integer, HSSFPicture>();

        for (HSSFShape shape : sheet.createDrawingPatriarch().getChildren()) {

            HSSFClientAnchor anchor = (HSSFClientAnchor) shape.getAnchor();
            int rowIndex = anchor.getRow1();
            if (shape instanceof HSSFPicture) {
                int rowmark = rowIndex;
                Log.d("XSSFAnchor", "ROW: " + rowmark);
                HSSFPicture picture = (HSSFPicture) shape;
                map.put(rowmark + 1, picture);
            }
        }
        return map;
    }

    public static Map<Integer, XSSFPicture> getPicturesXSSF(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.getSheet(IMAGE_SHEET_NAME);
        if (sheet == null) {
            return new HashMap<Integer, XSSFPicture>();
        }

        //Make a mapping of Interger and XSSFPictureData
        Map<Integer, XSSFPicture> map = new HashMap<Integer, XSSFPicture>();

        for (XSSFShape shape : sheet.createDrawingPatriarch().getShapes()) {

            XSSFClientAnchor anchor = (XSSFClientAnchor) shape.getAnchor();
            int rowIndex = anchor.getRow1();
            if (shape instanceof XSSFPicture) {
                int rowmark = rowIndex;
                Log.d("XSSFAnchor", "ROW: " + rowmark);
                XSSFPicture picture = (XSSFPicture) shape;
                map.put(rowmark + 1, picture);
            }
        }
        return map;
    }

    private static Map<String, List<QAInfo>> getQAPairsHSSF(InputStream is) {
        HSSFWorkbook wb = SpreadsheetReader.getHSSFWorkbook(is);
        Map<String, List<List<String>>> celldata = readHSSF(wb);
        Map<Integer, HSSFPicture> pics = getPictureHSSF(wb);

        Map<String, List<QAInfo>> ret = new TreeMap<String, List<QAInfo>>();

        for (String topic : celldata.keySet()) {
            List<QAInfo> qas = new ArrayList<QAInfo>();
            for (List<String> row : celldata.get(topic)) {
                if (row.size() == 2) {
                    QAInfo q = new QAInfo(row.get(0), row.get(1), null, null);
                    qas.add(q);
                } else if (row.size() == 3) {
                    byte[] qpicdata = pics.get(Integer.parseInt(row.get(2))).getPictureData().getData();
                    QAInfo q = new QAInfo(row.get(0), row.get(1), qpicdata, null);
                    qas.add(q);
                } else if (row.size() == 4) {
                    Integer qindex = -1;
                    Integer aindex = -1;
                    try {
                        qindex = Integer.parseInt(row.get(2));
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        aindex = Integer.parseInt(row.get(3));
                    } catch (Exception e) {
                        continue;
                    }
                    byte[] qpicdata, apicdata;
                    qpicdata = apicdata = null;

                    if (qindex > 0) {
                        HSSFPicture pic = pics.get(qindex);
                        if (pic != null)
                            qpicdata = pic.getPictureData().getData();
                    }
                    if (aindex > 0) {
                        HSSFPicture pic = pics.get(aindex);
                        if (pic != null)
                            apicdata = pics.get(aindex).getPictureData().getData();
                    }
                    QAInfo q = new QAInfo(row.get(0), row.get(1), qpicdata, apicdata);
                    qas.add(q);
                } else {
                    //skip
                }
            }
            ret.put(topic, qas);
        }
        return ret;
    }

    /*
    public static Map<String, List<List<String>>> readODS(SpreadSheet ss) {
        Map<String, List<List<String>>> ret = new HashMap<String, List<List<String>>>();

        for (int i = 0; i < ss.getSheetCount(); i++) {
            List<List<String>> topic = new ArrayList<List<String>>();

            // Get first sheet from the workbook
            Sheet sheet = ss.getSheet(i);

            Log.d("ODS", "Loaded sheet " + sheet.getName());

            int row = 0;
            while (true) {
                String q = sheet.getCellAt(0, row).getTextValue();
                String a = sheet.getCellAt(1, row).getTextValue();
                String qp = sheet.getCellAt(2, row).getTextValue();
                String ap = sheet.getCellAt(3, row).getTextValue();
                List<String> n = new ArrayList<String>();

                if (q != "" || a != "") {
                    break;
                } else {
                    n.add(q);
                    n.add(a);
                }

                if (qp != "" && ap != "") {
                    n.add(qp);
                    n.add(ap);
                }
                topic.add(n);
            }

            ret.put(sheet.getName(), topic);
            Log.d("ODS", String.valueOf(ret.size()) + " sheets loaded.");
        }
        return ret;
    }

    public static Map<Integer, XSSFPicture> getPicturesODS(SpreadSheet ss) {
        Sheet sheet = ss.getSheet(IMAGE_SHEET_NAME);
        if (sheet == null) {
            return new HashMap<Integer, XSSFPicture>();
        }


        //Make a mapping of Interger and XSSFPictureData
        Map<Integer, XSSFPicture> map = new HashMap<Integer, XSSFPicture>();

        for (XSSFShape shape : sheet.createDrawingPatriarch().getShapes()) {

            XSSFClientAnchor anchor = (XSSFClientAnchor) shape.getAnchor();
            int rowIndex = anchor.getRow1();
            if (shape instanceof XSSFPicture) {
                int rowmark = rowIndex;
                Log.d("XSSFAnchor", "ROW: " + rowmark);
                XSSFPicture picture = (XSSFPicture) shape;
                map.put(rowmark + 1, picture);
            }
        }
        return map;
    }

    public  static Map<String, List<QAInfo>> getQAPairsODS(String uriString) throws Exception {
        URI theUri = URI.create(uriString);
        String filepath = Uri.parse(uriString).getPath();

        File f = new File(theUri.getPath());

        SpreadSheet ss = SpreadSheet.createFromFile(f);
        Map<String, List<List<String>>> celldata = readODS(ss);
        Map<Integer, XSSFPicture> pics = getPicturesODS(ss);

        Map<String, List<QAInfo>> ret = new TreeMap<String, List<QAInfo>>();

        for (String topic : celldata.keySet()) {
            List<QAInfo> qas = new ArrayList<QAInfo>();
            for (List<String> row : celldata.get(topic)) {
                if (row.size() == 2) {
                    QAInfo q = new QAInfo(row.get(0), row.get(1), null, null);
                    qas.add(q);
                } else if (row.size() == 3) {
                    byte[] qpicdata = pics.get(Integer.parseInt(row.get(2))).getPictureData().getData();
                    QAInfo q = new QAInfo(row.get(0), row.get(1), qpicdata, null);
                    qas.add(q);
                } else if (row.size() == 4) {
                    Integer qindex = -1;
                    Integer aindex = -1;
                    try {
                        qindex = Integer.parseInt(row.get(2));
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        aindex = Integer.parseInt(row.get(3));
                    } catch (Exception e) {
                        continue;
                    }
                    byte[] qpicdata, apicdata;
                    qpicdata = apicdata = null;

                    if (qindex > 0) {
                        XSSFPicture pic = pics.get(qindex);
                        if (pic != null)
                            qpicdata = pic.getPictureData().getData();
                    }
                    if (aindex > 0) {
                        XSSFPicture pic = pics.get(aindex);
                        if (pic != null)
                            apicdata = pics.get(aindex).getPictureData().getData();
                    }
                    QAInfo q = new QAInfo(row.get(0), row.get(1), qpicdata, apicdata);
                    qas.add(q);
                } else {
                    //skip
                }
            }
            ret.put(topic, qas);
        }
        f.delete();
        return ret;
    }

    */

    private static Map<String, List<QAInfo>> getQAPairsCSV(InputStream is) {
        Map<String, List<List<String>>> celldata = readCSV("CSV Flashcards", is);

        Map<String, List<QAInfo>> ret = new TreeMap<String, List<QAInfo>>();

        for (String topic : celldata.keySet()) {
            List<QAInfo> qas = new ArrayList<QAInfo>();
            for (List<String> row : celldata.get(topic)) {
                if (row.size() == 2) {
                    QAInfo q = new QAInfo(row.get(0), row.get(1), null, null);
                    qas.add(q);
                } else {
                    //skip
                }
            }
            ret.put(topic, qas);
        }
        return ret;
    }

    private static Map<String, List<QAInfo>> getQAPairsXSSF(InputStream is) {
        XSSFWorkbook wb = SpreadsheetReader.getXSSFWorkbook(is);
        Map<String, List<List<String>>> celldata = readXlsx(wb);
        Map<Integer, XSSFPicture> pics = getPicturesXSSF(wb);

        Map<String, List<QAInfo>> ret = new TreeMap<String, List<QAInfo>>();

        for (String topic : celldata.keySet()) {
            List<QAInfo> qas = new ArrayList<QAInfo>();
            for (List<String> row : celldata.get(topic)) {
                if (row.size() == 2) {
                    QAInfo q = new QAInfo(row.get(0), row.get(1), null, null);
                    qas.add(q);
                } else if (row.size() == 3) {
                    byte[] qpicdata = pics.get(Integer.parseInt(row.get(2))).getPictureData().getData();
                    QAInfo q = new QAInfo(row.get(0), row.get(1), qpicdata, null);
                    qas.add(q);
                } else if (row.size() == 4) {
                    Integer qindex = -1;
                    Integer aindex = -1;
                    try {
                        qindex = Integer.parseInt(row.get(2));
                    } catch (Exception e) {
                        continue;
                    }
                    try {
                        aindex = Integer.parseInt(row.get(3));
                    } catch (Exception e) {
                        continue;
                    }
                    byte[] qpicdata, apicdata;
                    qpicdata = apicdata = null;

                    if (qindex > 0) {
                        XSSFPicture pic = pics.get(qindex);
                        if (pic != null)
                            qpicdata = pic.getPictureData().getData();
                    }
                    if (aindex > 0) {
                        XSSFPicture pic = pics.get(aindex);
                        if (pic != null)
                            apicdata = pics.get(aindex).getPictureData().getData();
                    }
                    QAInfo q = new QAInfo(row.get(0), row.get(1), qpicdata, apicdata);
                    qas.add(q);
                } else {
                    //skip
                }
            }
            ret.put(topic, qas);
        }
        return ret;
    }

    public static Map<String, List<QAInfo>> getQAPairs(InputStream is, SpreadSheetFormat format) throws Exception{
        switch (format) {
            case XLSX:
                return getQAPairsXSSF(is);
            case XLS:
                return getQAPairsHSSF(is);
            case CSV:
                return getQAPairsCSV(is);
            default:
                return null;
        }
    }

    public static Map<String, List<List<String>>> readXlsx(XSSFWorkbook wb) {
        Map<String, List<List<String>>> ret = new HashMap<String, List<List<String>>>();

        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            List<List<String>> topic = new ArrayList<List<String>>();

            // Get first sheet from the workbook
            XSSFSheet sheet = wb.getSheetAt(i);

            Log.d("XLS", "Loaded sheet " + sheet.getSheetName());

            Row row;
            Cell cell;

            // Iterate through each rows from first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            while (rowIterator.hasNext()) {
                row = rowIterator.next();

                Iterator cells = row.cellIterator();
                List<String> n = new ArrayList<String>();

                while (cells.hasNext()) {
                    cell = (XSSFCell) cells.next();
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    n.add(cell.getStringCellValue());
                }
                topic.add(n);
            }

            ret.put(sheet.getSheetName(), topic);
            Log.d("XLS", String.valueOf(ret.size()) + " sheets loaded.");
        }
        return ret;
    }
}

