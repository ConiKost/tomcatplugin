/* The MIT License
 * (c) Copyright Sysdeo SA 2001-2002
 * (c) Copyright Eclipse Tomcat Plugin 2014-2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.sf.eclipse.tomcat;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/*
 * (c) Copyright Sysdeo SA 2001, 2002.
 * All Rights Reserved.
 */

import net.sf.eclipse.tomcat.editors.ProjectListEditor;


public class TomcatSourcePathPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, TomcatPluginResources {

	private ProjectListEditor projectListEditor;
	private BooleanFieldEditor automaticEditor;

	public TomcatSourcePathPreferencePage() {
		super();
		setPreferenceStore(TomcatLauncherPlugin.getDefault().getPreferenceStore());
	}

	@Override
    protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());

		// Group securityGroup = new Group(composite,SWT.NONE);
		automaticEditor = new BooleanFieldEditor(
			TomcatLauncherPlugin.TOMCAT_PREF_COMPUTESOURCEPATH_KEY,
			PREF_PAGE_COMPUTESOURCEPATH_LABEL,
			composite);
		this.initField(automaticEditor);

		final Group projectListGroup = new Group(composite, SWT.NULL);
		projectListGroup.setLayout(new GridLayout());
		projectListEditor = new ProjectListEditor();
		projectListEditor.setLabel(PREF_PAGE_PROJECTINSOURCEPATH_LABEL);
		final Control projectList = projectListEditor.getControl(projectListGroup);
		projectListGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		projectList.setLayoutData(new GridData(GridData.FILL_BOTH));

		projectListEditor.setCheckedElements(TomcatLauncherPlugin.getDefault().readProjectsInSourcePathFromPref());

		projectListEditor.setEnabled(!automaticEditor.getBooleanValue());
		//projectListEditor.setEnabled(false);
		automaticEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				projectListEditor.setEnabled(!automaticEditor.getBooleanValue());
			}
		});

		return composite;
	}

	public void init(IWorkbench workbench) {
	    // empty implementation
	}


	@Override
    public boolean performOk() {
		automaticEditor.store();
		TomcatLauncherPlugin.getDefault().setProjectsInSourcePath(projectListEditor.getCheckedElements());
		return true;
	}

	private void initField(FieldEditor field) {
		field.setPreferenceStore(getPreferenceStore());
		field.setPreferencePage(this);
		field.load();
	}
}

