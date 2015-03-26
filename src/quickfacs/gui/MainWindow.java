package quickfacs.gui;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import quickfacs.data.Dataset;
import quickfacs.gates.Gate;
import quickfacs.gui.events.EventGatesChanged;
import quickfacs.gui.events.EventGatesMoved;
import quickfacs.gui.events.EventViewsChanged;
import quickfacs.gui.events.QuickfacsEvent;
import quickfacs.gui.qt.QTutil;
import quickfacs.gui.view.GateViewsPane;
import quickfacs.gui.view.ViewSettings;
import quickfacs.io.FCSFile;
import quickfacs.io.QuickfacsXML;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.core.QModelIndex;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.ScrollBarPolicy;
import com.trolltech.qt.gui.QAbstractItemView.SelectionBehavior;
import com.trolltech.qt.gui.QAbstractItemView.SelectionMode;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QFileDialog;
import com.trolltech.qt.gui.QFileDialog.AcceptMode;
import com.trolltech.qt.gui.QFileDialog.FileMode;
import com.trolltech.qt.gui.QHBoxLayout;
import com.trolltech.qt.gui.QHeaderView.ResizeMode;
import com.trolltech.qt.gui.QLineEdit.EchoMode;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QMenu;
import com.trolltech.qt.gui.QMenuBar;
import com.trolltech.qt.gui.QPushButton;
import com.trolltech.qt.gui.QResizeEvent;
import com.trolltech.qt.gui.QScrollArea;
import com.trolltech.qt.gui.QSizePolicy.Policy;
import com.trolltech.qt.gui.QInputDialog;
import com.trolltech.qt.gui.QTabWidget;
import com.trolltech.qt.gui.QTableWidget;
import com.trolltech.qt.gui.QTableWidgetItem;
import com.trolltech.qt.gui.QTreeWidget;
import com.trolltech.qt.gui.QTreeWidgetItem;
import com.trolltech.qt.gui.QVBoxLayout;
import com.trolltech.qt.gui.QWidget;

/**
 * 
 * Main window for this software
 * 
 * @author Johan Henriksson
 *
 */
public class MainWindow extends QMainWindow
	{
	public QuickfacsProject project=new QuickfacsProject();
		
	private QTableWidget tableDatasets=new QTableWidget();
	private QTableWidget tableViews=new QTableWidget();	
	private QTreeWidget treeGates=new QTreeWidget();
	private QTabWidget tabwidget=new QTabWidget();
	private QMenuBar menubar=new QMenuBar();
	private GateViewsPane vpane;
	private QScrollArea scrollArea=new QScrollArea();
	private GateStatsPane paneStats;
	
	private File lastDirectory=new File(".");
	private File currentProjectFile=null;
	
	private boolean isUpdating=false;

	
	

	/**
	 * Constructor
	 */
	public MainWindow()
		{
		setMenuBar(menubar);

		vpane=new GateViewsPane(this);
		
		QMenu mFile=menubar.addMenu(tr("File"));
		mFile.addAction(tr("New project"), this, "actionNewProject()");
		mFile.addAction(tr("Open project"), this, "actionOpenProject()");
		mFile.addAction(tr("Save project"), this, "actionSaveProject()");
		mFile.addAction(tr("Save project as"), this, "actionSaveProjectAs()");
		mFile.addSeparator();
		mFile.addAction(tr("Add datasets"), this, "actionAddDatasets()");
		mFile.addSeparator();
		mFile.addAction(tr("Exit"), this, "close()");

		
		QMenu mExport=menubar.addMenu(tr("Export"));
		mExport.addAction(tr("Graphs as one file"));
		mExport.addAction(tr("Graphs by dataset"));
		mExport.addAction(tr("Graphs by view"));
		mExport.addAction(tr("Graphs as individual files"));
		mExport.addSeparator();
		mExport.addAction(tr("Statistics to CSV"));

		
		tableDatasets.setColumnCount(1);
		tableDatasets.verticalHeader().hide();
		tableDatasets.setHorizontalHeaderLabels(Arrays.asList(tr("Dataset")));
		tableDatasets.setSelectionBehavior(SelectionBehavior.SelectRows);
		tableDatasets.horizontalHeader().setResizeMode(ResizeMode.ResizeToContents);
		tableDatasets.horizontalHeader().setStretchLastSection(true);		
		tableDatasets.selectionModel().selectionChanged.connect(this,"dothelayout()");

		tableViews.verticalHeader().hide();
		tableViews.setSelectionBehavior(SelectionBehavior.SelectRows);
		tableViews.horizontalHeader().setResizeMode(ResizeMode.ResizeToContents);
		tableViews.horizontalHeader().setStretchLastSection(true);		
		tableViews.selectionModel().selectionChanged.connect(this,"dothelayout()");
	
		treeGates.setHeaderLabels(Arrays.asList(tr("Gate")));
		treeGates.setSelectionBehavior(SelectionBehavior.SelectRows);
		treeGates.setSelectionMode(SelectionMode.MultiSelection);
		treeGates.selectionModel().selectionChanged.connect(this,"dothelayout()");

		QPushButton bSelectAllDataset=new QPushButton(tr("Select all"));
		QPushButton bRemoveDataset=new QPushButton(tr("Remove dataset"));
		
		QPushButton bSelectAllGates=new QPushButton(tr("Select all"));
		QPushButton bRenameGate=new QPushButton(tr("Rename gate"));
		QPushButton bRemoveGate=new QPushButton(tr("Remove gate"));
		
		QPushButton bSelectAllViews=new QPushButton(tr("Select all"));
		QPushButton bNewView=new QPushButton(tr("New view"));
		QPushButton bRemoveView=new QPushButton(tr("Remove view"));


		bNewView.clicked.connect(this,"actionNewView()");
		bRemoveView.clicked.connect(this,"actionRemoveView()");

		bRenameGate.clicked.connect(this,"actionRenameGate()");

		bRemoveDataset.clicked.connect(this,"actionRemoveDataset()");
		bRemoveGate.clicked.connect(this,"actionRemoveGates()");

		bSelectAllDataset.clicked.connect(this,"actionSelectAllDataset()");
		bSelectAllViews.clicked.connect(this,"actionSelectAllViews()");
		bSelectAllGates.clicked.connect(this,"actionSelectAllGates()");

		QVBoxLayout layLeft=new QVBoxLayout();
		layLeft.addWidget(tableDatasets);
		layLeft.addLayout(QTutil.layoutHorizontal(bSelectAllDataset, bRemoveDataset));
		layLeft.addWidget(tableViews);
		layLeft.addLayout(QTutil.layoutHorizontal(bSelectAllViews, bNewView, bRemoveView));
		layLeft.addWidget(treeGates);
		layLeft.addLayout(QTutil.layoutHorizontal(bSelectAllGates, bRenameGate, bRemoveGate));

		actionNewView();

		treeGates.setSizePolicy(Policy.Minimum, Policy.Expanding);
		tableDatasets.setSizePolicy(Policy.Minimum, Policy.Expanding);
		tableViews.setSizePolicy(Policy.Minimum, Policy.Expanding);
		
		/// Load all files from directory
		File getfrom=new File("/home/mahogny/javaproj/quickfacs/test");
		if(getfrom.exists())
			for(File path:getfrom.listFiles())
				loadFile(path);


		scrollArea.setWidgetResizable(true);
		scrollArea.setWidget(vpane);
		
		scrollArea.setSizePolicy(Policy.Expanding, Policy.Expanding);
		scrollArea.setVerticalScrollBarPolicy(ScrollBarPolicy.ScrollBarAlwaysOn);
		
		paneStats=new GateStatsPane(this);
		
		tabwidget.addTab(scrollArea, tr("Graphs"));
		tabwidget.addTab(paneStats, tr("Statistics"));

		QHBoxLayout lay=new QHBoxLayout();
		lay.addLayout(layLeft);
		lay.addWidget(tabwidget);
		
		QWidget cent=new QWidget();
		cent.setLayout(lay);
		setCentralWidget(cent);

		

		updateViewsList();
		updateDatasetList();
		updateGatesList();
		dothelayout();

		show();
		}
	
	
	/**
	 * Action: New project
	 */
	public void actionNewProject()
		{
		project=new QuickfacsProject();
		updateall();
		}
	
	private void updateall()
		{
		boolean wasUpdating=isUpdating;
		isUpdating=true;
		updateViewsList();
		updateGatesList();
		updateDatasetList();
		isUpdating=wasUpdating;
		dothelayout();
		}
	

	public void actionOpenProject()
		{
		QFileDialog dia=new QFileDialog();
		dia.setFileMode(FileMode.ExistingFile);
		dia.setDirectory(lastDirectory.getAbsolutePath());
		dia.setNameFilter(tr("Project files (*.qfacs)"));
		if(dia.exec()!=0)
			{
			File f=new File(dia.selectedFiles().get(0));
			lastDirectory=f.getParentFile();
			try
				{
				project=QuickfacsXML.importXML(f);
				currentProjectFile=f;
				}
			catch (IOException e)
				{
				QTutil.showNotice(this, e.getMessage());
				e.printStackTrace();
				}
			updateall();
			}		
		}
	
	
	/**
	 * Action: Save project
	 */
	public void actionSaveProject()
		{
		if(currentProjectFile==null)
			actionSaveProjectAs();
		if(currentProjectFile!=null)
			try
				{
				QuickfacsXML.exportToFile(project, currentProjectFile);
				}
			catch (IOException e)
				{
				QTutil.showNotice(this, e.getMessage());
				e.printStackTrace();
				}
		}

	
	/**
	 * Action: Save as... file
	 */
	public void actionSaveProjectAs()
		{
		QFileDialog dia=new QFileDialog();
		dia.setFileMode(FileMode.AnyFile);
		dia.setAcceptMode(AcceptMode.AcceptSave);
		dia.setDirectory(lastDirectory.getAbsolutePath());
		dia.setDefaultSuffix("qfacs");
		dia.setNameFilter(tr("Project files (*.qfacs)"));
		if(dia.exec()!=0)
			{
			File f=new File(dia.selectedFiles().get(0));
			lastDirectory=f.getParentFile();
			currentProjectFile=f;
			actionSaveProject();
			}
		}

	
	/**
	 * Action: Remove selected datasets
	 */
	public void actionRemoveDataset()
		{
		project.datasets.removeAll(getSelectedDatasets());
		updateDatasetList();
		dothelayout();
		}

	
	public void actionSelectAllDataset()
		{
		tableDatasets.selectAll();
		}
	public void actionSelectAllViews()
		{
		tableViews.selectAll();
		}
	public void actionSelectAllGates()
		{
		treeGates.selectAll();
		}

	public void actionRenameGate()
		{
		Gate g=getCurrentGate();
		if(g!=null && g!=project.gateset.getRootGate())
			{
			String newname=QInputDialog.getText(this, tr("Rename gate"), tr("New name:"), EchoMode.Normal, g.name);
			if(!newname.equals("") && (newname.equals(g.name) || !project.gateset.getGateNames().contains(newname)))
				{
				g.name=newname;
				handleEvent(new EventGatesChanged());
				}
			else
				QTutil.showNotice(this, tr("Invalid name"));
			}
		
		}
	
	/**
	 * Action: Add/import datasets
	 */
	public void actionAddDatasets()
		{
		QFileDialog dia=new QFileDialog();
		dia.setFileMode(FileMode.ExistingFiles);
		dia.setDirectory(lastDirectory.getAbsolutePath());
		dia.setNameFilter(tr("FACS files (*.fcs)"));
		if(dia.exec()!=0)
			{
			try
				{
				for(String sf:dia.selectedFiles())
					{
					File f=new File(sf);
					lastDirectory=f.getParentFile();
					if(FCSFile.isFCSfile(f))
						loadFile(f);
					else
						QTutil.showNotice(this, "Not a FACS file: "+f);
					}
				}
			catch (IOException e)
				{
				QTutil.showNotice(this, e.getMessage());
				e.printStackTrace();
				}
			}		
		updateDatasetList();
		}
	
	
	/**
	 * Action: Remove selected gates
	 */
	public void actionRemoveGates()
		{
		Collection<Gate> gates=getSelectedGates();
		gates.remove(project.gateset.getRootGate());
		//Should include gates recursively!

		for(Gate g:gates)
			g.detachParent();

		for(ViewSettings vs:new LinkedList<ViewSettings>(project.views))
			if(gates.contains(vs.fromGate))
				project.views.remove(vs);
		updateGatesList();
		updateViewsList();
		dothelayout();
		}
	
	/**
	 * Add a new gate
	 */
	public void addGate(Gate g)
		{
		g.name=project.gateset.getFreeName();
		Gate parent=getCurrentGate();
		if(parent==null)
			parent=project.gateset.getRootGate();
		parent.attachChild(g);
		
		updateGatesList();
		}

	
	
	/**
	 * Get the currently selected gate or null
	 */
	private Gate getCurrentGate()
		{
		QTreeWidgetItem it=treeGates.currentItem();
		if(it!=null)
			return (Gate)it.data(0,Qt.ItemDataRole.UserRole);
		else
			return null;
		}



	/**
	 * Action: Remove selected views
	 */
	public void actionRemoveView()
		{
		project.views.removeAll(getSelectedViews());
		updateViewsList();
		}

	
	/**
	 * Action: Create a new view
	 */
	public void actionNewView()
		{
		ViewSettings vs=new ViewSettings();
		vs.fromGate=project.gateset.getRootGate();
		vs.indexX=0;
		vs.indexY=1;                                                    
		if(project.getNumChannels()>vs.indexX)
			vs.indexX=0;
		if(project.getNumChannels()>vs.indexY)
			vs.indexY=0;
		
		//autoscale here the first time?
		
		project.views.add(vs);
		updateViewsList();
		}
	
	
	
	/**
	 * Load one file
	 */
	public void loadFile(File path)
		{
		try
			{
			project.addDataset(path);
			updateDatasetList();
			}
		catch (IOException e)
			{
			e.printStackTrace();
			}
		}

	
	/**
	 * Get selected views
	 */
	public LinkedList<ViewSettings> getSelectedViews()
		{
		LinkedList<ViewSettings> selviews=new LinkedList<ViewSettings>();
		for(QModelIndex in:tableViews.selectionModel().selectedRows())
			selviews.add((ViewSettings)tableViews.item(in.row(),0).data(Qt.ItemDataRole.UserRole));
		return selviews;
		}

	/**
	 * Get selected datasets
	 */
	public LinkedList<Dataset> getSelectedDatasets()
		{
		LinkedList<Dataset> selviews=new LinkedList<Dataset>();
		for(QModelIndex in:tableDatasets.selectionModel().selectedRows())
			selviews.add((Dataset)tableDatasets.item(in.row(),0).data(Qt.ItemDataRole.UserRole));
		return selviews;
		}

	/**
	 * Get selected gates
	 */
	public LinkedList<Gate> getSelectedGates()
		{
		LinkedList<Gate> selviews=new LinkedList<Gate>();
		for(QTreeWidgetItem it:treeGates.selectedItems())
			selviews.add((Gate)it.data(0,Qt.ItemDataRole.UserRole));
		return selviews;
		}


	/**
	 * Update gating results
	 */
	public void dogating()
		{
		//For speed, only do selected ones
		project.dogating(getSelectedDatasets());
		}
	

	
	
	/**
	 * Update list with views
	 */
	private void updateViewsList()
		{
		boolean wasUpdating=isUpdating;
		isUpdating=true;
		tableViews.clear(); //clears title?
		
		tableViews.setColumnCount(1);
		tableViews.setHorizontalHeaderLabels(Arrays.asList(tr("View")));

		tableViews.setRowCount(project.views.size());
		int row=0;
		for(ViewSettings vs:project.views)
			{
			String showname=vs.fromGate.name+": ";
			if(!project.datasets.isEmpty())
				{
				Dataset ds=project.datasets.get(0);
				showname+=ds.getChannelInfo().get(vs.indexX).getShortestName()+" / "+ds.getChannelInfo().get(vs.indexY).getShortestName();
				}
			
			
			QTableWidgetItem it=QTutil.createReadOnlyItem(showname);
			it.setData(Qt.ItemDataRole.UserRole, vs);
			tableViews.setItem(row, 0, it);
			row++;
			}
		isUpdating=wasUpdating;
		}
	
	/**
	 * Update list with datasets
	 */
	private void updateDatasetList()
		{
		boolean wasUpdating=isUpdating;
		isUpdating=false;
		tableDatasets.setRowCount(project.datasets.size());
		int row=0;
		for(Dataset ds:project.datasets)
			{
			System.out.println(ds.source);
			QTableWidgetItem it=QTutil.createReadOnlyItem(ds.source.getName());
			it.setData(Qt.ItemDataRole.UserRole, ds);
			tableDatasets.setItem(row, 0, it);
			row++;
			}
		isUpdating=wasUpdating;
		}

	/**
	 * Update list with gates
	 */
	private void updateGatesList()
		{
		boolean wasUpdating=isUpdating;
		isUpdating=false;
		treeGates.clear();
		updateGatesListRecursive(null, project.gateset.getRootGate());
		treeGates.expandAll();
		isUpdating=wasUpdating;
		}
	private void updateGatesListRecursive(QTreeWidgetItem parentItem, Gate g)
		{
		QTreeWidgetItem item;
		if(parentItem==null)
			item=new QTreeWidgetItem(treeGates);
		else
			item=new QTreeWidgetItem(parentItem);
		item.setData(0,Qt.ItemDataRole.UserRole, g);
		item.setText(0, g.name);
		for(Gate child:g.children)
			updateGatesListRecursive(item, child);
		}

	
	/**
	 * Entry point
	 */
	public static void main(String[] args)
		{
		QApplication.initialize(QtProgramInfo.programName, args);
		QCoreApplication.setApplicationName(QtProgramInfo.programName);
		new MainWindow();
		QTutil.execStaticQApplication();		
		}


	/**
	 * Event bus
	 */
	public void handleEvent(QuickfacsEvent event)
		{
		if(event instanceof EventGatesChanged)
			{
			updateGatesList();
			dothelayout();
			}
		else if(event instanceof EventViewsChanged || event instanceof EventGatesMoved)
			{
			dothelayout();
			}
		else
			throw new RuntimeException("!!!");
		}

	
	@Override
	protected void resizeEvent(QResizeEvent e)
		{
		super.resizeEvent(e);
		dothelayout();
		}
	
	public void dothelayout()
		{
		if(!isUpdating)
			{
			dogating();
			vpane.updateViews();
			paneStats.updateStats();
			}
		}
	}
