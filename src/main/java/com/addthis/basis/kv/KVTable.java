/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.basis.kv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class KVTable {
    List<String> cols = new LinkedList<>();
    List<KVPairs> data = new ArrayList<>();
    HashMap<String, String> colMap;

    public KVTable() {
        colMap = new HashMap<>();
    }

    public KVTable(HashMap<String, String> map) {
        colMap = map;
    }

    public void addRows(Iterator<KVPairs> iter, int count) {
        for (int i = 0; iter.hasNext() && (count == 0 || i < count); i++) {
            addRow(iter.next());
        }
    }

    public void addRow(KVPairs row) {
        for (Iterator<KVPair> e = row.elements(); e.hasNext();) {
            addColumn(e.next().getKey());
        }
        data.add(row);
    }

    public void addColumn(String col) {
        col = col.toLowerCase();
        if (!cols.contains(col)) {
            cols.add(col);
        }
    }

    public void clearData() {
        data.clear();
    }

    public Iterator<String> getColumns() {
        return cols.iterator();
    }

    public List<String> getColumnList() {
        return cols;
    }

    public List<String> getColumn(String key) {
        LinkedList<String> list = new LinkedList<>();
        for (Iterator<KVPairs> iter = getRows(); iter.hasNext();) {
            list.add(iter.next().getValue(key));
        }
        return list;
    }

    public int getColumnCount() {
        return cols.size();
    }

    public String getColumnKey(int pos) {
        return cols.get(pos);
    }

    public String getColumnName(String colKey) {
        String name = colMap.get(colKey);
        return name != null ? name : colKey;
    }

    public Iterator<KVPairs> getRows() {
        return data.iterator();
    }

    public List<KVPairs> getRowList() {
        return data;
    }

    public int getRowCount() {
        return data.size();
    }

    public String exportCSV() {
        return exportCSV(true);
    }

    public String exportCSV(boolean sortcols) {
        StringBuilder sb = new StringBuilder();
        LinkedList<String> columns = new LinkedList<>();
        for (Iterator<String> cols = getColumns(); cols.hasNext();) {
            columns.add(cols.next());
        }
        if (sortcols) {
            Collections.sort(columns);
        }
        for (Iterator<String> cols = columns.iterator(); cols.hasNext();) {
            sb.append(cols.next());
            if (cols.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("\n");
        for (Iterator<KVPairs> rows = getRows(); rows.hasNext();) {
            KVPairs row = rows.next();
            for (Iterator<String> cols = columns.iterator(); cols.hasNext();) {
                String col = cols.next();
                String val = row.getValue(col, "");
                sb.append(val.replace(',', ' '));
                if (cols.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public synchronized void pivot(String column) {
        List<KVPairs> nudata = new ArrayList<>();
        List<String> nucols = new ArrayList<>();
        nucols.add(column);
        boolean first = true;
        for (String col : cols) {
            if (col.equals(column)) {
                continue;
            }
            KVPairs nd = new KVPairs();
            for (KVPairs kv : data) {
                String colname = kv.getValue(column);
                nd.addValue(column, col);
                nd.addValue(colname, kv.getValue(col));
                if (first && !nucols.contains(colname.toLowerCase())) {
                    nucols.add(colname);
                }
            }
            first = false;
            nudata.add(nd);
        }
        data = nudata;
        cols = nucols;
    }
}
