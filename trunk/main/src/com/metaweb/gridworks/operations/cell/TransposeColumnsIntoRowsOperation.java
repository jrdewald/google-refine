package com.metaweb.gridworks.operations.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.MassRowColumnChange;
import com.metaweb.gridworks.operations.OperationRegistry;

public class TransposeColumnsIntoRowsOperation extends AbstractOperation {
    final protected String  _startColumnName;
    final protected int     _columnCount;
    final protected String  _combinedColumnName;
    final protected boolean _prependColumnName;
    final protected String  _separator;
    final protected boolean _ignoreBlankCells;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new TransposeColumnsIntoRowsOperation(
            obj.getString("startColumnName"),
            obj.getInt("columnCount"),
            obj.getString("combinedColumnName"),
            obj.getBoolean("prependColumnName"),
            obj.getString("separator"),
            obj.getBoolean("ignoreBlankCells")
        );
    }
    
    public TransposeColumnsIntoRowsOperation(
        String  startColumnName,
        int     columnCount,
        String  combinedColumnName,
        boolean prependColumnName,
        String  separator,
        boolean ignoreBlankCells
    ) {
        _startColumnName = startColumnName;
        _columnCount = columnCount;
        _combinedColumnName = combinedColumnName;
        _prependColumnName = prependColumnName;
        _separator = separator;
        _ignoreBlankCells = ignoreBlankCells;
    }

   public void write(JSONWriter writer, Properties options)
           throws JSONException {
       
       writer.object();
       writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
       writer.key("description"); writer.value("Transpose cells in " + _columnCount + " column(s) starting with " + _startColumnName + " into rows");
       writer.key("startColumnName"); writer.value(_startColumnName);
       writer.key("columnCount"); writer.value(_columnCount);
       writer.key("combinedColumnName"); writer.value(_combinedColumnName);
       writer.key("prependColumnName"); writer.value(_prependColumnName);
       writer.key("separator"); writer.value(_separator);
       writer.key("ignoreBlankCells"); writer.value(_ignoreBlankCells);
       writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Transpose cells in " + _columnCount + " column(s) starting with " + _startColumnName + " into rows";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        List<Column> newColumns = new ArrayList<Column>();
        List<Column> oldColumns = project.columnModel.columns;
        
        int columnsLeftToTranspose = _columnCount;
        int startColumnIndex = oldColumns.size();
        for (int c = 0; c < oldColumns.size(); c++) {
            Column column = oldColumns.get(c);
            if (columnsLeftToTranspose == 0) {
                // This column is beyond the columns to transpose
                
                Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                newColumn.setName(column.getName());
                
                newColumns.add(newColumn);
            } else if (columnsLeftToTranspose < _columnCount) {
                // This column is a column to transpose, but not the first
                // nothing to do
                
                columnsLeftToTranspose--;
            } else if (_startColumnName.equals(column.getName())) {
                // This is the first column to transpose
                
                startColumnIndex = c;
                
                String columnName = _combinedColumnName != null && _combinedColumnName.length() > 0 ? _combinedColumnName : column.getName();
                Column newColumn = new Column(newColumns.size(), columnName);
                
                newColumns.add(newColumn);
                
                columnsLeftToTranspose--;
            } else {
                // This column is before all columns to transpose
                
                Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                newColumn.setName(column.getName());
                
                newColumns.add(newColumn);
            }
        }
        
        
        List<Row> oldRows = project.rows;
        List<Row> newRows = new ArrayList<Row>(oldRows.size() * _columnCount);
        for (int r = 0; r < oldRows.size(); r++) {
            Row oldRow = project.rows.get(r);
            Row firstNewRow = new Row(newColumns.size());
            
            newRows.add(firstNewRow);
            
            int transposedCells = 0;
            for (int c = 0; c < oldColumns.size(); c++) {
                Column column = oldColumns.get(c);
                Cell cell = oldRow.getCell(column.getCellIndex());
                
                if (c < startColumnIndex) {
                    firstNewRow.setCell(c, cell);
                } else if (c == startColumnIndex || c < startColumnIndex + _columnCount) {
                    Cell newCell;
                    
                    if (cell == null || cell.value == null) {
                        if (_prependColumnName && !_ignoreBlankCells) {
                            newCell = new Cell(column.getName() + _separator, null);
                        } else {
                            continue;
                        }
                    } else if (_prependColumnName) {
                        newCell = new Cell(column.getName() + _separator + cell.value, null);
                    } else {
                        newCell = cell;
                    }
                    
                    if (transposedCells == 0) {
                        firstNewRow.setCell(startColumnIndex, newCell);
                    } else {
                        Row newRow = new Row(newColumns.size());
                        
                        newRow.setCell(startColumnIndex, newCell);
                        newRows.add(newRow);
                    }
                    
                    transposedCells++;
                } else {
                    firstNewRow.setCell(c - _columnCount + 1, cell);
                }
            }
        }
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            getBriefDescription(null), 
            this, 
            new MassRowColumnChange(newColumns, newRows)
        );
    }
}
