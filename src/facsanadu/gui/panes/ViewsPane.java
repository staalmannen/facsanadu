package facsanadu.gui.panes;

import io.qt.core.Qt.ScrollBarPolicy;
import io.qt.widgets.QButtonGroup;
import io.qt.widgets.QCheckBox;
import io.qt.widgets.QHBoxLayout;
import io.qt.gui.QIcon;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QSpinBox;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import io.qt.widgets.QSizePolicy.Policy;

import facsanadu.gui.MainWindow;
import facsanadu.gui.resource.ImgResource;
import facsanadu.gui.view.tool.ViewToolChoice;

/**
 * 
 * Pane showing all the views
 * 
 * @author Johan Henriksson
 *
 */
public class ViewsPane extends QWidget
	{
	private QSpinBox spMaxEvents=new QSpinBox();
	private QCheckBox cbMaxEvents=new QCheckBox(tr("Show max events:"));

	private ViewsMatrix matrix;
	private QButtonGroup bgroup=new QButtonGroup(this);

	private QPushButton bGateSelect=new QPushButton(new QIcon(ImgResource.gateSelect),"");
	private QPushButton bGatePoly=new QPushButton(new QIcon(ImgResource.gatePolygon),"");
	private QPushButton bGateRect=new QPushButton(new QIcon(ImgResource.gateRect),"");
	private QPushButton bGateEllipse=new QPushButton(new QIcon(ImgResource.gateEllipse),"");
	private QPushButton bGateRange=new QPushButton(new QIcon(ImgResource.gateRange),"");
	QPushButton[] tb=new QPushButton[]{bGateSelect, bGatePoly, bGateRect, bGateEllipse, bGateRange};

	public ViewsPane(MainWindow mw)
		{
		matrix=new ViewsMatrix(mw);
		
		for(QPushButton t:tb)
			bgroup.addButton(t);
		for(QPushButton t:tb)
			t.setCheckable(true);
		bGateSelect.setChecked(true);
		for(QPushButton t:tb)
			t.toggled.connect(this,"actionSetTool()");
		
		spMaxEvents.setMinimum(100);
		spMaxEvents.setMaximum(10000000);
		spMaxEvents.setValue(100000);
		cbMaxEvents.setChecked(true);
		
		QHBoxLayout laytop=new QHBoxLayout();
		for(QPushButton t:tb)
			laytop.addWidget(t);
		laytop.addStretch();
		laytop.addWidget(cbMaxEvents);
		laytop.addWidget(spMaxEvents);
		laytop.setContentsMargins(2,2,2,2);
		laytop.setSpacing(2);		
		
		QScrollArea scrollArea=new QScrollArea();
		scrollArea.setWidgetResizable(true);
		scrollArea.setWidget(matrix);
		scrollArea.setSizePolicy(Policy.Expanding, Policy.Expanding);
		scrollArea.setVerticalScrollBarPolicy(ScrollBarPolicy.ScrollBarAlwaysOn);

		QVBoxLayout lay=new QVBoxLayout();
		lay.addLayout(laytop);
		lay.addWidget(scrollArea);
		lay.setContentsMargins(0,0,0,0);
		lay.setSpacing(2);
		setLayout(lay);
		
		spMaxEvents.valueChanged.connect(this,"valuesupdated()");
		cbMaxEvents.stateChanged.connect(this,"valuesupdated()");
		valuesupdated();
		}

	public void updateViews()
		{
		matrix.updateViews();
		}

	public void valuesupdated()
		{
		int maxevents=Integer.MAX_VALUE;
		if(cbMaxEvents.isChecked())
			maxevents=spMaxEvents.value();
		matrix.setMaxEvents(maxevents);
		}

	public void actionSetTool()
		{
		if(bgroup.checkedButton()==bGatePoly)
			setTool(ViewToolChoice.POLY);
		else if(bgroup.checkedButton()==bGateSelect)
			setTool(ViewToolChoice.SELECT);
		else if(bgroup.checkedButton()==bGateRect)
			setTool(ViewToolChoice.RECT);
		else if(bgroup.checkedButton()==bGateEllipse)
			setTool(ViewToolChoice.ELLIPSE);
		else if(bgroup.checkedButton()==bGateRange)
			setTool(ViewToolChoice.RANGE);
		}

	public void setTool(ViewToolChoice t)
		{
		if(t==ViewToolChoice.SELECT)
			bGateSelect.setChecked(true);
		else if(t==ViewToolChoice.POLY)
			bGatePoly.setChecked(true);
		else if(t==ViewToolChoice.RECT)
			bGateRect.setChecked(true);
		matrix.setTool(t);
		}

	
	public void invalidateCache()
		{
		matrix.invalidateCache();
		}
	}
