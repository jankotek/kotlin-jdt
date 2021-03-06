package org.codehaus.groovy.eclipse.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.groovy.eclipse.core.GroovyCoreActivator;
import org.codehaus.groovy.eclipse.core.builder.GroovyClasspathContainerInitializer;
import org.codehaus.groovy.eclipse.core.compiler.CompilerCheckerParticipant;
import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.eclipse.core.preferences.PreferenceConstants;
import org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.groovy.core.Activator;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.actions.OpenWorkspaceAction;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

public class CompilerPreferencesPage extends PropertyAndPreferencePage implements
IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PROPERTY_ID = Activator.USING_PROJECT_PROPERTIES;

    public static final String PREFERENCES_ID = "org.codehaus.groovy.eclipse.preferences.compiler";

    private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$

    private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$

    private static final String PROP_REFRESH_BUNDLES = "-Declipse.refreshBundles=true";

    private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$

    private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

    private static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$

    private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$

    private static final String NEW_LINE = "\n"; //$NON-NLS-1$

    protected final SpecifiedVersion activeGroovyVersion;

    protected SpecifiedVersion currentProjectVersion;

    private Button groovyLibButt;

    private ScriptFolderSelectorPreferences scriptFolderSelector;

    private IEclipsePreferences preferences;

    private Combo compilerCombo;

    private Button doCheckForCompilerMismatch;

    public CompilerPreferencesPage() {
        super();
        activeGroovyVersion = CompilerUtils.getActiveGroovyVersion();
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        IProject project = getProject();
        ScopedPreferenceStore store;
        if (project == null) {
            // workspace settings
            IScopeContext scope = InstanceScope.INSTANCE;
            preferences = scope.getNode(Activator.PLUGIN_ID);
            store = new ScopedPreferenceStore(scope, Activator.PLUGIN_ID);
        } else {
            // project settings
            IScopeContext projectScope = new ProjectScope(project);
            preferences = projectScope.getNode(Activator.PLUGIN_ID);
            store = new ScopedPreferenceStore(projectScope, Activator.PLUGIN_ID);
        }
        return store;
    }

    public IEclipsePreferences getPreferences() {
        if (preferences == null) {
            doGetPreferenceStore();
        }
        return preferences;
    }


    @Override
    protected Label createDescriptionLabel(Composite parent) {
        if (isProjectPreferencePage()) {
            Composite body = new Composite(parent, SWT.NONE);
            GridLayout layout= new GridLayout();
            layout.marginHeight= 0;
            layout.marginWidth= 0;
            layout.numColumns = 2;
            body.setLayout(layout);
            body.setFont(parent.getFont());

            GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
            body.setLayoutData(data);
            createProjectCompilerSection(body);
        }
        return super.createDescriptionLabel(parent);
    }

    @Override
    protected Control createPreferenceContent(Composite parent) {
        final Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        page.setLayout(layout);
        page.setFont(parent.getFont());

        if (getElement() == null) {
            createWorkspaceCompilerSection(page);
        }

        // Groovy script folder
        scriptFolderSelector = new ScriptFolderSelectorPreferences(page, getPreferences(), getPreferenceStore());
        scriptFolderSelector.createListContents();

        if (getElement() == null) {
            // Only for the workspace version
            // Groovy classpath container
            createClasspathContainerSection(page);
        }

        return page;
    }

    /**
     * @param parent
     * @param page
     */
    protected void createClasspathContainerSection(final Composite page) {
        Label gccLabel = new Label(page, SWT.WRAP);
        gccLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        gccLabel.setText("Groovy Classpath Container:");
        gccLabel.setFont(getBoldFont(page));

        Composite gccPage = new Composite(page, SWT.NONE | SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        gccPage.setLayout(layout);
        gccPage.setFont(page.getFont());
        gccPage.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        groovyLibButt = new Button(gccPage, SWT.CHECK);
        groovyLibButt.setText("Include all jars in ~/.groovy/lib on the classpath.");
        groovyLibButt.setSelection(GroovyCoreActivator.getDefault().getPreference(PreferenceConstants.GROOVY_CLASSPATH_USE_GROOVY_LIB_GLOBAL, true));

        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.widthHint = 500;

        Label groovyLibLabel = new Label(gccPage, SWT.WRAP);
        groovyLibLabel.setText("This is the default setting and individual projects can be configured "
                + "by clicking on the properties page of the Groovy Support classpath container.");
        groovyLibLabel.setLayoutData(gd);

        Label classpathLabel = new Label(gccPage, SWT.WRAP);
        classpathLabel.setText("\nReset the Groovy Classpath Containers.");
        Button updateGCC = new Button(gccPage, SWT.PUSH);
        updateGCC.setText("Update all Groovy Classpath Containers");
        updateGCC.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                updateClasspathContainers();
            }
            public void widgetDefaultSelected(SelectionEvent e) {
                updateClasspathContainers();
            }
        });
        Label classpathLabel2 = new Label(gccPage, SWT.WRAP);
        classpathLabel2.setText("Perform this action if there are changes to ~/.groovy/lib "
                + "that should be reflected in your projects' classpaths.");
        classpathLabel2.setLayoutData(gd);
    }

    protected void createProjectCompilerSection(final Composite page) {
        Label compilerLabel = new Label(page, SWT.WRAP);
        compilerLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        compilerLabel.setText("Groovy compiler level for project " + getProject().getName() + ":");
        compilerLabel.setFont(getBoldFont(page));
        compilerCombo = new Combo(page, SWT.DROP_DOWN | SWT.READ_ONLY);
        compilerCombo.add("1.7");
        compilerCombo.add("1.8");
        compilerCombo.add("2.0");
        currentProjectVersion = CompilerUtils.getCompilerLevel(getProject());

        Label explainLabel = new Label(page, SWT.WRAP);
        explainLabel.setText("If the project compiler level does not match the workspace compiler level,\n" +
                "there will be a build error placed on the project.");
        GridData data = new GridData();
        data.horizontalSpan = 2;
        data.grabExcessHorizontalSpace = false;
        explainLabel.setLayoutData(data);
        setToProjectVersion();
    }

    private void setToProjectVersion() {
        switch (currentProjectVersion) {
            case _17:
                compilerCombo.select(0);
                break;
            case _18:
                compilerCombo.select(1);
                break;
            case _20:
                compilerCombo.select(2);
                break;
        }
    }

    protected void createWorkspaceCompilerSection(final Composite page) {
        Label compilerLabel = new Label(page, SWT.WRAP);
        compilerLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        compilerLabel.setText("Groovy Compiler settings:");
        compilerLabel.setFont(getBoldFont(page));

        Composite compilerPage = new Composite(page, SWT.NONE | SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        compilerPage.setLayout(layout);
        compilerPage.setFont(page.getFont());
        compilerPage.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Label compilerVersion = new Label(compilerPage, SWT.LEFT | SWT.WRAP);
        compilerVersion.setText("You are currently using Groovy Compiler version " + CompilerUtils.getGroovyVersion() + ".");

        if (activeGroovyVersion != SpecifiedVersion._17) {
            switchVersion(SpecifiedVersion._17, compilerPage);
        }
        if (activeGroovyVersion != SpecifiedVersion._18) {
            switchVersion(SpecifiedVersion._18, compilerPage);
        }
        if (activeGroovyVersion != SpecifiedVersion._20) {
            switchVersion(SpecifiedVersion._20, compilerPage);
        }

        Link moreInfoLink = new Link(compilerPage, 0);
        moreInfoLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
                false));
        moreInfoLink
        .setText("<a href=\"http://docs.codehaus.org/display/GROOVY/Compiler+Switching+within+Groovy-Eclipse\">See here</a> for more information "
                + "on compiler switching (opens a browser window).");
        moreInfoLink.addListener (SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                openUrl(event.text);
            }
        });

        doCheckForCompilerMismatch = new Button(compilerPage, SWT.CHECK);
        doCheckForCompilerMismatch.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        doCheckForCompilerMismatch.setText("Enable checking for mismatches between the project and workspace Groovy compiler levels");
        doCheckForCompilerMismatch.setSelection(getPreferences().getBoolean(Activator.GROOVY_CHECK_FOR_COMPILER_MISMATCH, true));
        //        doCheckForCompilerMismatch.addSelectionListener(new SelectionAdapter() {
        //            @Override
        //            public void widgetSelected(SelectionEvent e) {
        //                doCheckForCompilerMismatch.setSelection(!doCheckForCompilerMismatch.getSelection());
        //            }
        //        });
    }

    /**
     * @param toVersion
     */
    private void switchVersion(final SpecifiedVersion toVersion, final Composite compilerPage) {
        final BundleDescription toBundle = CompilerUtils.getBundleDescription(toVersion);
        if (toBundle == null) {
            // this version is not installed
            return;
        }

        Button switchTo = new Button(compilerPage, SWT.PUSH);
        switchTo.setText("Switch to " + toBundle.getVersion());
        switchTo.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                Shell shell = compilerPage.getShell();
                boolean result = MessageDialog.openQuestion(shell, "Change compiler and restart?",
                        "Do you want to change the compiler?\n\nIf you select \"Yes\"," +
                                " the compiler will be changed and Eclipse will be restarted.\n\n" +
                        "Make sure all your work is saved before clicking \"Yes\".");

                if (result) {
                    // change compiler
                    IStatus status = CompilerUtils.switchVersions(activeGroovyVersion, toVersion);
                    if (status == Status.OK_STATUS) {
                        restart(shell);
                    } else {
                        ErrorDialog error = new ErrorDialog(shell,
                                "Error occurred", "Error occurred when trying to enable Groovy " +
                                        toBundle.getVersion(),
                                        status, IStatus.ERROR);
                        error.open();
                    }
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {}
        });
    }

    /**
     * @param page
     * @return
     */
    private Font getBoldFont(Composite page) {
        return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
    }

    /**
     * borrowed from {@link OpenWorkspaceAction}
     */
    protected void restart(Shell shell) {
        String command_line = buildCommandLine(shell);
        if (command_line == null) {
            return;
        }

        System.out.println("Restart command line begin:\n " + command_line);
        System.out.println("Restart command line end");
        System.setProperty(PROP_EXIT_DATA, command_line);
        System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
        Workbench.getInstance().restart();

    }

    /**
     * Create and return a string with command line options for eclipse.exe that
     * will launch a new workbench that is the same as the currently running
     * one, but using the argument directory as its workspace.
     *
     * @param workspace
     *            the directory to use as the new workspace
     * @return a string of command line options or null on error
     */
    private String buildCommandLine(Shell shell) {
        String property = FrameworkProperties.getProperty(PROP_VM);
        if (property == null) {
            MessageDialog
            .openError(
                    shell,
                    IDEWorkbenchMessages.OpenWorkspaceAction_errorTitle,
                    NLS.bind(IDEWorkbenchMessages.OpenWorkspaceAction_errorMessage,
                            PROP_VM));
            return null;
        }

        StringBuffer result = new StringBuffer(512);
        result.append(property);
        result.append(NEW_LINE);

        // append the vmargs and commands. Assume that these already end in \n
        String vmargs = System.getProperty(PROP_VMARGS);
        vmargs = vmargs == null ?
                PROP_REFRESH_BUNDLES + NEW_LINE :
                    vmargs + NEW_LINE + PROP_REFRESH_BUNDLES + NEW_LINE;
        result.append(vmargs);

        // append the rest of the args, replacing or adding -data as required
        property = System.getProperty(PROP_COMMANDS);
        if (property != null) {
            result.append(property);
        }

        // put the vmargs back at the very end (the eclipse.commands property
        // already contains the -vm arg)
        if (vmargs != null) {
            result.append(CMD_VMARGS);
            result.append(NEW_LINE);
            result.append(vmargs);
        }

        return result.toString();
    }

    @Override
    public void init(IWorkbench workbench) {}

    public static void openUrl(String location) {
        try {
            URL url = null;

            if (location != null) {
                url = new URL(location);
            }

            if (WebBrowserPreference.getBrowserChoice() == WebBrowserPreference.EXTERNAL) {
                try {
                    IWorkbenchBrowserSupport support = PlatformUI
                            .getWorkbench().getBrowserSupport();
                    support.getExternalBrowser().openURL(url);
                } catch (Exception e) {
                    GroovyCore.logException("Could not open browser", e);
                }
            } else {
                IWebBrowser browser = null;
                int flags = 0;
                if (WorkbenchBrowserSupport.getInstance()
                        .isInternalWebBrowserAvailable()) {
                    flags |= IWorkbenchBrowserSupport.AS_EDITOR
                            | IWorkbenchBrowserSupport.LOCATION_BAR
                            | IWorkbenchBrowserSupport.NAVIGATION_BAR;
                } else {
                    flags |= IWorkbenchBrowserSupport.AS_EXTERNAL
                            | IWorkbenchBrowserSupport.LOCATION_BAR
                            | IWorkbenchBrowserSupport.NAVIGATION_BAR;
                }

                String id = "org.eclipse.contribution.weaving.jdt";
                browser = WorkbenchBrowserSupport.getInstance().createBrowser(
                        flags, id, null, null);
                browser.openURL(url);
            }
        } catch (PartInitException e) {
            MessageDialog.openError(Display.getDefault().getActiveShell(),
                    "Browser initialization error",
                    "Browser could not be initiated");
        } catch (MalformedURLException e) {
            MessageDialog.openInformation(Display.getDefault()
                    .getActiveShell(), "Malformed URL",
                    location);
        }
    }

    private void updateClasspathContainers() {
        try {
            GroovyClasspathContainerInitializer.updateAllGroovyClasspathContainers();
        } catch (JavaModelException e) {
            GroovyCore.logException("Problem updating Groovy classpath contianers", e);
        }
    }

    @Override
    public boolean performOk() {
        applyPreferences();
        return super.performOk();
    }
    @Override
    public void performApply() {
        applyPreferences();
        super.performApply();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        if (getProject() == null) {
            GroovyCoreActivator.getDefault().setPreference(PreferenceConstants.GROOVY_CLASSPATH_USE_GROOVY_LIB_GLOBAL, true);
        } else {
            enableProjectSpecificSettings(false);
        }
        scriptFolderSelector.restoreDefaultsPressed();
        if (compilerCombo != null) {
            setToProjectVersion();
        }

        if (doCheckForCompilerMismatch != null) {
            doCheckForCompilerMismatch.setSelection(true);
            getPreferences().putBoolean(Activator.GROOVY_CHECK_FOR_COMPILER_MISMATCH, true);
        }
    }

    private void applyPreferences() {
        if (getProject() == null) {
            GroovyCoreActivator.getDefault().setPreference(PreferenceConstants.GROOVY_CLASSPATH_USE_GROOVY_LIB_GLOBAL, groovyLibButt.getSelection());
        } else {
            getPreferenceStore().setValue(PROPERTY_ID, useProjectSettings());
        }
        scriptFolderSelector.applyPreferences();

        if (doCheckForCompilerMismatch != null) {
            boolean isSelected = doCheckForCompilerMismatch.getSelection();
            boolean currentPref = getPreferences().getBoolean(Activator.GROOVY_CHECK_FOR_COMPILER_MISMATCH, true);
            if (!isSelected && currentPref) {
                // delete all markers in the workspace
                try {
                    ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(CompilerCheckerParticipant.COMPILER_MISMATCH_PROBLEM, true, IResource.DEPTH_ONE);
                } catch (CoreException e) {
                    GroovyCore.logException("Error deleting markers", e);
                }
            }
            if (isSelected != currentPref) {
                getPreferences().putBoolean(Activator.GROOVY_CHECK_FOR_COMPILER_MISMATCH, isSelected);
                try {
                    getPreferences().flush();
                } catch (BackingStoreException e) {
                    GroovyCore.logException("Error saving compiler preferences", e);
                }
            }
        }

        if (compilerCombo != null) {
            int selectedIndex = compilerCombo.getSelectionIndex();
            SpecifiedVersion selected;
            switch (selectedIndex) {
                case 0:
                    selected = SpecifiedVersion._17;
                    break;
                case 1:
                    selected = SpecifiedVersion._18;
                    break;
                case 2:
                    selected = SpecifiedVersion._20;
                    break;
                default:
                    selected = SpecifiedVersion.UNSPECIFIED;
            }
            if (selected != currentProjectVersion && selected != SpecifiedVersion.UNSPECIFIED) {
                CompilerUtils.setCompilerLevel(getProject(), selected);
            }
        }
    }

    @Override
    protected boolean hasProjectSpecificOptions(IProject project) {
        if (project != null && project.equals(getProject())) {
            return getPreferenceStore().getBoolean(PROPERTY_ID);
        }
        return false;
    }

    @Override
    protected String getPreferencePageID() {
        return PREFERENCES_ID;
    }

    @Override
    protected String getPropertyPageID() {
        return PROPERTY_ID;
    }

}
