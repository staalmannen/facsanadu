package facsanadu.gui;


import io.qt.widgets.QDialog;
import io.qt.widgets.QLabel;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QTextEdit;
import io.qt.widgets.QVBoxLayout;

import facsanadu.gui.resource.ImgResource;

/**
 * 
 * The about dialog
 * 
 * @author Johan Henriksson
 *
 */
public class DialogAbout extends QDialog
	{
	public DialogAbout()
		{
		QTextEdit textedit=new QTextEdit(linebreaksAsBR(QtProgramInfo.licenseText));
		textedit.setReadOnly(true);

		QPushButton bOk=new QPushButton("OK");
		bOk.setDefault(true);
		
		QVBoxLayout vlayout=new QVBoxLayout();
		vlayout.addWidget(new QLabel(
				"<b>"+QtProgramInfo.programName+" "+"</b> "+tr("version")+" "+QtProgramInfo.getVersionString()+
        " by Johan Henriksson et al<br/>"));
		vlayout.addWidget(textedit);
		vlayout.addWidget(bOk);
		setLayout(vlayout);

		setMinimumSize(500, 300);
		
		setModal(true);
		setWindowTitle(QtProgramInfo.programName + " - "+ tr("About"));
		ImgResource.setWindowIcon(this);
		
		bOk.clicked.connect(this,"actionOK()");
		
		setVisible(true);
		}
	
	/**
	 * Action: OK
	 */
	public void actionOK()
		{
		close();
		}
	
	public static String linebreaksAsBR(String s)
		{
		return s.replace("\n","<br/>");
		}

	}
