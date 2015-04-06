package facsanadu.gui.view.tool;

import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.gui.QMouseEvent;

import facsanadu.gates.Gate;
import facsanadu.gates.GateRect;
import facsanadu.gui.events.EventGatesChanged;
import facsanadu.gui.events.EventGatesMoved;
import facsanadu.gui.view.ViewWidget;

/**
 * 
 * Tool to draw rectangle gates
 * 
 * @author Johan Henriksson
 *
 */
public class ViewToolDrawRect implements ViewTool
	{
	private Gate isDrawing=null;

	ViewWidget w;
	public ViewToolDrawRect(ViewWidget w)
		{
		this.w=w;
		}
	
	public void mouseReleaseEvent(QMouseEvent ev)
		{
		isDrawing=null;
		w.mw.handleEvent(new EventGatesMoved());
		}

	public void mouseMoveEvent(QMouseEvent event)
		{
		if(isDrawing!=null)
			{
			GateRect grect=(GateRect)isDrawing;
			
			QPointF p = w.trans.mapScreenToFacs(event.posF()); 
			
			grect.x2=p.x();
			grect.y2=p.y();
			grect.updateInternal();
			w.sendEvent(new EventGatesMoved());
			}
		}

	
	
	public void mousePressEvent(QMouseEvent event)
		{
		if(event.button()==MouseButton.LeftButton)
			{
			QPointF p = w.trans.mapScreenToFacs(event.posF()); 
			
			GateRect grect=new GateRect();
			grect.indexX=w.getIndexX();
			grect.indexY=w.getIndexY();
			grect.x1=grect.x2=p.x();
			grect.y1=grect.y2=p.y();
			grect.updateInternal();
			isDrawing=grect;

			w.mw.addGate(grect);
			w.sendEvent(new EventGatesChanged());
			}
		
		}

	@Override
	public void mouseDoubleClickEvent(QMouseEvent event)
		{
		}
	
	
	}