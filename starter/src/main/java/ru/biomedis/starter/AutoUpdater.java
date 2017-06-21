package ru.biomedis.starter;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import org.anantacreative.updater.FilesUtil;
import org.anantacreative.updater.Pack.Exceptions.PackException;
import org.anantacreative.updater.Pack.Packer;
import org.anantacreative.updater.Update.AbstractUpdateTaskCreator;
import org.anantacreative.updater.Update.UpdateActionException;
import org.anantacreative.updater.Update.UpdateTask;
import org.anantacreative.updater.Update.XML.XmlUpdateTaskCreator;
import org.anantacreative.updater.Version;
import org.anantacreative.updater.VersionCheck.DefineActualVersionError;
import org.anantacreative.updater.VersionCheck.XML.XmlVersionChecker;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.biomedis.starter.BaseController.getApp;

/**
 * Автообновления
 */
public class AutoUpdater {
    private static AutoUpdater autoUpdater;
    private SimpleBooleanProperty processed = new SimpleBooleanProperty(false);
    private static UpdateTask updateTask;
    private File downloadDir;
    private SimpleBooleanProperty readyToInstall = new SimpleBooleanProperty(false);
    private File rootDirApp;


    private AutoUpdater() {

    }

    public File getRootDirApp() {
        return rootDirApp;
    }

    public synchronized static AutoUpdater getAutoUpdater() {
        if (autoUpdater == null) autoUpdater = new AutoUpdater();
        return autoUpdater;
    }

    public boolean isReadyToInstall() {
        return readyToInstall.get();
    }

    public SimpleBooleanProperty readyToInstallProperty() {
        return readyToInstall;
    }

    public void setReadyToInstall(boolean readyToInstall) {
        this.readyToInstall.set(readyToInstall);
    }

    public boolean isProcessed() {
        return processed.get();
    }

    private synchronized void setProcessed(boolean processed) {
        this.processed.set(processed);
    }

    public SimpleBooleanProperty processedProperty() {
        return processed;
    }

    private ResourceBundle getRes() {
        return App.getAppController().getApp().getResources();
    }

    public File getDownloadUpdateDir() {
        return downloadDir;
    }

    public XmlVersionChecker getVersionChecker(Version currentVersion) throws NotSupportedPlatformException, MalformedURLException {
        XmlVersionChecker versionChecker = new XmlVersionChecker(currentVersion,
                new URL(getUpdaterBaseUrl() + "/version.xml"));
        return versionChecker;
    }

    private String getUpdaterBaseUrl() throws NotSupportedPlatformException {
        String updaterBaseURL = "http://www.biomedis.ru/doc/b_mair/updater";
        if (OSValidator.isWindows()) updaterBaseURL = updaterBaseURL + "/win";
        else if (OSValidator.isMac()) updaterBaseURL = updaterBaseURL + "/mac";
        else if (OSValidator.isUnix()) updaterBaseURL = updaterBaseURL + "/linux";
        else throw new NotSupportedPlatformException();
        return updaterBaseURL;
    }

    public void startUpdater(boolean silentProcess) {
        if (isProcessed()) {

            Platform.runLater(() -> App.getAppController().showWarningDialog(
                    getRes().getString("app.update"),
                    getRes().getString("upgrade_process"),
                    "", App.getAppController().getApp().getMainWindow(), Modality.WINDOW_MODAL));

            return;
        }

        Thread thread = new Thread(() -> {
            System.out.println("startUpdater");


            rootDirApp = null;
            try {
                rootDirApp = defineRootDirApp();
                if (rootDirApp == null) throw new Exception();
                System.out.println(rootDirApp.getAbsolutePath());
            } catch (Exception e) {
                updateNotAvailableOnPlatformMessage();
            }


            setProcessed(true);
            setReadyToInstall(false);
            try {
                XmlVersionChecker versionChecker = getVersionChecker(App.getAppController().getApp().getVersion());
                if (versionChecker.checkNeedUpdate()) {

                    File dlDir = new File(App.getInnerDataDir_(),
                            "downloads" + File.separator + versionChecker.getActualVersion()
                                                                         .toString()
                                                                         .replace(".", "_"));
                    final XmlUpdateTaskCreator updater = new XmlUpdateTaskCreator(
                            FilesUtil.extractRelativePathFrom(rootDirApp, dlDir)
                            , rootDirApp, new AbstractUpdateTaskCreator.Listener() {
                        @Override
                        public void taskCompleted(UpdateTask updateTask, File rootDirApp, File downloadDir) {
                            System.out.println("Закачали обнову");
                            System.out.println(updateTask.toString());
                            AutoUpdater.updateTask = updateTask;
                            //if (!silentProcess) Platform.runLater(() -> App.getAppController().hideProgressBar(true));
                            toUpdate();


                        }

                        @Override
                        public void error(Exception e) {
                            setProcessed(false);
                            Log.logger.error("", e);
                            Platform.runLater(() -> App.getAppController().showErrorDialog(
                                    getRes().getString("app.update"),
                                    "",
                                    getRes().getString("get_update_error")
                                    , App.getAppController().getApp().getMainWindow(),
                                    Modality.WINDOW_MODAL));
                        }

                        @Override
                        public void completeFile(String s, File file) {
                            System.out.println("Закачан файл "+s);
                        }

                        @Override
                        public void currentFileProgress(float v) {

                        }

                        @Override
                        public void nextFileStartDownloading(String s, File file) {
                            System.out.println("Начат файл "+s);
                        }

                        @Override
                        public void totalProgress(float v) {
                           // if (!silentProcess) Platform.runLater(
                            //        () -> getController().setProgressBar(v,getRes().getString("downloading_files"),""));


                        }
                    }, new URL(getUpdaterBaseUrl() + "/update.xml"));

                    downloadDir = updater.getDownloadsDir();
                    updater.createTask(false);

                } else {
                    setProcessed(false);
                    if (!silentProcess) Platform.runLater(() -> App.getAppController().showWarningDialog(
                            getRes().getString("app.update"),
                            getRes().getString("updates_absent"),
                            "", App.getAppController().getApp().getMainWindow(),
                            Modality.WINDOW_MODAL));

                    return;

                }

            } catch (MalformedURLException e) {
                Platform.runLater(() -> App.getAppController().showErrorDialog(
                        getRes().getString("acces_to_update_server"),
                        getRes().getString("update_server_access_error"),
                        getRes().getString("yarovaya_is_here_censored"),
                        App.getAppController().getApp().getMainWindow(), Modality.WINDOW_MODAL));
                setProcessed(false);
            } catch (DefineActualVersionError e) {
                Platform.runLater(() -> App.getAppController().showErrorDialog(
                        getRes().getString("define_update_version"),
                        getRes().getString("data_retrieve_error"),
                        getRes().getString("yarovaya_is_here_censored"),
                        App.getAppController().getApp().getMainWindow(),
                        Modality.WINDOW_MODAL));
                setProcessed(false);

            } catch (AbstractUpdateTaskCreator.CreateUpdateTaskError e) {
                Platform.runLater(() -> App.getAppController().showErrorDialog(getRes().getString("app.update"), "",
                        getRes().getString("prepare_update_error"),
                        App.getAppController().getApp().getMainWindow(), Modality.WINDOW_MODAL));
                setProcessed(false);
            } catch (NotSupportedPlatformException e) {
                setProcessed(false);
                e.printStackTrace();
                updateNotAvailableOnPlatformMessage();

            }
        });

        thread.start();
    }

    public static boolean isIDEStarted() {
        File innerDataDir = App.getInnerDataDir_();
        File rootDir = new File(innerDataDir, "../");
        return rootDir.listFiles((dir, name) -> name.equals("pom.xml")).length == 1;
    }

    private File defineRootDirApp() throws Exception {
        File rootAppDir;
        if (isIDEStarted()) rootAppDir = new File("./");
        else {
            if (OSValidator.isWindows()) rootAppDir = new File(App.getInnerDataDir_(), "../../");
            else if (OSValidator.isMac())
                rootAppDir = new File(App.getInnerDataDir_(), "../../");//TODO: корректировать на MAC
            else if (OSValidator.isUnix()) rootAppDir = new File(App.getInnerDataDir_(), "../../");
            else throw new Exception();
        }
        return rootAppDir;
    }

    private void updateNotAvailableOnPlatformMessage() {
        try {
            App.disableAutoUpdate();
        } catch (Exception e) {
            Log.logger.error("", e);
        }
        Platform.runLater(() -> App.getAppController().showWarningDialog(
                getRes().getString("app.update"),
                getRes().getString("platform_updates_not_av"),
                ""
                , App.getAppController().getApp().getMainWindow(),
                Modality.WINDOW_MODAL));
    }


    private SimpleBooleanProperty isPerformAction = new SimpleBooleanProperty(false);

    public boolean isPerformAction() {
        return isPerformAction.get();
    }

    public SimpleBooleanProperty performActionProperty() {
        return isPerformAction;
    }

    private void setPerformAction(boolean performAction) {
        this.isPerformAction.set(performAction);
    }

    /**
     * Совершает обновление, асинхронно. Рестарт программы нужно делать самим уже после окончания обновления
     *
     * @return CompletableFuture
     */
    public CompletableFuture<Void> performUpdateTask() throws Exception {
        setReadyToInstall(false);
        if (isPerformAction()) throw new Exception();
        Platform.runLater(() -> Waiter.openLayer(App.getAppController().getApp().getMainWindow(), true));
        setPerformAction(true);
        CompletableFuture<Void> future = new CompletableFuture<>();

        CompletableFuture.runAsync(this::makeUpdateActions)
                         .thenAccept(v -> {
                             setProcessed(false);

                             FilesUtil.recursiveClear(getDownloadUpdateDir());
                             future.complete(null);
                             setPerformAction(false);
                             Platform.runLater(() -> Waiter.closeLayer());

                         })
                         .exceptionally(e -> {

                             setProcessed(false);
                             future.completeExceptionally(e);
                             setPerformAction(false);
                             Platform.runLater(() -> Waiter.closeLayer());
                             return null;
                         });

        return future;
    }

    private void makeUpdateActions() {
        if (updateTask != null) {
            setProcessed(true);
            try {

                updateTask.update();
                setProcessed(false);
            } catch (UpdateActionException e) {
                setProcessed(false);
                throw new RuntimeException(e);
            }

        }
    }

    private void toUpdate() {

        while (Waiter.isOpen()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                setProcessed(false);
                return;
            }
        }


        Platform.runLater(() -> {
            Optional<ButtonType> buttonType = App.getAppController()
                                                 .showConfirmationDialog(getRes().getString("app.update"),
                                                         getRes().getString("ready_for_update"),
                                                         getRes().getString("update_ask"),
                                                         App.getAppController().getApp().getMainWindow(),
                                                         Modality.APPLICATION_MODAL);
            if (buttonType.isPresent()) {
                if (buttonType.get() == App.getAppController().okButtonType) {

                    try {
                        onInstallUpdates();
                    } catch (Exception e) {
                        setProcessed(false);
                        setReadyToInstall(false);
                    }

                } else {
                    setProcessed(false);
                    setReadyToInstall(true);
                }
            } else {
                setProcessed(false);
                setReadyToInstall(true);
            }
        });
    }

    private AppController getController(){return App.getAppController();}


    //TODO доработать для Мак
    private void ZipDBToBackup() throws PackException {

        File rootDirApp = AutoUpdater.getAutoUpdater().getRootDirApp();
        File backupDir = new File(rootDirApp, "backup_db");
        if (!backupDir.exists()) backupDir.mkdir();
        File dbDir = null;
        if (AutoUpdater.isIDEStarted()) {
            dbDir = rootDirApp;
        } else if (OSValidator.isUnix()) {
            dbDir = new File(rootDirApp, "app");

        } else if (OSValidator.isWindows()) {
            dbDir = new File(rootDirApp, "assets");

        }

        Packer.packFiles(Stream.of(dbDir.listFiles((dir, name) -> name.endsWith(".db"))).collect(Collectors.toList()),
                new File(backupDir, Calendar.getInstance().getTimeInMillis() + ".zip"));


    }

    public void onInstallUpdates() throws Exception {
        App.getAppController().getApp().closePersisenceContext();
        try {
            ZipDBToBackup();
        } catch (PackException e) {
            Platform.runLater(() ->  {
                getController().showExceptionDialog(getRes().getString("app.update"),
                        getController().getApp().getResources().getString("backup_error"),
                        getRes().getString("process_updateing_stoped"),
                        e,
                        getApp().getMainWindow(), Modality.APPLICATION_MODAL);

            });

            throw new Exception();

        }finally {
            getApp().reopenPersistentContext();
        }
        try {
            AutoUpdater.getAutoUpdater().performUpdateTask().thenAccept(v -> {

                Platform.runLater(() ->  {
                    getController().showInfoDialog(getRes().getString("app.update"),
                            getRes().getString("all_files_copied"),
                            getRes().getString("complete_update"),
                            getApp().getMainWindow(), Modality.APPLICATION_MODAL);
                    restartProgram();
                });

            })
                       //.thenRun(this::restartProgram)
                       .exceptionally(e -> {

                           Exception ee;
                           if (e instanceof Exception) ee = (Exception) e;
                           else ee = new Exception(e);
                           Platform.runLater(() -> getController().showExceptionDialog(getRes().getString("app.update"),
                                   getRes().getString("processing_updating_files_error"), "", ee,
                                   getApp().getMainWindow(), Modality.APPLICATION_MODAL));

                           return null;
                       });
        } catch (Exception e) {

        }
    }

    private void restartProgram() {
/*
 Runtime.getRuntime().addShutdownHook(new Thread() {
    public void run() {
    ((Window) view).setVisible(false);
    Runtime.halt(0);
    }
    });
 */
        if(AutoUpdater.isIDEStarted()) return;

        try {
            File currentJar = new File(AppController.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if(!currentJar.getName().endsWith(".jar")) throw new Exception("Не найден путь к jar");

            //TODO Сделать для MacOs
            final List<String> command = new ArrayList<>();
            String exec="";
            if(OSValidator.isUnix()){
                exec = new File(currentJar.getParentFile(),"../BiomedisMAir4").getAbsolutePath();

            }else if(OSValidator.isWindows()){
                exec = new File(currentJar.getParentFile(),"../BiomedisMAir4.exe").getAbsolutePath();

            }else return;
            command.add(exec);


            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            //Platform.exit();
            System.out.println("restartProgram");
            System.exit(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    public void onCheckForUpdates(){
        startUpdater(false);
    }

    public static class NotSupportedPlatformException extends Exception {
    }
}
