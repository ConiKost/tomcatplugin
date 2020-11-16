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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * This class adds a selection list to the tomcat plugin project configuration.
 *
 * the selection is read and written to the file ".webclasspath" in the
 * root of the project.
 *
 * A special WebAppClassLoader implementation for tomcat 4.x loads the
 * generated file during startup of the webapplication.
 *
 * @version 	1.0
 * @author		Martin Kahr (martin.kahr@brainment.com)
 */
public class TomcatProjectWebclasspathPropertyPage {
	private static final String WEBAPP_CLASSPATH_FILENAME = ".webclasspath";
	private CheckedListDialogField cpList;
	private Button mavenClassPathCheck;
	private Button webClassPathCheck;
	private WebClassPathEntries entries;
	private ArrayList visitedProjects = new ArrayList();

	private TomcatProjectPropertyPage page;

	public TomcatProjectWebclasspathPropertyPage(TomcatProjectPropertyPage page) {
		this.page = page;
	}

	public IJavaProject getJavaProject() {
		try {
			return page.getJavaProject();
		} catch (CoreException e) {
			TomcatLauncherPlugin.log(e);
			return null;
		}
	}

	/** okay has been pressed */
	public boolean performOk() {
		List newSelection = cpList.getCheckedElements();

		try {
			page.getTomcatProject().setMavenClasspath(mavenClassPathCheck.getSelection());
			if (webClassPathCheck.getSelection()) {
				page.getTomcatProject().setWebClassPathEntries(new WebClassPathEntries(newSelection));
			} else {
				page.getTomcatProject().setWebClassPathEntries(null);
			}
			page.getTomcatProject().saveProperties();
		} catch(Exception ex) {
			TomcatLauncherPlugin.log(ex);
			return false;
		}

		return true;
	}

	public Control getControl(Composite ctrl) {
    boolean activated = isWebClasspathActive();

		Composite group   = new Composite(ctrl, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);

    mavenClassPathCheck = new Button(group, SWT.CHECK | SWT.LEFT);
    mavenClassPathCheck.setText(TomcatPluginResources.PROPERTIES_PAGE_PROJECT_ACTIVATE_MAVENCLASSPATH_LABEL);
    mavenClassPathCheck.setEnabled(true);
    mavenClassPathCheck.setSelection(isMavenClasspathActive());

    new Label(group, SWT.RIGHT).setText(" ");

		webClassPathCheck = new Button(group, SWT.CHECK | SWT.LEFT);
		webClassPathCheck.setText(TomcatPluginResources.PROPERTIES_PAGE_PROJECT_ACTIVATE_DEVLOADER_LABEL);
		webClassPathCheck.setEnabled(true);
		webClassPathCheck.setSelection(activated);
		webClassPathCheck.addSelectionListener(new SelectionAdapter() {
			@Override
      public void widgetSelected(SelectionEvent ev) {
				if (webClassPathCheck.getSelection()) {
					entries = new WebClassPathEntries();
					cpList.setEnabled(true);
				} else {
					entries = null;
					cpList.setEnabled(false);
				}
			}
		});

		cpList = new CheckedListDialogField(null, new String[]{"Check All", "Uncheck All"},new LabelProvider(){});
		cpList.setEnabled(activated);
		cpList.setCheckAllButtonIndex(0);
		cpList.setUncheckAllButtonIndex(1);
		ArrayList classPathEntries = new ArrayList();
		getClassPathEntries(getJavaProject(), classPathEntries);

		List selected = null;
		if (entries != null)
		{
			selected = entries.getList();
			// check for entries which are still in the list but no more in classpath entries list and remove them
			for (Iterator it = selected.iterator(); it.hasNext();) {
				String sel = (String) it.next();
				if (!classPathEntries.contains(sel))
				{
					it.remove();
				}
			}
		}


		// sort the entries
		Collections.sort(classPathEntries);

		/* Quick hack :
		 * Using reflection for compatability with Eclipse 2.1 and 3.0	M9
		 *
		 * Old code :
		 * 	cpList.setElements(classPathEntries);
		 * 	if (entries != null) {
		 *		cpList.setCheckedElements(entries.getList());
		 * 	}
		 */
		this.invokeForCompatibility("setElements", classPathEntries);
		if (entries != null) {
			this.invokeForCompatibility("setCheckedElements", entries.getList());
		}

		cpList.doFillIntoGrid(group, 3);
		LayoutUtil.setHorizontalGrabbing(cpList.getListControl(null));
		return group;
	}


	public void getClassPathEntries(IJavaProject prj, ArrayList data) {

		IClasspathEntry[] myEntries = null;
		IPath outputPath = null;
		try {
			outputPath = prj.getOutputLocation();
			add(data, prj.getOutputLocation());
			myEntries = prj.getRawClasspath();
		} catch(JavaModelException e) {
			TomcatLauncherPlugin.log(e);
		}
		if (myEntries != null) {
			getClassPathEntries(myEntries, prj, data, outputPath);
		}
	}


	private void getClassPathEntries(IClasspathEntry[] entries, IJavaProject prj, ArrayList data, IPath outputPath) {
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
				String prjName = entry.getPath().lastSegment();
				if(!visitedProjects.contains(prjName)) {
					visitedProjects.add(prjName);
					getClassPathEntries(getJavaProject().getJavaModel().getJavaProject(prjName), data);
				}
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					add(data, entry.getPath());
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath path = entry.getOutputLocation();
				if(path != null && !path.equals(outputPath))
				{
					add(data, path);
				}
			} else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
				if (!entry.getPath().toString().equals("org.eclipse.jdt.launching.JRE_CONTAINER")) {
					// Add container itself, as TomcatBootstrap can actually process them
					// at the moment
					// Basically, users will be able to choose b/w the whole container
					// or some artifacts enclosed by it
					add(data, entry.getPath());

					// Expand container and add its content as individual
					// elements
					IClasspathContainer container;
					try {
						container =
						JavaCore.getClasspathContainer(entry.getPath(), prj);
					} catch (JavaModelException e) {
						TomcatLauncherPlugin.log(
								"failed to obtain classpath container '" + entry.getPath() + "'" +
								" for project '" + prj.getProject().getName() + "'");
						TomcatLauncherPlugin.log(e);
						container = null;
					}

					if (container != null) {
						getClassPathEntries(container.getClasspathEntries(), prj, data, outputPath);
					}
				}
			} else {
				add(data, entry.getPath());
			}
		}
	}

	private void add(ArrayList data, IPath entry) {
		String path = entry.toFile().toString().replace('\\','/');
		// ignore tomcat's own libs and the JRE paths..
		if(!data.contains(path) && path.indexOf("TOMCAT_HOME") == -1 && path.indexOf("JRE_CONTAINER") == -1 && path.indexOf("JRE_LIB") == -1)
		{
			data.add(path.replace('\\','/'));
		}
	}

  private boolean isMavenClasspathActive() {
    //		entries = null;
		try {
			TomcatProject project = page.getTomcatProject();
      if (project != null) {
        return project.getMavenClasspath();
      }
		} catch(CoreException coreEx) {
        	// ignore exception
		}
    return false;
	}

  private boolean isWebClasspathActive() {
    entries = null;
    try {
      TomcatProject project = page.getTomcatProject();
      if (project == null) {
        return false;
      }
      entries = project.getWebClassPathEntries();
    } catch (CoreException coreEx) {
      // ignore exception
    }
    return (entries != null);
  }

	/* Quick hack :
	 * Using reflection for compatability with Eclipse 2.1 and 3.0	M9
	 */
	private void invokeForCompatibility(String methodName, List projects) {
		Class clazz = cpList.getClass();
		Class[] collectionParameter = {Collection.class};
		try {
			Method method = clazz.getMethod(methodName, collectionParameter);
			Object[] args = {projects};
			method.invoke(cpList, args);
		} catch (Exception e) {
			Class[] listParameter = {List.class};
			try {
				Method method = clazz.getMethod(methodName, listParameter);
				Object[] args = {projects};
				method.invoke(cpList, args);
			} catch (Exception ex) {
				TomcatLauncherPlugin.log(ex);
			}
		}

	}
}
