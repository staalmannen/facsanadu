package facsanadu.gui.panes;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import io.qt.core.Qt.ItemFlag;
import io.qt.core.Qt.ItemFlags;
import io.qt.widgets.QTableWidgetItem;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import io.qt.widgets.QHeaderView.ResizeMode;
import io.qt.widgets.QTableWidget;

import facsanadu.data.Dataset;
import facsanadu.gui.MainWindow;

/**
 * 
 * Pane showing metadata about a dataset
 * 
 * @author Johan Henriksson
 *
 */
public class DatasetInfoPane extends QWidget
	{
	private QTableWidget tableMatrix=new QTableWidget();
	private MainWindow mw;
	private boolean updating=false;
		
	public DatasetInfoPane(MainWindow mw)
		{
		this.mw=mw;		

		updateForm();
		
		QVBoxLayout lay=new QVBoxLayout();
		lay.addWidget(tableMatrix);
	//	lay.addStretch();
		lay.setContentsMargins(0,0,0,0);
		setLayout(lay);
		updateForm();
		}
	
	
	public void updateForm()
		{
		updating=true;
		
		tableMatrix.clear();
		tableMatrix.verticalHeader().hide();
		
		//Columns
		LinkedList<String> header=new LinkedList<String>();
		header.add("Key");
		header.add("Value");
		tableMatrix.setColumnCount(2);
		tableMatrix.setHorizontalHeaderLabels(header);
		
		if(mw.getSelectedDatasets().size()==1)
			{
			Dataset ds=mw.getSelectedDatasets().getFirst();
			TreeMap<String, String> metaKeyName=new TreeMap<String, String>(ds.metaKeyName);
			metaKeyName.put("# observations", ""+ds.getNumObservations());
			
			
			ArrayList<String> keyw=new ArrayList<String>(metaKeyName.keySet());
			tableMatrix.setRowCount(keyw.size());

			for(int i=0;i<keyw.size();i++)
				{
				QTableWidgetItem it=new QTableWidgetItem(""+keyw.get(i));
				it.setFlags(new ItemFlags(ItemFlag.ItemIsSelectable, ItemFlag.ItemIsEnabled));
				tableMatrix.setItem(i, 0, it);

				it=new QTableWidgetItem(""+metaKeyName.get(keyw.get(i)));
				it.setFlags(new ItemFlags(ItemFlag.ItemIsSelectable, ItemFlag.ItemIsEnabled));
				tableMatrix.setItem(i, 1, it);
				}
			}
		else
			{
			tableMatrix.setRowCount(0);
			}
		
		tableMatrix.horizontalHeader().setSectionResizeMode(ResizeMode.ResizeToContents);
		tableMatrix.horizontalHeader().setStretchLastSection(true);		
		tableMatrix.resizeColumnsToContents();
    
		tableMatrix.itemChanged.connect(this,"dataChanged(QTableWidgetItem)");
		updating=false;
		}
	
	
	public void dataChanged(QTableWidgetItem it)
		{
		if(!updating)
			{
			}
		}
	}
