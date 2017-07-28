package ru.biomedis.biomedismair3.TherapyTabs.Complex;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import ru.biomedis.biomedismair3.App;
import ru.biomedis.biomedismair3.CellFactories.TextAreaTableCell;
import ru.biomedis.biomedismair3.ModelDataApp;
import ru.biomedis.biomedismair3.TherapyTabs.Profile.ProfileTable;
import ru.biomedis.biomedismair3.entity.TherapyComplex;
import ru.biomedis.biomedismair3.utils.Date.DateUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.biomedis.biomedismair3.BaseController.getApp;
import static ru.biomedis.biomedismair3.Log.logger;
import static ru.biomedis.biomedismair3.TherapyTabs.TablesCommon.isEnablePaste;

public class ComplexTable {
    private  ResourceBundle res;
    private  Image imageCancel;
    private  Image imageDone;
    private TableView<TherapyComplex> table;
    private static ComplexTable instance;
    private SimpleStringProperty textComplexTime=new SimpleStringProperty();//хранит время комплекса в строковом представлении и через # id комплекса. Используется в выводе в табе комплекса времени и его обновления

    public SimpleStringProperty textComplexTimeProperty() {
        return textComplexTime;
    }

    public static final DataFormat COMPLEX_CUT_ITEM_INDEX =new DataFormat("biomedis/cut_complex_item_index");
    public static final DataFormat COMPLEX_CUT_ITEM_ID=new DataFormat("biomedis/cut_complex_item_id");
    public static final DataFormat COMPLEX_CUT_ITEM_PROFILE =new DataFormat("biomedis/cut_complex_profile");
    public static final DataFormat COMPLEX_COPY_ITEM=new DataFormat("biomedis/copy_complexitem");

    private ContextMenu  complexesMenu=new ContextMenu();

    public static ComplexTable init(TableView<TherapyComplex> tableComplex, ResourceBundle res, Image imageCancel, Image imageDone){

        if(instance==null){
            instance =new ComplexTable(tableComplex,res,imageCancel,imageDone);
            instance.initTable();
        }
        return instance;
    }

    public static ComplexTable getInstance(){

        if(instance==null){
           return null;
        }else return  instance;
    }
    private ComplexTable() {
    }

    private ComplexTable(TableView<TherapyComplex> tableComplex, ResourceBundle res,Image imageCancel, Image imageDone) {
        this.table = tableComplex;
        this.res = res;

        this.imageCancel = imageCancel;
        this.imageDone = imageDone;
    }

    private void initTable(){
        //номер по порядку
        TableColumn<TherapyComplex,Number> numComplexCol =new TableColumn<>("№");
        numComplexCol.setCellValueFactory(param -> new SimpleIntegerProperty(param.getTableView().getItems().indexOf(param.getValue()) + 1));


        //имя
        TableColumn<TherapyComplex,String> nameColTC=new TableColumn<>(res.getString("app.table.name_complex"));
        nameColTC.cellValueFactoryProperty().setValue(new PropertyValueFactory<TherapyComplex, String>("name"));
        nameColTC.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColTC.setOnEditCommit(event ->
        {

            if (!event.getNewValue().equals(event.getOldValue())) {

                String s = event.getNewValue();
                if (s.length() == 0) {
                    event.getRowValue().setName(event.getOldValue());
                    TherapyComplex p = event.getRowValue();
                    int i = table.getItems().indexOf(event.getRowValue());
                    table.getItems().set(i, null);
                    table.getItems().set(i, p);
                    table.getSelectionModel().select(i);
                    p = null;
                    return;
                }
                event.getRowValue().setName(s);
                try {
                    getModel().updateTherapyComplex(event.getRowValue());
                    TherapyComplex p = event.getRowValue();
                    int i = table.getItems().indexOf(event.getRowValue());
                    table.getItems().set(i, null);
                    table.getItems().set(i, p);
                    table.getSelectionModel().select(i);
                    p = null;

                } catch (Exception e) {
                    logger.error("",e);
                }


            }
        });

        //описание
        TableColumn<TherapyComplex,String> descColTC=new TableColumn<>(res.getString("app.table.complex_descr"));
        descColTC.cellValueFactoryProperty().setValue(new PropertyValueFactory<TherapyComplex, String>("description"));
        descColTC.setCellFactory(TextAreaTableCell.forTableColumn());
        descColTC.setOnEditCommit(event ->
        {

            if (!event.getNewValue().equals(event.getOldValue())) {

                String s = event.getNewValue();
                if (s.length() == 0) {
                    event.getRowValue().setDescription(event.getOldValue());
                    TherapyComplex p = event.getRowValue();
                    int i = table.getItems().indexOf(event.getRowValue());
                    table.getItems().set(i, null);
                    table.getItems().set(i, p);
                    p = null;
                    return;
                }
                event.getRowValue().setDescription(s);
                try {
                    getModel().updateTherapyComplex(event.getRowValue());
                    TherapyComplex p = event.getRowValue();
                    int i = table.getItems().indexOf(event.getRowValue());
                    table.getItems().set(i, null);
                    table.getItems().set(i, p);
                    p = null;

                } catch (Exception e) {
                    logger.error("",e);
                }


            }
        });

        //общая длительность, зависит от количества програм их частот и мультичастотного режима, также времени на частоту
        TableColumn<TherapyComplex,String> timeColTC=new TableColumn<>(res.getString("app.table.delay"));
        //  timeColTC.setCellValueFactory(param -> new SimpleStringProperty(DateUtil.convertSecondsToHMmSs(getModel().getTimeTherapyComplex(param.getValue()))));
        //пересчет индуцируется при изменении свойства time


        timeColTC.setCellValueFactory(param -> {
            SimpleStringProperty property = new SimpleStringProperty();
            property.bind(new StringBinding() {
                {
                    super.bind(param.getValue().timeProperty());
                }

                @Override
                protected String computeValue() {
                    String s = DateUtil.convertSecondsToHMmSs(getModel().getTimeTherapyComplex(param.getValue()));
                    textComplexTime.setValue(s+"#"+param.getValue().getId().longValue());
                    return s;
                }
            });
            return property;
        });


        TableColumn<TherapyComplex,Boolean>fileComplexCol=new TableColumn<>(res.getString("app.table.file"));
        fileComplexCol.cellValueFactoryProperty().setValue(param -> {
            SimpleBooleanProperty property = new SimpleBooleanProperty();
            property.bind(param.getValue().changedProperty());
            return property;
        });
        fileComplexCol.setCellFactory(col ->
        {
            TableCell<TherapyComplex, Boolean> cell = new TableCell<TherapyComplex, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {

                    super.updateItem(item, empty);
                    this.setText(null);
                    this.setGraphic(null);

                    HBox hbox=null;
                    ImageView iv=null;

                    if( this.getUserData()!=null)
                    {
                        hbox=(HBox)this.getUserData();
                        if(hbox!=null){
                            iv=(ImageView)hbox.getChildren().get(0);

                        }else {
                            iv=new ImageView();
                            hbox=new HBox();
                            hbox.setSpacing(3);
                            hbox.getChildren().addAll(iv);
                        }
                    }else {
                        iv=new ImageView(imageCancel);

                        hbox=new HBox();
                        hbox.setSpacing(3);
                        hbox.getChildren().addAll(iv);
                        this.setUserData(hbox);
                    }


                    if (!empty) {
                        if (this.getTableRow().getItem() == null) {
                            setText("");
                            return;
                        }
                        if (item)  iv.setImage(imageCancel);
                        else {
                            TherapyComplex thisComplex = (TherapyComplex) getTableRow().getItem();
                            if(thisComplex==null) return;

                            if(getModel().countTherapyPrograms(thisComplex)==0)iv.setImage(imageCancel);
                            else if(getModel().hasNeedGenerateProgramInComplex(thisComplex))  iv.setImage(imageCancel);
                            else  {
                                long sum=0;
                                File f;
                                for (Long id : getModel().getTherapyComplexFiles(thisComplex)) {
                                    f=new File(getApp().getDataDir(),id+".dat");
                                    if(f.exists())sum+=f.length();
                                }
                                for (String v : getModel().mp3ProgramPathsInComplex(thisComplex)) {

                                    f = new File(v);
                                    if (f.exists()) sum += f.length();
                                }


                                if(sum>0) setText(Math.ceil((double)sum / 1048576.0) + " Mb");
                                iv.setImage(imageDone);
                            }

                        }



                        setGraphic(hbox);
                    }
                }
            };

            return cell;
        });


        numComplexCol.setStyle( "-fx-alignment: CENTER;");
        timeColTC.setStyle( "-fx-alignment: CENTER;");
        nameColTC.setStyle( "-fx-alignment: CENTER-LEFT;");
        this.table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getColumns().addAll(numComplexCol, nameColTC, descColTC, timeColTC, fileComplexCol);
        table.placeholderProperty().setValue(new Label(res.getString("app.table.complex_placeholder")));
        table.setEditable(true);


        numComplexCol.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        nameColTC.prefWidthProperty().bind(table.widthProperty().multiply(0.325));
        descColTC.prefWidthProperty().bind(table.widthProperty().multiply(0.325));
        timeColTC.prefWidthProperty().bind(table.widthProperty().multiply(0.1));
        fileComplexCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));

        numComplexCol.setSortable(false);
        nameColTC.setSortable(false);
        descColTC.setSortable(false);
        timeColTC.setSortable(false);
        fileComplexCol.setSortable(false);

        fileComplexCol.setEditable(true);
    }


    public void initComplexesContextMenu(Path devicePath,
                                          Runnable onPrintComplex,
                                          Runnable cutInTables,
                                          Runnable deleteInTables,
                                          Runnable copyTherapyComplexToBase,
                                          Runnable generateComplexes,
                                          Runnable uploadComplexesToDir,
                                          Runnable uploadComplexesToM,
                                          Runnable copyInTables,
                                          Runnable pasteInTables,
                                          Consumer<List<TherapyComplex>> complexesToBiofon,
                                          Supplier<Boolean> toUserBaseMenuItemPredicate) {

        MenuItem mic1 = new MenuItem(this.res.getString("app.to_user_base"));
        MenuItem mic2 = new MenuItem(this.res.getString("app.ui_comlexes_generation"));
        MenuItem mic3 = new MenuItem(this.res.getString("app.upload_to_dir"));
        MenuItem mic5 = new MenuItem(this.res.getString("app.upload_to_biomedism"));
        MenuItem mic4 =new MenuItem(this.res.getString("app.to_biofon"));

        MenuItem mic6=new SeparatorMenuItem();
        MenuItem mic7=new SeparatorMenuItem();
        MenuItem mic8=new SeparatorMenuItem();

        MenuItem mic9 = new MenuItem(this.res.getString("app.ui.copy"));
        MenuItem mic10 =new MenuItem(this.res.getString("app.ui.paste"));
        MenuItem mic11 =new MenuItem(this.res.getString("app.cut"));
        MenuItem mic12 =new MenuItem(this.res.getString("app.delete"));

        MenuItem mic13 =new MenuItem(this.res.getString("app.ui.printing_complexes"));

        mic11.setAccelerator(KeyCombination.keyCombination("Ctrl+X"));
        mic9.setAccelerator(KeyCombination.keyCombination("Ctrl+C"));
        mic10.setAccelerator(KeyCombination.keyCombination("Ctrl+V"));
        mic12.setAccelerator(KeyCombination.keyCombination("Delete"));

        mic13.setOnAction(e->onPrintComplex.run());
        mic11.setOnAction(e-> cutInTables.run());
        mic12.setOnAction(e->deleteInTables.run());

        mic1.setOnAction((event2) -> {
            copyTherapyComplexToBase.run();
        });

        mic2.setOnAction((event2) -> {
            generateComplexes.run();
        });
        mic3.setOnAction((event2) -> {
            uploadComplexesToDir.run();
        });
        mic5.setOnAction((event2) -> {
            uploadComplexesToM.run();
        });
        mic9.setOnAction(event -> copyInTables.run());
        mic10.setOnAction(event -> pasteInTables.run());
        mic4.setOnAction(event -> complexesToBiofon.accept(table.getSelectionModel().getSelectedItems()));
        this.complexesMenu.getItems().addAll(
                mic11,
                mic9,
                mic10,
                mic12,
                mic6,
                mic2,
                mic3,
                mic5,
                mic7,
                mic4,
                mic1,
                mic13);
        table.setContextMenu(complexesMenu);
        complexesMenu.setOnShowing((event1) -> {
            mic1.setDisable(false);
            mic2.setDisable(true);
            mic3.setDisable(false);
            mic4.setDisable(false);
            mic5.setDisable(false);
            mic13.setDisable(false);
            mic9.setDisable(false);
            mic10.setDisable(true);
            Clipboard clipboard =Clipboard.getSystemClipboard();
            if(this.table.getSelectionModel().getSelectedItems().isEmpty()) {

                mic1.setDisable(true);
                mic2.setDisable(true);
                mic3.setDisable(true);
                mic4.setDisable(true);
                mic5.setDisable(true);
                mic9.setDisable(true);
                mic11.setDisable(true);
                mic12.setDisable(true);
                mic13.setDisable(true);

                //вставить можно и в пустую таблицу
                if(clipboard.hasContent(COMPLEX_COPY_ITEM) || clipboard.hasContent(COMPLEX_CUT_ITEM_ID)  )  mic10.setDisable(false);

            } else {
                mic9.setDisable(false);
                mic11.setDisable(false);



                if(clipboard.hasContent(COMPLEX_COPY_ITEM) || clipboard.hasContent(this.COMPLEX_CUT_ITEM_ID)) {


                    if (clipboard.hasContent(COMPLEX_COPY_ITEM)) {
                        if(table.getSelectionModel().getSelectedIndices().size()==1) mic10.setDisable(false);
                        else mic10.setDisable(true);
                    }
                    else  if(table.getSelectionModel().getSelectedIndices().size()==1) {
                        mic10.setDisable(true);
                        Integer[] ind = (Integer[]) clipboard.getContent(COMPLEX_CUT_ITEM_INDEX);
                        if (ind != null) {
                            if (ind.length != 0) {
                                Long idProfile = (Long) clipboard.getContent(COMPLEX_CUT_ITEM_PROFILE);
                                if(idProfile==null)mic10.setDisable(true);
                                else if(idProfile.longValue()== ProfileTable.getInstance().getSelectedItem().getId().longValue()){
                                    //вставка в том же профиле
                                    int dropIndex = table.getSelectionModel().getSelectedIndex();
                                    if(isEnablePaste(dropIndex,ind))mic10.setDisable(false);

                                }else   mic10.setDisable(false);//вставка в другом профиле, можно в любое место
                            }
                        }

                    }

                } else {
                    mic10.setDisable(true);
                }


                mic12.setDisable(false);

                Iterator tag = this.table.getSelectionModel().getSelectedItems().iterator();

                while(tag.hasNext()) {
                    TherapyComplex therapyComplex = (TherapyComplex)tag.next();

                    if( therapyComplex.isChanged() || this.getModel().hasNeedGenerateProgramInComplex(therapyComplex) ) {
                        if(getModel().countTherapyPrograms(therapyComplex)==0) continue;
                        mic2.setDisable(false);
                        mic3.setDisable(true);
                        mic5.setDisable(true);
                        break;
                    }
                }
               mic1.setDisable(toUserBaseMenuItemPredicate.get());


                if(devicePath!=null && !mic5.isDisable() )   mic5.setDisable(false);
                else  mic5.setDisable(true);
            }
        });
    }
    private ModelDataApp getModel() {
        return App.getStaticModel();
    }


    public TherapyComplex getSelectedItem(){
        return table.getSelectionModel().getSelectedItem();
    }
    public List<TherapyComplex> getSelectedItems(){
        return table.getSelectionModel().getSelectedItems();
    }
    public List<Integer> getSelectedIndexes(){
        return table.getSelectionModel().getSelectedIndices();
    }
    public ObservableList<TherapyComplex> getAllItems(){
        return table.getItems();
    }
}